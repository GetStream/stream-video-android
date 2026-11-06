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

import androidx.lifecycle.Lifecycle
import com.google.common.truth.Truth.assertThat
import io.getstream.result.Result.Failure
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.analytics.call.observer.SfuAnalytics
import io.getstream.video.android.core.base.DispatcherRule
import io.getstream.video.android.core.call.EmptyRtcSession
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.components.CallSessionManager
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.getstream.video.android.core.call.scope.ScopeProvider
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.internal.module.SfuConnectionModule
import io.getstream.video.android.core.socket.sfu.SfuSocketConnection
import io.getstream.video.android.core.socket.sfu.state.SfuSocketState
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import stream.video.sfu.models.PeerType
import stream.video.sfu.models.TrackType

class EmptyRtcSessionTest {

    @get:Rule
    val dispatcherRule = DispatcherRule()

    private val testScope = TestScope(StandardTestDispatcher())

    private lateinit var mockCall: Call
    private lateinit var mockLifecycle: Lifecycle
    private lateinit var mockVideoClient: StreamVideoClient
    private lateinit var mockStreamVideo: StreamVideo
    private lateinit var peerConnectionFactory: StreamPeerConnectionFactory

    @Before
    fun setup() {
        mockStreamVideo = mockk(relaxed = true)
        mockVideoClient = mockk(relaxed = true)
        mockLifecycle = mockk(relaxed = true)
        peerConnectionFactory = mockk(relaxed = true)
        val callState = mockk<CallState>(relaxed = true)
        val scopeProvider = mockk<ScopeProvider> {
            every { getCoroutineScope(any()) } returns testScope
            every { getRtcSessionScope(any(), any()) } returns testScope
        }
        mockCall = mockk(relaxed = true)
        every { mockCall.state } returns callState
        every { mockCall.scope } returns testScope
        every { mockCall.scopeProvider } returns scopeProvider
        every { mockCall.peerConnectionFactory } returns peerConnectionFactory
        StreamVideo.install(mockStreamVideo)
    }

    @After
    fun tearDown() {
        StreamVideo.removeClient()
        unmockkAll()
    }

    private fun createEmptySession() = EmptyRtcSession(
        client = mockStreamVideo,
        call = mockCall,
        sessionManager = CallSessionManager(),
        sessionId = "test-session-id",
        apiKey = "test-api-key",
        lifecycle = mockLifecycle,
        sfuAnalytics = SfuAnalytics.getFakeSfuAnalytics(),
        clientImpl = mockVideoClient,
    )

    @Test
    fun `empty session never touches the peer connection factory or the lifecycle`() =
        testScope.runTest {
            val session = createEmptySession()
            advanceUntilIdle()

            assertThat(session.subscriber.value).isNull()
            assertThat(session.publisher.value).isNull()
            assertThat(session.sfuSocketState.value)
                .isEqualTo(SfuSocketState.Disconnected.Stopped)
            verify(exactly = 0) { mockCall.peerConnectionFactory }
            verify(exactly = 0) { mockLifecycle.addObserver(any()) }
        }

    @Test
    fun `empty session public methods are safe no-ops`() = testScope.runTest {
        val session: RtcSession = createEmptySession()

        session.reconnect(forceRestart = true)
        @Suppress("DEPRECATION")
        session.connect()
        session.setScreenShareTrack()
        session.handleEvent(mockk<SubscriberOfferEvent>(relaxed = true))
        session.handleIceTrickle(
            ICETrickleEvent(candidate = "{}", peerType = PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED),
        )
        session.handleSubscriberOffer(mockk(relaxed = true))
        session.updateTrackDimensions("other-session", TrackType.TRACK_TYPE_VIDEO, visible = true)
        advanceUntilIdle()

        assertThat(session.getPublisherStats()).isNull()
        assertThat(session.getSubscriberStats()).isNull()
        assertThat(session.requestSubscriberIceRestart()).isInstanceOf(Failure::class.java)
        assertThat(session.requestPublisherIceRestart()).isInstanceOf(Failure::class.java)
        assertThat(session.subscriberPendingEvents).isEmpty()
        assertThat(session.publisherPendingEvents).isEmpty()
        verify(exactly = 0) { mockCall.peerConnectionFactory }
        verify(exactly = 0) { mockCall.mediaManager }
    }

    @Test
    fun `empty session cleanup succeeds without an SFU connection and can repeat`() =
        testScope.runTest {
            val session = createEmptySession()

            session.cleanup()
            session.cleanup()
            advanceUntilIdle()

            assertThat(session.subscriber.value).isNull()
            verify(exactly = 0) { mockCall.peerConnectionFactory }
        }

    @Test
    fun `regular session still creates the SFU connection and the subscriber`() = runTest {
        every { mockCall.state.participants } returns MutableStateFlow(emptyList())
        every { mockCall.state.remoteParticipants } returns MutableStateFlow(emptyList())
        every { mockCall.state.ownCapabilities } returns MutableStateFlow(emptyList())
        var moduleCreations = 0
        val socket = mockk<SfuSocketConnection>(relaxed = true) {
            every { state() } returns MutableStateFlow(SfuSocketState.Disconnected.Stopped)
            every { errors() } returns MutableSharedFlow()
            every { events() } returns MutableSharedFlow()
        }
        val module = mockk<SfuConnectionModule>(relaxed = true) {
            every { socketConnection } returns socket
        }

        val session = RtcSession(
            client = mockStreamVideo,
            powerManager = null,
            call = mockCall,
            sessionManager = CallSessionManager(),
            sessionId = "test-session-id",
            apiKey = "test-api-key",
            lifecycle = mockLifecycle,
            sfuUrl = "https://test-sfu.stream.com",
            sfuWsUrl = "wss://test-sfu.stream.com",
            sfuToken = "fake-sfu-token",
            sfuName = "sfu-edge-1",
            remoteIceServers = emptyList(),
            clientImpl = mockVideoClient,
            coroutineScope = testScope,
            rtcSessionScope = testScope,
            sfuConnectionModuleProvider = {
                moduleCreations++
                module
            },
            sfuAnalytics = SfuAnalytics.getFakeSfuAnalytics(),
        )
        testScope.advanceUntilIdle()

        assertThat(moduleCreations).isEqualTo(1)
        assertThat(session.subscriber.value).isNotNull()
        verify(atLeast = 1) { mockCall.peerConnectionFactory }
    }
}
