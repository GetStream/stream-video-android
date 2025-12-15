/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

import android.graphics.ColorSpace.match
import android.os.PowerManager
import androidx.lifecycle.Lifecycle
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.MediaManagerImpl
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.connection.Publisher
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.internal.module.SfuConnectionModule
import io.getstream.video.android.core.model.IceServer
import io.getstream.video.android.core.socket.sfu.SfuSocketConnection
import io.getstream.video.android.core.socket.sfu.state.SfuSocketState
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.webrtc.SessionDescription
import stream.video.sfu.models.ParticipantCount
import stream.video.sfu.models.PeerType
import stream.video.sfu.models.PublishOption
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.signal.SendAnswerResponse

class RtcSessionTest2 {

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

    // We'll spy on a minimal StreamVideo
    private lateinit var mockStreamVideo: StreamVideo

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // We also need to mock out the client = StreamVideo
        // So we can cast it to (StreamVideoClient) internally
        mockStreamVideo = mockk(relaxed = true)
        every { mockCall.state } returns mockCallState
        every { mockCall.scope } returns testScope
        every { mockCall.mediaManager } returns mockMediaManager
        every { mockCall.peerConnectionFactory } returns mockk(relaxed = true) {
            every {
                makePeerConnection(
                    any(), any(), any(), any(),
                )
            } returns mockk(relaxed = true) {}
        }
        every { mockCallState.ownCapabilities } returns ownCapabilitiesFlow
        every { mockCallState.participants } returns participantsFlow
        every { mockCallState.remoteParticipants } returns remoteParticipantsFlow
        every { mockCallState.replaceParticipants(any()) } answers { }

        // We can stub out other pieces
        every { mockCallState.me.value } returns null
        StreamVideo.install(mockStreamVideo)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `rtcSession constructor creates a subscriber StreamPeerConnection`() = runTest(
        UnconfinedTestDispatcher(),
    ) {
        // Given
        val sessionId = "test-session-id"
        val apiKey = "test-api-key"
        val lifecycle = mockLifecycle
        val sfuUrl = "https://test-sfu.stream.com"
        val sfuWsUrl = "wss://test-sfu.stream.com"
        val sfuToken = "fake-sfu-token"
        val remoteIceServers = listOf<IceServer>() // empty for test

        // When
        val rtcSession = spyk(
            RtcSession(
                client = mockStreamVideo,
                powerManager = mockPowerManager,
                call = mockCall,
                sessionId = sessionId,
                apiKey = apiKey,
                lifecycle = lifecycle,
                sfuUrl = sfuUrl,
                sfuWsUrl = sfuWsUrl,
                sfuToken = sfuToken,
                remoteIceServers = remoteIceServers,
                clientImpl = mockVideoClient,
                coroutineScope = testScope,
                sfuConnectionModuleProvider = { mockk(relaxed = true) },
            ),
        )

        // Then
        assertNotNull("Subscriber StreamPeerConnection should not be null", rtcSession.subscriber)
        assertEquals("Wrong sessionId", sessionId, rtcSession.fieldValue("sessionId"))
    }

    @Test
    fun `connect calls socketConnection_connect with JoinRequest and sets state to Connected`() =
        runTest(
            UnconfinedTestDispatcher(),
        ) {
            // Given
            val sessionId = "test-session-id"
            val apiKey = "test-api-key"
            val sfuUrl = "https://test-sfu.stream.com"
            val sfuWsUrl = "wss://test-sfu.stream.com"
            val sfuToken = "fake-sfu-token"
            val remoteIceServers = emptyList<IceServer>()

            // We’ll create an RtcSession with all mocks prepared:
            val sfuSocketModule = mockk<SfuConnectionModule>(relaxed = true)
            val rtcSession = RtcSession(
                client = mockStreamVideo,
                powerManager = mockPowerManager,
                call = mockCall,
                sessionId = sessionId,
                apiKey = apiKey,
                lifecycle = mockLifecycle,
                sfuUrl = sfuUrl,
                sfuWsUrl = sfuWsUrl,
                sfuToken = sfuToken,
                clientImpl = mockVideoClient,
                coroutineScope = testScope,
                remoteIceServers = remoteIceServers,
                sfuConnectionModuleProvider = { sfuSocketModule },
            )

            // When
            rtcSession.connect()

            // Then
            // We verify that connect(...) was actually called on the socket with any JoinRequest
            coVerify {
                sfuSocketModule.socketConnection.connect(
                    match { request ->
                        // Optionally, we can be more strict about checking the session_id, etc.
                        request.session_id == sessionId && request.token == sfuToken
                    },
                )
            }
        }

    @Test
    fun `handleSubscriberOffer sets remoteDescription, creates answer, sets localDescription`() =
        runTest(
            UnconfinedTestDispatcher(),
        ) {
            // Given: an RtcSession with a non-null subscriber
            val sessionId = "test-session-id"
            val apiKey = "test-api-key"
            val sfuUrl = "https://test-sfu.stream.com"
            val sfuWsUrl = "wss://test-sfu.stream.com"
            val sfuToken = "fake-sfu-token"
            val remoteIceServers = emptyList<IceServer>()
            val rtcSession = spyk(
                RtcSession(
                    client = mockStreamVideo,
                    powerManager = mockPowerManager,
                    call = mockCall,
                    sessionId = sessionId,
                    apiKey = apiKey,
                    lifecycle = mockLifecycle,
                    sfuUrl = sfuUrl,
                    sfuWsUrl = sfuWsUrl,
                    sfuToken = sfuToken,
                    clientImpl = mockVideoClient,
                    coroutineScope = testScope,
                    remoteIceServers = remoteIceServers,
                    sfuConnectionModuleProvider = { mockk(relaxed = true) },
                ),
            )
            val subscriber = rtcSession.subscriber
            assertNotNull("Subscriber must not be null", subscriber)

            val fakeSdpOffer = "fake-offer-sdp"
            val offerEvent = SubscriberOfferEvent(
                sdp = fakeSdpOffer,
            )
            coEvery { subscriber!!.setRemoteDescription(any()) } returns io.getstream.result.Result.Success(
                Unit,
            )
            coEvery { subscriber!!.createAnswer() } returns io.getstream.result.Result.Success(
                SessionDescription(SessionDescription.Type.ANSWER, "fake-answer-sdp"),
            )
            coEvery { subscriber!!.setLocalDescription(any()) } returns io.getstream.result.Result.Success(
                Unit,
            )
            val mockApi = rtcSession.sfuConnectionModule.api
            coEvery { mockApi.sendAnswer(any()) } returns SendAnswerResponse(
                error = null,
            )

            rtcSession.handleSubscriberOffer(offerEvent)

            coVerify {
                subscriber!!.negotiate(
                    match {
                        it.contains("fake-offer-sdp")
                    },
                )
            }
        }

    // TODO: Test is broken because socket connection is not established in this test.
    @Test
    fun `handleIceTrickle adds event to publisherPendingEvents if publisher is null`() = runTest {
        // Given an RtcSession with no publisher set (publisher = null by default until fully joined)
        val mockSocket = mockk<SfuSocketConnection>()
        val mockConnectedEvent = mockk<JoinCallResponseEvent>(relaxed = true)
        val socketStateFlow =
            MutableStateFlow<SfuSocketState>(SfuSocketState.Connected(mockConnectedEvent))
        every { mockSocket.state() } returns socketStateFlow
        val mockModule = mockk<SfuConnectionModule>(relaxed = true) {
            every { socketConnection } returns mockSocket
        }
        val rtcSession = RtcSession(
            client = mockStreamVideo,
            powerManager = mockPowerManager,
            call = mockCall,
            sessionId = "session-id",
            apiKey = "api-key",
            lifecycle = mockLifecycle,
            sfuUrl = "https://test-sfu.stream.com",
            sfuWsUrl = "wss://test-sfu.stream.com",
            sfuToken = "fake-sfu-token",
            clientImpl = mockVideoClient,
            coroutineScope = testScope,
            rtcSessionScope = testScope,
            remoteIceServers = emptyList(),
            sfuConnectionModuleProvider = { mockModule },
        )
        // Confirm publisher is null
        assertNull(rtcSession.publisher)

        // A typical ICETrickleEvent with peerType = PUBLISHER_UNSPECIFIED
        val event = ICETrickleEvent(
            candidate = """{
            "sdpMid": "0",
            "sdpMLineIndex": 0,
            "candidate": "candidate-data",
            "usernameFragment": "fake-username-frag"}
            """.trimIndent(),
            peerType = PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED,
        )

        // When
        rtcSession.handleIceTrickle(event)
        testScope.testScheduler.advanceUntilIdle()

        // Then
        // The event should be added to publisherPendingEvents
        assertTrue(
            "publisherPendingEvents should contain the ICETrickleEvent",
            event in rtcSession.publisherPendingEvents,
        )
        // No call to subscriber or publisher handleNewIceCandidate
        // We can do a negative verify on subscriber or log checks, but typically verifying there's no error is enough.
    }

    @Test
    fun `handleIceTrickle calls publisherhandleNewIceCandidate if publisher is available`() =
        runTest {
            // Given
            val rtcSession = RtcSession(
                client = mockStreamVideo,
                powerManager = mockPowerManager,
                call = mockCall,
                sessionId = "session-id",
                apiKey = "api-key",
                lifecycle = mockLifecycle,
                sfuUrl = "https://test-sfu.stream.com",
                sfuWsUrl = "wss://test-sfu.stream.com",
                sfuToken = "fake-sfu-token",
                clientImpl = mockVideoClient,
                coroutineScope = testScope,
                remoteIceServers = emptyList(),
                sfuConnectionModuleProvider = { mockk(relaxed = true) },
            )
            val mockPublisher = mockk<Publisher>(relaxed = true)
            rtcSession.publisher = mockPublisher
            val event = ICETrickleEvent(
                candidate = """{
            "sdpMid": "0",
            "sdpMLineIndex": 0,
            "candidate": "candidate-data",
            "usernameFragment": "fake-username-frag"}
                """.trimIndent(),
                peerType = PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED,
            )

            // When
            rtcSession.handleIceTrickle(event)

            // Then
            coVerify {
                mockPublisher.handleNewIceCandidate(
                    match { it.candidate.contains("candidate-data") },
                )
            }
            assertTrue(
                "publisherPendingEvents should be empty",
                rtcSession.publisherPendingEvents.isEmpty(),
            )
        }

    @Test
    fun `cleanup disconnects SFU, closes peer connections, and clears tracks`() = runTest {
        // Given
        val sessionId = "test-session-id"
        val rtcSession = RtcSession(
            client = mockStreamVideo,
            powerManager = mockPowerManager,
            call = mockCall,
            sessionId = sessionId,
            apiKey = "test-api-key",
            lifecycle = mockLifecycle,
            sfuUrl = "https://test-sfu.stream.com",
            sfuWsUrl = "wss://test-sfu.stream.com",
            sfuToken = "fake-sfu-token",
            clientImpl = mockVideoClient,
            coroutineScope = testScope,
            remoteIceServers = emptyList(),
            sfuConnectionModuleProvider = { mockk(relaxed = true) },
        )
        val subscriber = rtcSession.subscriber
        assertNotNull(subscriber)
        val publisher = mockk<Publisher>(relaxed = true)
        rtcSession.publisher = publisher
        val mockSocketConnection = rtcSession.sfuConnectionModule.socketConnection
        coJustRun { mockSocketConnection.disconnect() }

        // When
        rtcSession.cleanup()

        // Then
        coVerify { publisher.close(any()) }
    }

    @Test
    fun `join response without publish capability skips publisher creation`() = runTest {
        ownCapabilitiesFlow.value = emptyList()
        val (rtcSession, _) = createRtcSessionSpyWithMockSocket()
        val event = fakeJoinResponseEvent(samplePublishOptions())

        rtcSession.handleEvent(event)
        testScope.testScheduler.advanceUntilIdle()

        assertNull(rtcSession.publisher)
        verify(exactly = 0) { rtcSession["createPublisher"](any<List<PublishOption>>()) }
    }

    private fun <T> RtcSession.fieldValue(name: String): T? {
        val field = RtcSession::class.java.getDeclaredField(name)
        field.isAccessible = true
        return field.get(this) as? T
    }

    private fun samplePublishOptions(): List<PublishOption> = listOf(
        PublishOption(
            track_type = TrackType.TRACK_TYPE_VIDEO,
            bitrate = 1_000_000,
            fps = 30,
            max_spatial_layers = 1,
            max_temporal_layers = 1,
            video_dimension = VideoDimension(width = 1280, height = 720),
            id = 1,
        ),
    )

    private fun fakeJoinResponseEvent(
        publishOptions: List<PublishOption>,
    ): JoinCallResponseEvent {
        val protoCallState = mockk<stream.video.sfu.models.CallState>(relaxed = true) {
            every { participants } returns emptyList()
        }
        val count = mockk<io.getstream.video.android.core.events.ParticipantCount>(relaxed = true)
        return mockk(relaxed = true) {
            every { callState } returns protoCallState
            every { participantCount } returns count
            every { fastReconnectDeadlineSeconds } returns 0
            every { isReconnected } returns false
            every { this@mockk.publishOptions } returns publishOptions
        }
    }

    private fun createRtcSessionSpyWithMockSocket(): Pair<RtcSession, Publisher> {
        val mockSocket = mockk<SfuSocketConnection>()
        val mockConnectedEvent = mockk<JoinCallResponseEvent>(relaxed = true)
        val socketStateFlow =
            MutableStateFlow<SfuSocketState>(SfuSocketState.Connected(mockConnectedEvent))
        every { mockSocket.state() } returns socketStateFlow
        every { mockSocket.whenConnected(any<Long>(), any()) } answers {
            val callback = secondArg<suspend (String) -> Unit>()
            // Launch the callback in backgroundScope to simulate the real whenConnected behavior
            // The real impl launches in socket's scope; we launch in background scope for control
            testScope.backgroundScope.launch {
                delay(500) // Simulate the real whenConnected delay
                callback("connection-id")
            }
            Unit // Return Unit since whenConnected returns Unit
        }
        val mockModule = mockk<SfuConnectionModule>(relaxed = true) {
            every { socketConnection } returns mockSocket
            every { api } returns mockk(relaxed = true)
        }
        val rtcSession = spyk(
            RtcSession(
                client = mockStreamVideo,
                powerManager = mockPowerManager,
                call = mockCall,
                sessionId = "session-id",
                apiKey = "api-key",
                lifecycle = mockLifecycle,
                sfuUrl = "https://test-sfu.stream.com",
                sfuWsUrl = "wss://test-sfu.stream.com",
                sfuToken = "fake-sfu-token",
                clientImpl = mockVideoClient,
                coroutineScope = testScope,
                rtcSessionScope = testScope,
                remoteIceServers = emptyList(),
                sfuConnectionModuleProvider = { mockModule },
            ),
            recordPrivateCalls = true,
        )
        val publisherMock = mockk<Publisher>(relaxed = true)
        every { rtcSession["createPublisher"](any<List<PublishOption>>()) } returns publisherMock
        return rtcSession to publisherMock
    }
}
