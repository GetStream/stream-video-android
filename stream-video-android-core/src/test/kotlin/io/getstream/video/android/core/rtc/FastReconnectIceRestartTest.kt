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

import android.os.PowerManager
import androidx.lifecycle.Lifecycle
import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.MediaManagerImpl
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.analytics.call.observer.SfuAnalytics
import io.getstream.video.android.core.call.FastReconnectResult
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.SfuConnectionResult
import io.getstream.video.android.core.call.connection.Publisher
import io.getstream.video.android.core.call.connection.Subscriber
import io.getstream.video.android.core.internal.module.SfuConnectionModule
import io.getstream.video.android.core.model.IceServer
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.webrtc.SessionDescription

/**
 * Tests that [RtcSession.fastReconnect] proactively restarts ICE on both
 * publisher and subscriber after a successful SFU WebSocket reconnect,
 * and only escalates to REJOIN when peer connections are truly CLOSED.
 */
class FastReconnectIceRestartTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val ownCapabilitiesFlow = MutableStateFlow<List<OwnCapability>>(emptyList())
    private val participantsFlow = MutableStateFlow<List<ParticipantState>>(emptyList())
    private val remoteParticipantsFlow = MutableStateFlow<List<ParticipantState>>(emptyList())

    @MockK
    private lateinit var mockPowerManager: PowerManager

    @RelaxedMockK
    private lateinit var mockCall: Call

    @RelaxedMockK
    private lateinit var mockCallState: CallState

    @RelaxedMockK
    private lateinit var mockMediaManager: MediaManagerImpl

    @RelaxedMockK
    private lateinit var mockLifecycle: Lifecycle

    @RelaxedMockK
    private lateinit var mockVideoClient: StreamVideoClient

    private lateinit var mockStreamVideo: StreamVideo

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        mockStreamVideo = mockk(relaxed = true)
        every { mockCall.state } returns mockCallState
        every { mockCall.scope } returns testScope
        every { mockCall.mediaManager } returns mockMediaManager
        every { mockCall.peerConnectionFactory } returns mockk(relaxed = true) {
            every {
                makePeerConnection(any(), any(), any(), any())
            } answers {
                mockk(relaxed = true) {
                    coEvery { createOffer(any()) } returns io.getstream.result.Result.Success(
                        SessionDescription(SessionDescription.Type.OFFER, "fake-sdp"),
                    )
                    coEvery { createAnswer(any()) } returns io.getstream.result.Result.Success(
                        SessionDescription(SessionDescription.Type.ANSWER, "fake-answer-sdp"),
                    )
                }
            }
        }
        every { mockCallState.ownCapabilities } returns ownCapabilitiesFlow
        every { mockCallState.participants } returns participantsFlow
        every { mockCallState.remoteParticipants } returns remoteParticipantsFlow
        every { mockCallState.replaceParticipants(any()) } answers { }
        every { mockCallState.me.value } returns null
        StreamVideo.install(mockStreamVideo)
    }

    @After
    fun tearDown() {
        StreamVideo.removeClient()
        unmockkAll()
    }

    private fun createRtcSession(): RtcSession {
        val sfuConnectionModule = mockk<SfuConnectionModule>(relaxed = true)
        return spyk(
            RtcSession(
                client = mockStreamVideo,
                powerManager = mockPowerManager,
                call = mockCall,
                sessionId = "test-session",
                apiKey = "test-api-key",
                lifecycle = mockLifecycle,
                sfuUrl = "https://test-sfu.stream.com",
                sfuWsUrl = "wss://test-sfu.stream.com",
                sfuToken = "fake-sfu-token",
                sfuName = "test-sfu-edge",
                remoteIceServers = emptyList<IceServer>(),
                clientImpl = mockVideoClient,
                coroutineScope = testScope,
                sfuConnectionModuleProvider = { sfuConnectionModule },
                sfuAnalytics = SfuAnalytics.getFakeSfuAnalytics(),
            ),
        )
    }

    private fun mockPublisher(closed: Boolean = false): Publisher = mockk(relaxed = true) {
        every { isClosed() } returns closed
        every { currentOptions() } returns emptyList()
    }

    private fun mockSubscriber(closed: Boolean = false): Subscriber = mockk(relaxed = true) {
        every { isClosed() } returns closed
    }

    @Test
    fun `fastReconnect restarts ICE on healthy publisher and subscriber`() = runTest(
        UnconfinedTestDispatcher(),
    ) {
        val session = createRtcSession()
        val pub = mockPublisher()
        val sub = mockSubscriber()

        session.publisher.value = pub
        session.subscriber.value = sub

        coEvery { session.connectInternal(any(), any()) } returns SfuConnectionResult.Connected

        val result = session.fastReconnect(null)

        assertThat(result).isEqualTo(FastReconnectResult.Connected)
        coVerify { pub.restartIce(match { it.contains("fastReconnect") }) }
        coVerify { session.requestSubscriberIceRestart() }
    }

    @Test
    fun `fastReconnect restarts ICE when publisher is not closed`() = runTest(
        UnconfinedTestDispatcher(),
    ) {
        val session = createRtcSession()
        val pub = mockPublisher(closed = false)
        val sub = mockSubscriber(closed = false)

        session.publisher.value = pub
        session.subscriber.value = sub

        coEvery { session.connectInternal(any(), any()) } returns SfuConnectionResult.Connected

        val result = session.fastReconnect(null)

        assertThat(result).isEqualTo(FastReconnectResult.Connected)
        coVerify { pub.restartIce(any()) }
    }

    @Test
    fun `fastReconnect returns PeerConnectionStale when publisher is CLOSED`() = runTest(
        UnconfinedTestDispatcher(),
    ) {
        val session = createRtcSession()
        val pub = mockPublisher(closed = true)
        val sub = mockSubscriber()

        session.publisher.value = pub
        session.subscriber.value = sub

        coEvery { session.connectInternal(any(), any()) } returns SfuConnectionResult.Connected

        val result = session.fastReconnect(null)

        assertThat(result).isEqualTo(FastReconnectResult.PeerConnectionStale)
        coVerify(exactly = 0) { pub.restartIce(any()) }
    }

    @Test
    fun `fastReconnect returns PeerConnectionStale when subscriber is CLOSED`() = runTest(
        UnconfinedTestDispatcher(),
    ) {
        val session = createRtcSession()
        val pub = mockPublisher()
        val sub = mockSubscriber(closed = true)

        session.publisher.value = pub
        session.subscriber.value = sub

        coEvery { session.connectInternal(any(), any()) } returns SfuConnectionResult.Connected

        val result = session.fastReconnect(null)

        assertThat(result).isEqualTo(FastReconnectResult.PeerConnectionStale)
        coVerify(exactly = 0) { pub.restartIce(any()) }
    }

    @Test
    fun `fastReconnect returns Failed when SFU connection fails`() = runTest(
        UnconfinedTestDispatcher(),
    ) {
        val session = createRtcSession()
        val pub = mockPublisher()
        val sub = mockSubscriber()

        session.publisher.value = pub
        session.subscriber.value = sub

        val error = Exception("SFU connection timed out")
        coEvery { session.connectInternal(any(), any()) } returns SfuConnectionResult.Failed(error)

        val result = session.fastReconnect(null)

        assertThat(result).isInstanceOf(FastReconnectResult.Failed::class.java)
        coVerify(exactly = 0) { pub.restartIce(any()) }
        coVerify(exactly = 0) { session.requestSubscriberIceRestart() }
    }
}
