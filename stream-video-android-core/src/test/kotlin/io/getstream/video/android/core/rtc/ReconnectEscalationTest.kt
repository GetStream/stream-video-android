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

package io.getstream.video.android.core.rtc

import com.google.common.truth.Truth.assertThat
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.base.DispatcherRule
import io.getstream.video.android.core.call.FastReconnectResult
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.internal.module.CoordinatorConnectionModule
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.model.User
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import stream.video.sfu.models.TrackInfo
import stream.video.sfu.models.WebsocketReconnectStrategy
import stream.video.sfu.signal.TrackSubscriptionDetails

/**
 * Tests the reconnect loop escalation in [Call.reconnect].
 *
 * When [RtcSession.fastReconnect] returns [FastReconnectResult.PeerConnectionStale],
 * the loop must immediately escalate to REJOIN instead of retrying FAST
 * (which would deadlock because REJOIN requires re-acquiring the reconnect mutex).
 */
class ReconnectEscalationTest {

    @get:Rule
    val dispatcherRule = DispatcherRule()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @RelaxedMockK
    private lateinit var mockClientImpl: StreamVideoClient

    @RelaxedMockK
    private lateinit var mockSession: RtcSession

    private lateinit var mockStreamVideo: StreamVideo
    private lateinit var call: Call

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        mockStreamVideo = mockk(relaxed = true)
        StreamVideo.install(mockStreamVideo)

        val mockNetworkStateProvider = mockk<NetworkStateProvider>(relaxed = true)
        every { mockNetworkStateProvider.isConnected() } returns true
        val mockCoordinatorModule = mockk<CoordinatorConnectionModule>(relaxed = true)
        every { mockCoordinatorModule.networkStateProvider } returns mockNetworkStateProvider

        every { mockClientImpl.coordinatorConnectionModule } returns mockCoordinatorModule
        every { mockClientImpl.scope } returns testScope as CoroutineScope
        every { mockClientImpl.leaveAfterDisconnectSeconds } returns 120L
        every { mockClientImpl.apiKey } returns "test-api-key"

        val user = User(id = "test-user", role = "user")

        call = Call(
            client = mockClientImpl,
            type = "default",
            id = "test-call",
            user = user,
        )

        every { mockSession.sfuName } returns "sfu-edge-1"
        coEvery { mockSession.getPublisherStats() } returns null
        coEvery { mockSession.getSubscriberStats() } returns null
        every { mockSession.subscriber } returns MutableStateFlow(null)
        every { mockSession.publisher } returns MutableStateFlow(null)
        every { mockSession.currentSfuInfo() } returns Triple(
            "prev-session",
            emptyList<TrackSubscriptionDetails>(),
            emptyList<TrackInfo>(),
        )

        call.session.value = mockSession
        call.state._connection.value = RealtimeConnection.Connected
    }

    @After
    fun tearDown() {
        StreamVideo.removeClient()
        unmockkAll()
    }

    /**
     * With PeerConnectionStale the loop escalates to REJOIN on the very first attempt.
     * REJOIN fails immediately (location = null) → ReconnectingFailed → leave() →
     * Disconnected. fastReconnect is called exactly once, proving early escalation.
     */
    @Test
    fun `PeerConnectionStale escalates immediately to REJOIN`() = runTest(
        testDispatcher,
    ) {
        coEvery { mockSession.fastReconnect(any()) } returns FastReconnectResult.PeerConnectionStale

        call.reconnect(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
            "test:peer-connection-stale",
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { mockSession.fastReconnect(any()) }
        assertThat(
            call.state.connection.value,
        ).isInstanceOf(RealtimeConnection.Disconnected::class.java)
    }

    /**
     * With a generic exception the loop retries FAST up to MAX_FAST_RECONNECT_ATTEMPTS (3)
     * before escalating to REJOIN. fastReconnect is called at iterations 0,1,2 → 3 calls.
     * REJOIN then fails (location = null) → ReconnectingFailed → leave() → Disconnected.
     */
    @Test
    fun `Failed result retries FAST before escalating to REJOIN`() = runTest(testDispatcher) {
        coEvery { mockSession.fastReconnect(any()) } returns FastReconnectResult.Failed(
            Exception("SFU connect timeout"),
        )

        call.reconnect(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
            "test:generic-failure",
        )
        advanceUntilIdle()

        coVerify(exactly = 3) { mockSession.fastReconnect(any()) }
        assertThat(
            call.state.connection.value,
        ).isInstanceOf(RealtimeConnection.Disconnected::class.java)
    }

    /**
     * The reconnect loop must complete without deadlocking. The old code called
     * call.rejoin() from inside fastReconnect() which re-entered the reconnect
     * mutex and hung indefinitely. This test proves the loop terminates.
     */
    @Test
    fun `reconnect loop terminates without deadlock on PeerConnectionStale`() = runTest(
        testDispatcher,
    ) {
        coEvery { mockSession.fastReconnect(any()) } returns FastReconnectResult.PeerConnectionStale

        call.reconnect(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
            "test:no-deadlock",
        )
        advanceUntilIdle()

        assertThat(call.state.connection.value).isNotEqualTo(RealtimeConnection.Reconnecting)
    }
}
