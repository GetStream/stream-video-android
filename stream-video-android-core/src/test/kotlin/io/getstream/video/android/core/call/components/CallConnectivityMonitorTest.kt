/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.core.call.components

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallLeaveReason
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.internal.module.CoordinatorConnectionModule
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import stream.video.sfu.models.WebsocketReconnectStrategy

/**
 * Tests [CallConnectivityMonitor]: network subscription forwarding, the reconnect
 * strategy chosen on reconnection, and the delayed leave when the device stays offline.
 */
class CallConnectivityMonitorTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var network: NetworkStateProvider
    private lateinit var clientImpl: StreamVideoClient
    private lateinit var state: CallState
    private lateinit var connectionFlow: MutableStateFlow<RealtimeConnection>
    private lateinit var call: Call

    @Before
    fun setup() {
        network = mockk(relaxed = true)
        clientImpl = mockk(relaxed = true)
        state = mockk(relaxed = true)
        connectionFlow = MutableStateFlow(RealtimeConnection.Reconnecting)
        call = mockk(relaxed = true)

        val coordinatorModule = mockk<CoordinatorConnectionModule>(relaxed = true)
        every { coordinatorModule.networkStateProvider } returns network
        every { clientImpl.coordinatorConnectionModule } returns coordinatorModule
        every { clientImpl.leaveAfterDisconnectSeconds } returns 1L
        every { network.isConnected() } returns true

        every { call.type } returns "default"
        every { call.id } returns "call-id"
        every { call.clientImpl } returns clientImpl
        every { call.scope } returns testScope
        every { call.state } returns state
        every { call.reconnectDeadlineMillis } returns 10_000
        every { state.connection } returns connectionFlow
    }

    private fun monitor() = CallConnectivityMonitor(call)

    private fun listenerOf(monitor: CallConnectivityMonitor): NetworkStateProvider.NetworkStateListener {
        val field = CallConnectivityMonitor::class.java.getDeclaredField("listener")
        field.isAccessible = true
        return field.get(monitor) as NetworkStateProvider.NetworkStateListener
    }

    @Test
    fun `subscribe unsubscribe and isConnected forward to the network provider`() {
        val monitor = monitor()

        monitor.subscribe()
        monitor.unsubscribe()
        val connected = monitor.isConnected()

        verify { network.subscribe(any()) }
        verify { network.unsubscribe(any()) }
        assertThat(connected).isTrue()
    }

    @Test
    fun `first connection with no prior disconnect rejoins`() = runTest(testDispatcher) {
        val listener = listenerOf(monitor())

        listener.onConnected()
        advanceUntilIdle()

        coVerify {
            call.reconnect(WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN, any())
        }
    }

    @Test
    fun `reconnection soon after a disconnect uses fast reconnect`() = runTest(testDispatcher) {
        val listener = listenerOf(monitor())

        listener.onDisconnected()
        listener.onConnected()
        advanceUntilIdle()

        coVerify {
            call.reconnect(WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST, any())
        }
    }

    @Test
    fun `staying offline past the timeout leaves the call`() = runTest(testDispatcher) {
        val listener = listenerOf(monitor())

        listener.onDisconnected()
        advanceTimeBy(2_000)
        advanceUntilIdle()

        verify { call.leave(any<CallLeaveReason>()) }
    }

    @Test
    fun `reconnecting before the timeout does not leave the call`() = runTest(testDispatcher) {
        val listener = listenerOf(monitor())

        listener.onDisconnected()
        connectionFlow.value = RealtimeConnection.Connected
        advanceTimeBy(2_000)
        advanceUntilIdle()

        verify(exactly = 0) { call.leave(any<CallLeaveReason>()) }
    }
}
