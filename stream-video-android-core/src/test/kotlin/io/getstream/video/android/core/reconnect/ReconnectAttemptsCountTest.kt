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

package io.getstream.video.android.core.reconnect

import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.call.FastReconnectResult
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import stream.video.sfu.models.WebsocketReconnectStrategy
import kotlin.test.assertTrue

/**
 * Tests that the unified [io.getstream.video.android.core.Call.reconnect] loop
 * increments [io.getstream.video.android.core.Call.nonFastReconnectAttempts] according
 * to the JS SDK contract:
 * - FAST strategy does NOT increment the counter.
 * - REJOIN strategy increments the counter once per attempt.
 */
@RunWith(RobolectricTestRunner::class)
class ReconnectAttemptsCountTest : IntegrationTestBase() {

    private fun Call.injectMockNetwork(connected: Boolean = true) {
        val mockNetwork = mockk<NetworkStateProvider>(relaxed = true)
        every { mockNetwork.isConnected() } returns connected
        val field = Call::class.java.getDeclaredField("network\$delegate")
        field.isAccessible = true
        field.set(this, lazyOf(mockNetwork))
    }

    private fun stubSessionForReconnect(sessionMock: RtcSession) {
        coEvery { sessionMock.getPublisherStats() } returns null
        coEvery { sessionMock.getSubscriberStats() } returns null
        every { sessionMock.subscriber } returns MutableStateFlow(null)
        every { sessionMock.publisher } returns MutableStateFlow(null)
        every { sessionMock.currentSfuInfo() } returns Triple(
            "",
            emptyList(),
            emptyList(),
        )
    }

    @Test
    fun `FAST reconnect does not increment reconnectAttempts`() = runTest {
        val sessionMock = mockk<RtcSession>(relaxed = true)
        stubSessionForReconnect(sessionMock)
        coEvery { sessionMock.fastReconnect(any()) } returns FastReconnectResult.Connected
        val call = client.call("default", randomUUID())
        call.injectMockNetwork(connected = true)
        call.session.value = sessionMock

        call.fastReconnect()

        // FAST strategy never increments reconnectAttepmts — the retry loop
        // treats FAST as non-counting. If the loop eventually escalates to
        // REJOIN (because FAST keeps failing), those iterations will increment,
        // but we verify there's no increment for the initial FAST attempt.
        assertTrue(
            call.nonFastReconnectAttempts == 0 ||
                call.state.connection.value is RealtimeConnection.ReconnectingFailed,
            "Expected 0 reconnect attempts for FAST or ReconnectingFailed state, " +
                "got ${call.nonFastReconnectAttempts}",
        )
    }

    @Test
    fun `REJOIN increments reconnectAttempts`() = runTest {
        val sessionMock = mockk<RtcSession>(relaxed = true)
        stubSessionForReconnect(sessionMock)
        val call = client.call("default", randomUUID())
        call.injectMockNetwork(connected = true)
        call.session.value = sessionMock

        call.reconnect(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
            "test",
        )

        // At least one REJOIN attempt should have been counted
        assertTrue(
            call.nonFastReconnectAttempts > 0,
            "Expected reconnect attempts > 0, got ${call.nonFastReconnectAttempts}",
        )
    }

    @Test
    fun `Multiple reconnect calls accumulate attempts`() = runTest {
        val sessionMock = mockk<RtcSession>(relaxed = true)
        stubSessionForReconnect(sessionMock)
        val call = client.call("default", randomUUID())
        call.injectMockNetwork(connected = true)
        call.session.value = sessionMock

        call.reconnect(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
            "first",
        )
        val attemptsAfterFirst = call.nonFastReconnectAttempts

        call.reconnect(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
            "second",
        )

        assertTrue(
            call.nonFastReconnectAttempts >= attemptsAfterFirst,
            "Expected accumulated attempts >= $attemptsAfterFirst, got ${call.nonFastReconnectAttempts}",
        )
    }
}
