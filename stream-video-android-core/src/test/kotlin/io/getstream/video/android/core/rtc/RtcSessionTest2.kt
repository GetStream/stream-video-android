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
import io.getstream.android.video.generated.models.OwnCapability
import io.getstream.result.Error
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.MediaManagerImpl
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.analytics.call.observer.SfuAnalytics
import io.getstream.video.android.core.analytics.reporting.model.AnalyticsCallAbortReason
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.SfuConnectFailureCause
import io.getstream.video.android.core.call.SfuConnectionResult
import io.getstream.video.android.core.call.components.CallSessionManager
import io.getstream.video.android.core.call.connection.Publisher
import io.getstream.video.android.core.errors.VideoErrorCode
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.SubscriberOfferEvent
import io.getstream.video.android.core.internal.module.SfuConnectionModule
import io.getstream.video.android.core.model.IceServer
import io.getstream.video.android.core.socket.common.ConnectionConf
import io.getstream.video.android.core.socket.sfu.SfuSocketConnection
import io.getstream.video.android.core.socket.sfu.state.SfuSocketState
import io.getstream.webrtc.PeerConnection
import io.getstream.webrtc.SessionDescription
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
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import stream.video.sfu.event.ReconnectDetails
import stream.video.sfu.models.ParticipantCount
import stream.video.sfu.models.PeerType
import stream.video.sfu.models.PublishOption
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.models.WebsocketReconnectStrategy
import stream.video.sfu.signal.StartNoiseCancellationRequest
import stream.video.sfu.signal.StopNoiseCancellationRequest
import java.io.InterruptedIOException

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
        StreamVideo.removeClient()
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
                sessionManager = CallSessionManager(),
                sessionId = sessionId,
                apiKey = apiKey,
                lifecycle = lifecycle,
                sfuUrl = sfuUrl,
                sfuWsUrl = sfuWsUrl,
                sfuToken = sfuToken,
                sfuName = "test-sfu-edge",
                remoteIceServers = remoteIceServers,
                clientImpl = mockVideoClient,
                coroutineScope = testScope,
                sfuConnectionModuleProvider = { mockk(relaxed = true) },
                sfuAnalytics = SfuAnalytics.getFakeSfuAnalytics(),
            ),
        )

        // Then
        assertNotNull(
            "Subscriber StreamPeerConnection should not be null",
            rtcSession.subscriber.value,
        )
        assertEquals("Wrong sessionId", sessionId, rtcSession.fieldValue("sessionId"))
    }

    @Suppress("DEPRECATION")
    @Test
    fun `connect calls socketConnection_connect with JoinRequest and sets state to Connected`() =
        runTest(
            testDispatcher,
        ) {
            // Given
            val sessionId = "test-session-id"
            val apiKey = "test-api-key"
            val sfuUrl = "https://test-sfu.stream.com"
            val sfuWsUrl = "wss://test-sfu.stream.com"
            val sfuToken = "fake-sfu-token"
            val remoteIceServers = emptyList<IceServer>()

            val sfuSocketStateFlow = MutableStateFlow<SfuSocketState>(
                SfuSocketState.Disconnected.Stopped,
            )
            val mockSocketConnection = mockk<SfuSocketConnection>(relaxed = true)
            every { mockSocketConnection.state() } returns sfuSocketStateFlow
            coEvery { mockSocketConnection.connect(any()) } coAnswers {
                sfuSocketStateFlow.value = SfuSocketState.Connected(mockk(relaxed = true))
            }
            val sfuSocketModule = mockk<SfuConnectionModule>(relaxed = true)
            every { sfuSocketModule.socketConnection } returns mockSocketConnection

            val rtcSession = spyk(
                RtcSession(
                    client = mockStreamVideo,
                    powerManager = mockPowerManager,
                    call = mockCall,
                    sessionManager = CallSessionManager(),
                    sessionId = sessionId,
                    apiKey = apiKey,
                    lifecycle = mockLifecycle,
                    sfuUrl = sfuUrl,
                    sfuWsUrl = sfuWsUrl,
                    sfuToken = sfuToken,
                    sfuName = "test-sfu-edge",
                    clientImpl = mockVideoClient,
                    coroutineScope = testScope,
                    remoteIceServers = remoteIceServers,
                    sfuConnectionModuleProvider = { sfuSocketModule },
                    sfuAnalytics = SfuAnalytics.getFakeSfuAnalytics(),
                ),
            )
            coJustRun { rtcSession.sendCallStats(any(), any(), any()) }

            // When
            rtcSession.connect()

            // Then
            coVerify {
                mockSocketConnection.connect(
                    match { request ->
                        request.session_id == sessionId && request.token == sfuToken
                    },
                )
            }
        }

    @Suppress("DEPRECATION")
    @Test
    fun `connectInternal returns Failed when the socket reports a connection timeout`() =
        runTest(testDispatcher) {
            val sfuSocketStateFlow = MutableStateFlow<SfuSocketState>(
                SfuSocketState.Disconnected.Stopped,
            )
            val mockSocketConnection = mockk<SfuSocketConnection>(relaxed = true)
            every { mockSocketConnection.state() } returns sfuSocketStateFlow
            // The socket layer surfaces timeouts as DisconnectedTemporarily; connectInternal
            // also has a safety timeout if the state machine never reaches a terminal state.
            coEvery { mockSocketConnection.connect(any()) } coAnswers {
                sfuSocketStateFlow.value =
                    SfuSocketState.Disconnected.DisconnectedTemporarily(
                        Error.NetworkError(
                            message = VideoErrorCode.SFU_JOIN_RESPONSE_TIMEOUT.description,
                            serverErrorCode = VideoErrorCode.SFU_JOIN_RESPONSE_TIMEOUT.code,
                            statusCode = -1,
                        ),
                        WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
                    )
            }
            val sfuSocketModule = mockk<SfuConnectionModule>(relaxed = true)
            every { sfuSocketModule.socketConnection } returns mockSocketConnection

            val rtcSession = spyk(
                RtcSession(
                    client = mockStreamVideo,
                    powerManager = mockPowerManager,
                    call = mockCall,
                    sessionManager = CallSessionManager(),
                    sessionId = "test-session-id",
                    apiKey = "test-api-key",
                    lifecycle = mockLifecycle,
                    sfuUrl = "https://test-sfu.stream.com",
                    sfuWsUrl = "wss://test-sfu.stream.com",
                    sfuToken = "fake-sfu-token",
                    sfuName = "test-sfu-edge",
                    clientImpl = mockVideoClient,
                    coroutineScope = testScope,
                    remoteIceServers = emptyList(),
                    sfuConnectionModuleProvider = { sfuSocketModule },
                    sfuAnalytics = SfuAnalytics.getFakeSfuAnalytics(),
                ),
            )
            coJustRun { rtcSession.sendCallStats(any(), any(), any()) }

            val result = rtcSession.connectInternal()

            assertTrue(
                "Expected SfuConnectionResult.Failure but got $result",
                result is SfuConnectionResult.Failure,
            )
            assertTrue(
                "Expected timeout message",
                (result as SfuConnectionResult.Failure).error.message!!.contains("timed out"),
            )
            assertEquals(
                SfuConnectFailureCause.RecoverableSocketFailure,
                result.cause,
            )
            assertEquals(
                AnalyticsCallAbortReason.REQUEST_TIMEOUT,
                result.abortReason,
            )
        }

    @Suppress("DEPRECATION")
    @Test
    fun `connectInternal socket state observation timeout fires when socket stays non-terminal`() =
        runTest(testDispatcher) {
            val sfuSocketStateFlow = MutableStateFlow<SfuSocketState>(
                SfuSocketState.Disconnected.Stopped,
            )
            val mockSocketConnection = mockk<SfuSocketConnection>(relaxed = true)
            every { mockSocketConnection.state() } returns sfuSocketStateFlow
            coEvery { mockSocketConnection.connect(any()) } coAnswers {
                sfuSocketStateFlow.value = SfuSocketState.Connecting(
                    mockk<ConnectionConf.SfuConnectionConf>(relaxed = true),
                )
            }
            val sfuSocketModule = mockk<SfuConnectionModule>(relaxed = true)
            every { sfuSocketModule.socketConnection } returns mockSocketConnection
            every { mockVideoClient.connectionTimeoutInMs } returns 50L

            val rtcSession = spyk(
                RtcSession(
                    client = mockStreamVideo,
                    powerManager = mockPowerManager,
                    call = mockCall,
                    sessionManager = CallSessionManager(),
                    sessionId = "test-session-id",
                    apiKey = "test-api-key",
                    lifecycle = mockLifecycle,
                    sfuUrl = "https://test-sfu.stream.com",
                    sfuWsUrl = "wss://test-sfu.stream.com",
                    sfuToken = "fake-sfu-token",
                    sfuName = "test-sfu-edge",
                    clientImpl = mockVideoClient,
                    coroutineScope = testScope,
                    remoteIceServers = emptyList(),
                    sfuConnectionModuleProvider = { sfuSocketModule },
                    sfuAnalytics = SfuAnalytics.getFakeSfuAnalytics(),
                ),
            )
            coJustRun { rtcSession.sendCallStats(any(), any(), any()) }

            val resultDeferred = async { rtcSession.connectInternal() }
            // Socket state observation timeout = 2 * 50ms + 1000ms grace
            advanceTimeBy(1_101L)
            val result = resultDeferred.await()

            assertTrue(
                "Expected SfuConnectionResult.Failure but got $result",
                result is SfuConnectionResult.Failure,
            )
            assertTrue(
                "Expected socket state observation timeout message",
                (result as SfuConnectionResult.Failure).error.message!!.contains("timed out"),
            )
            assertEquals(
                SfuConnectFailureCause.SocketStateObservationTimeout,
                result.cause,
            )
            assertEquals(
                AnalyticsCallAbortReason.REQUEST_TIMEOUT,
                result.abortReason,
            )
            // The abandoned, still-in-flight socket must be torn down so a late
            // Connected can't resurface through stateJob and resurrect the session.
            coVerify { mockSocketConnection.disconnect() }
        }

    @Suppress("DEPRECATION")
    @Test
    fun `connectInternal maps OkHttp InterruptedIOException timeout to REQUEST_TIMEOUT abort reason`() =
        runTest(testDispatcher) {
            val sfuSocketStateFlow = MutableStateFlow<SfuSocketState>(
                SfuSocketState.Disconnected.Stopped,
            )
            val mockSocketConnection = mockk<SfuSocketConnection>(relaxed = true)
            every { mockSocketConnection.state() } returns sfuSocketStateFlow
            coEvery { mockSocketConnection.connect(any()) } coAnswers {
                sfuSocketStateFlow.value =
                    SfuSocketState.Disconnected.DisconnectedTemporarily(
                        Error.NetworkError(
                            message = "timeout",
                            serverErrorCode = VideoErrorCode.SOCKET_FAILURE.code,
                            statusCode = -1,
                            cause = InterruptedIOException("timeout"),
                        ),
                        WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_UNSPECIFIED,
                    )
            }
            val sfuSocketModule = mockk<SfuConnectionModule>(relaxed = true)
            every { sfuSocketModule.socketConnection } returns mockSocketConnection

            val rtcSession = spyk(
                RtcSession(
                    client = mockStreamVideo,
                    powerManager = mockPowerManager,
                    call = mockCall,
                    sessionManager = CallSessionManager(),
                    sessionId = "test-session-id",
                    apiKey = "test-api-key",
                    lifecycle = mockLifecycle,
                    sfuUrl = "https://test-sfu.stream.com",
                    sfuWsUrl = "wss://test-sfu.stream.com",
                    sfuToken = "fake-sfu-token",
                    sfuName = "test-sfu-edge",
                    clientImpl = mockVideoClient,
                    coroutineScope = testScope,
                    remoteIceServers = emptyList(),
                    sfuConnectionModuleProvider = { sfuSocketModule },
                    sfuAnalytics = SfuAnalytics.getFakeSfuAnalytics(),
                ),
            )
            coJustRun { rtcSession.sendCallStats(any(), any(), any()) }

            val result = rtcSession.connectInternal()

            assertTrue(
                "Expected SfuConnectionResult.Failure but got $result",
                result is SfuConnectionResult.Failure,
            )
            assertEquals(
                AnalyticsCallAbortReason.REQUEST_TIMEOUT,
                (result as SfuConnectionResult.Failure).abortReason,
            )
            assertEquals(
                SfuConnectFailureCause.RecoverableSocketFailure,
                result.cause,
            )
        }

    @Suppress("DEPRECATION")
    @Test
    fun `connectInternal returns non-recoverable Failed on a permanent disconnect`() =
        runTest(testDispatcher) {
            val sfuSocketStateFlow = MutableStateFlow<SfuSocketState>(
                SfuSocketState.Disconnected.Stopped,
            )
            val mockSocketConnection = mockk<SfuSocketConnection>(relaxed = true)
            every { mockSocketConnection.state() } returns sfuSocketStateFlow
            coEvery { mockSocketConnection.connect(any()) } coAnswers {
                sfuSocketStateFlow.value = SfuSocketState.Disconnected.DisconnectedPermanently(
                    Error.NetworkError(
                        message = "permanent auth error",
                        serverErrorCode = 0,
                        statusCode = -1,
                    ),
                )
            }
            val sfuSocketModule = mockk<SfuConnectionModule>(relaxed = true)
            every { sfuSocketModule.socketConnection } returns mockSocketConnection

            val rtcSession = spyk(
                RtcSession(
                    client = mockStreamVideo,
                    powerManager = mockPowerManager,
                    call = mockCall,
                    sessionManager = CallSessionManager(),
                    sessionId = "test-session-id",
                    apiKey = "test-api-key",
                    lifecycle = mockLifecycle,
                    sfuUrl = "https://test-sfu.stream.com",
                    sfuWsUrl = "wss://test-sfu.stream.com",
                    sfuToken = "fake-sfu-token",
                    sfuName = "test-sfu-edge",
                    clientImpl = mockVideoClient,
                    coroutineScope = testScope,
                    remoteIceServers = emptyList(),
                    sfuConnectionModuleProvider = { sfuSocketModule },
                    sfuAnalytics = SfuAnalytics.getFakeSfuAnalytics(),
                ),
            )
            coJustRun { rtcSession.sendCallStats(any(), any(), any()) }

            val result = rtcSession.connectInternal()

            assertTrue(
                "Expected SfuConnectionResult.Failure but got $result",
                result is SfuConnectionResult.Failure,
            )
            assertEquals(
                SfuConnectFailureCause.TerminalSocketFailure,
                (result as SfuConnectionResult.Failure).cause,
            )
        }

    @Suppress("DEPRECATION")
    @Test
    fun `connectInternal forwards ReconnectDetails in the JoinRequest`() =
        runTest(testDispatcher) {
            val sfuSocketStateFlow = MutableStateFlow<SfuSocketState>(
                SfuSocketState.Disconnected.Stopped,
            )
            val mockSocketConnection = mockk<SfuSocketConnection>(relaxed = true)
            every { mockSocketConnection.state() } returns sfuSocketStateFlow
            coEvery { mockSocketConnection.connect(any()) } coAnswers {
                sfuSocketStateFlow.value = SfuSocketState.Connected(mockk(relaxed = true))
            }
            val sfuSocketModule = mockk<SfuConnectionModule>(relaxed = true)
            every { sfuSocketModule.socketConnection } returns mockSocketConnection

            val rtcSession = spyk(
                RtcSession(
                    client = mockStreamVideo,
                    powerManager = mockPowerManager,
                    call = mockCall,
                    sessionManager = CallSessionManager(),
                    sessionId = "test-session-id",
                    apiKey = "test-api-key",
                    lifecycle = mockLifecycle,
                    sfuUrl = "https://test-sfu.stream.com",
                    sfuWsUrl = "wss://test-sfu.stream.com",
                    sfuToken = "fake-sfu-token",
                    sfuName = "test-sfu-edge",
                    clientImpl = mockVideoClient,
                    coroutineScope = testScope,
                    remoteIceServers = emptyList(),
                    sfuConnectionModuleProvider = { sfuSocketModule },
                    sfuAnalytics = SfuAnalytics.getFakeSfuAnalytics(),
                ),
            )
            coJustRun { rtcSession.sendCallStats(any(), any(), any()) }

            val reconnectDetails = ReconnectDetails(
                strategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
                previous_session_id = "old-session-id",
                announced_tracks = emptyList(),
                subscriptions = emptyList(),
                from_sfu_id = "",
                reconnect_attempt = 1,
                reason = "test-rejoin",
            )

            val result = rtcSession.connectInternal(reconnectDetails = reconnectDetails)

            assertEquals(SfuConnectionResult.Success, result)
            coVerify {
                mockSocketConnection.connect(
                    match { request ->
                        request.reconnect_details == reconnectDetails
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
                    sessionManager = CallSessionManager(),
                    sessionId = sessionId,
                    apiKey = apiKey,
                    lifecycle = mockLifecycle,
                    sfuUrl = sfuUrl,
                    sfuWsUrl = sfuWsUrl,
                    sfuToken = sfuToken,
                    sfuName = "test-sfu-edge",
                    clientImpl = mockVideoClient,
                    coroutineScope = testScope,
                    remoteIceServers = remoteIceServers,
                    sfuConnectionModuleProvider = { mockk(relaxed = true) },
                    sfuAnalytics = SfuAnalytics.getFakeSfuAnalytics(),
                ),
            )
            val subscriber = rtcSession.subscriber
            assertNotNull("Subscriber must not be null", subscriber.value)

            val fakeSdpOffer = "fake-offer-sdp"
            val offerEvent = SubscriberOfferEvent(
                sdp = fakeSdpOffer,
            )
            coEvery {
                subscriber.value!!.setRemoteDescription(any())
            } returns io.getstream.result.Result.Success(
                Unit,
            )
            coEvery { subscriber.value!!.createAnswer() } returns io.getstream.result.Result.Success(
                SessionDescription(SessionDescription.Type.ANSWER, "fake-answer-sdp"),
            )
            coEvery {
                subscriber.value!!.setLocalDescription(any())
            } returns io.getstream.result.Result.Success(
                Unit,
            )
            rtcSession.handleSubscriberOffer(offerEvent)

            coVerify {
                subscriber.value!!.negotiate(
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
            sessionManager = CallSessionManager(),
            sessionId = "session-id",
            apiKey = "api-key",
            lifecycle = mockLifecycle,
            sfuUrl = "https://test-sfu.stream.com",
            sfuWsUrl = "wss://test-sfu.stream.com",
            sfuToken = "fake-sfu-token",
            sfuName = "test-sfu-edge",
            clientImpl = mockVideoClient,
            coroutineScope = testScope,
            rtcSessionScope = testScope,
            remoteIceServers = emptyList(),
            sfuConnectionModuleProvider = { mockModule },
            sfuAnalytics = SfuAnalytics.getFakeSfuAnalytics(),
        )
        // Confirm publisher is null
        assertNull(rtcSession.publisher.value)

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
                sessionManager = CallSessionManager(),
                sessionId = "session-id",
                apiKey = "api-key",
                lifecycle = mockLifecycle,
                sfuUrl = "https://test-sfu.stream.com",
                sfuWsUrl = "wss://test-sfu.stream.com",
                sfuToken = "fake-sfu-token",
                sfuName = "test-sfu-edge",
                clientImpl = mockVideoClient,
                coroutineScope = testScope,
                remoteIceServers = emptyList(),
                sfuConnectionModuleProvider = { mockk(relaxed = true) },
                sfuAnalytics = SfuAnalytics.getFakeSfuAnalytics(),
            )
            val mockPublisher = mockk<Publisher>(relaxed = true)
            rtcSession.publisher.value = mockPublisher
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
            sessionManager = CallSessionManager(),
            sessionId = sessionId,
            apiKey = "test-api-key",
            lifecycle = mockLifecycle,
            sfuUrl = "https://test-sfu.stream.com",
            sfuWsUrl = "wss://test-sfu.stream.com",
            sfuToken = "fake-sfu-token",
            sfuName = "test-sfu-edge",
            clientImpl = mockVideoClient,
            coroutineScope = testScope,
            remoteIceServers = emptyList(),
            sfuConnectionModuleProvider = { mockk(relaxed = true) },
            sfuAnalytics = SfuAnalytics.getFakeSfuAnalytics(),
        )
        val subscriber = rtcSession.subscriber.value
        assertNotNull(subscriber)
        val publisher = mockk<Publisher>(relaxed = true)
        rtcSession.publisher.value = publisher
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

        assertNull(rtcSession.publisher.value)
        verify(exactly = 0) { rtcSession["createPublisher"](any<List<PublishOption>>()) }
    }

    @Test
    fun `createAndPublishAudioTrack does not crash when publishStream returns null`() = runTest(
        testDispatcher,
    ) {
        ownCapabilitiesFlow.value = listOf(OwnCapability.SendAudio)
        val (rtcSession, publisherMock) = createRtcSessionSpyWithMockSocket()
        rtcSession.publisher.value = publisherMock
        coEvery {
            publisherMock.publishStream(any(), TrackType.TRACK_TYPE_AUDIO)
        } returns null

        rtcSession.createAndPublishAudioTrack()

        coVerify { publisherMock.publishStream(any(), TrackType.TRACK_TYPE_AUDIO) }
        // Unmuting before a failed publish would tell the SFU we are live with no track.
        verify(exactly = 0) {
            rtcSession["setMuteState"](true, TrackType.TRACK_TYPE_AUDIO)
        }
    }

    @Test
    fun `createAndPublishAudioTrack unmutes only after publishStream returns a track`() = runTest(
        testDispatcher,
    ) {
        ownCapabilitiesFlow.value = listOf(OwnCapability.SendAudio)
        val (rtcSession, publisherMock) = createRtcSessionSpyWithMockSocket()
        rtcSession.publisher.value = publisherMock
        val audioTrack = mockk<io.getstream.webrtc.AudioTrack>(relaxed = true)
        coEvery {
            publisherMock.publishStream(any(), TrackType.TRACK_TYPE_AUDIO)
        } returns audioTrack

        rtcSession.createAndPublishAudioTrack()

        verify(exactly = 1) {
            rtcSession["setMuteState"](true, TrackType.TRACK_TYPE_AUDIO)
        }
    }

    @Test
    fun `createAndPublishAudioTrack does not crash when publisher is missing`() = runTest(
        testDispatcher,
    ) {
        ownCapabilitiesFlow.value = listOf(OwnCapability.SendAudio)
        val (rtcSession, _) = createRtcSessionSpyWithMockSocket()
        rtcSession.publisher.value = null

        rtcSession.createAndPublishAudioTrack()

        verify(exactly = 0) {
            rtcSession["setMuteState"](true, TrackType.TRACK_TYPE_AUDIO)
        }
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

    @Test
    fun `iceHealthTransition recovers a reconnecting call when no ICE side is bad`() {
        // The subscriber has nothing to negotiate after a reconnect and stays NEW; that must
        // not block the recovery (it deadlocked the connection state as Reconnecting forever).
        assertEquals(
            RealtimeConnection.Connected,
            RtcSession.iceHealthTransition(
                connection = RealtimeConnection.Reconnecting,
                sfuSocketConnected = true,
                publisherIce = PeerConnection.IceConnectionState.CONNECTED,
                subscriberIce = PeerConnection.IceConnectionState.NEW,
            ),
        )
        // No peer connections at all: the connected socket is the only transport signal.
        assertEquals(
            RealtimeConnection.Connected,
            RtcSession.iceHealthTransition(
                connection = RealtimeConnection.Reconnecting,
                sfuSocketConnected = true,
                publisherIce = null,
                subscriberIce = null,
            ),
        )
    }

    @Test
    fun `iceHealthTransition does not recover while an ICE side is bad or the socket is down`() {
        assertNull(
            RtcSession.iceHealthTransition(
                connection = RealtimeConnection.Reconnecting,
                sfuSocketConnected = true,
                publisherIce = PeerConnection.IceConnectionState.DISCONNECTED,
                subscriberIce = PeerConnection.IceConnectionState.NEW,
            ),
        )
        assertNull(
            RtcSession.iceHealthTransition(
                connection = RealtimeConnection.Reconnecting,
                sfuSocketConnected = true,
                publisherIce = PeerConnection.IceConnectionState.CONNECTED,
                subscriberIce = PeerConnection.IceConnectionState.DISCONNECTED,
            ),
        )
        assertNull(
            RtcSession.iceHealthTransition(
                connection = RealtimeConnection.Reconnecting,
                sfuSocketConnected = false,
                publisherIce = PeerConnection.IceConnectionState.CONNECTED,
                subscriberIce = PeerConnection.IceConnectionState.CONNECTED,
            ),
        )
        // A closed peer connection never emits another ICE event, so it must block recovery.
        assertNull(
            RtcSession.iceHealthTransition(
                connection = RealtimeConnection.Reconnecting,
                sfuSocketConnected = true,
                publisherIce = PeerConnection.IceConnectionState.CONNECTED,
                subscriberIce = PeerConnection.IceConnectionState.CLOSED,
            ),
        )
        assertNull(
            RtcSession.iceHealthTransition(
                connection = RealtimeConnection.Reconnecting,
                sfuSocketConnected = true,
                publisherIce = PeerConnection.IceConnectionState.CLOSED,
                subscriberIce = null,
            ),
        )
    }

    @Test
    fun `iceHealthTransition degrades a connected call when an ICE side goes bad`() {
        assertEquals(
            RealtimeConnection.Reconnecting,
            RtcSession.iceHealthTransition(
                connection = RealtimeConnection.Connected,
                sfuSocketConnected = true,
                publisherIce = PeerConnection.IceConnectionState.FAILED,
                subscriberIce = PeerConnection.IceConnectionState.NEW,
            ),
        )
        assertEquals(
            RealtimeConnection.Reconnecting,
            RtcSession.iceHealthTransition(
                connection = RealtimeConnection.Connected,
                sfuSocketConnected = true,
                publisherIce = PeerConnection.IceConnectionState.CONNECTED,
                subscriberIce = PeerConnection.IceConnectionState.FAILED,
            ),
        )
        assertNull(
            RtcSession.iceHealthTransition(
                connection = RealtimeConnection.Connected,
                sfuSocketConnected = true,
                publisherIce = PeerConnection.IceConnectionState.CONNECTED,
                subscriberIce = PeerConnection.IceConnectionState.NEW,
            ),
        )
        // CLOSED does not degrade: peer connections close during legitimate teardowns and
        // the closing flow owns the connection state there.
        assertNull(
            RtcSession.iceHealthTransition(
                connection = RealtimeConnection.Connected,
                sfuSocketConnected = true,
                publisherIce = PeerConnection.IceConnectionState.CONNECTED,
                subscriberIce = PeerConnection.IceConnectionState.CLOSED,
            ),
        )
    }

    @Test
    fun `evaluateIceHealth applies the transition to the connection state`() = runTest(
        testDispatcher,
    ) {
        every { mockCallState.connection } returns
            MutableStateFlow<RealtimeConnection>(RealtimeConnection.Connected)
        val internalConnection = mockk<MutableStateFlow<RealtimeConnection>>(relaxed = true)
        every { mockCallState._connection } returns internalConnection

        val rtcSession = RtcSession(
            client = mockStreamVideo,
            powerManager = mockPowerManager,
            call = mockCall,
            sessionManager = CallSessionManager(),
            sessionId = "test-session-id",
            apiKey = "test-api-key",
            lifecycle = mockLifecycle,
            sfuUrl = "https://test-sfu.stream.com",
            sfuWsUrl = "wss://test-sfu.stream.com",
            sfuToken = "fake-sfu-token",
            sfuName = "test-sfu-edge",
            clientImpl = mockVideoClient,
            coroutineScope = testScope,
            remoteIceServers = emptyList(),
            sfuConnectionModuleProvider = { mockk(relaxed = true) },
            sfuAnalytics = SfuAnalytics.getFakeSfuAnalytics(),
        )
        every { rtcSession.subscriber.value!!.iceState } returns
            MutableStateFlow<PeerConnection.IceConnectionState?>(
                PeerConnection.IceConnectionState.FAILED,
            )

        rtcSession.evaluateIceHealth()

        verify { internalConnection.value = RealtimeConnection.Reconnecting }
    }

    private fun createRtcSessionSpyWithMockSocket(): Pair<RtcSession, Publisher> {
        val mockSocket = mockk<SfuSocketConnection>()
        val mockConnectedEvent = mockk<JoinCallResponseEvent>(relaxed = true)
        val socketStateFlow =
            MutableStateFlow<SfuSocketState>(SfuSocketState.Connected(mockConnectedEvent))
        every { mockSocket.state() } returns socketStateFlow
        every { mockSocket.whenConnected(any<Long>(), any(), any()) } answers {
            val callback = thirdArg<suspend (String) -> Unit>()
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
        }
        val rtcSession = spyk(
            RtcSession(
                client = mockStreamVideo,
                powerManager = mockPowerManager,
                call = mockCall,
                sessionManager = CallSessionManager(),
                sessionId = "session-id",
                apiKey = "api-key",
                lifecycle = mockLifecycle,
                sfuUrl = "https://test-sfu.stream.com",
                sfuWsUrl = "wss://test-sfu.stream.com",
                sfuToken = "fake-sfu-token",
                sfuName = "test-sfu-edge",
                clientImpl = mockVideoClient,
                coroutineScope = testScope,
                rtcSessionScope = testScope,
                remoteIceServers = emptyList(),
                sfuConnectionModuleProvider = { mockModule },
                sfuAnalytics = SfuAnalytics.getFakeSfuAnalytics(),
            ),
            recordPrivateCalls = true,
        )
        val publisherMock = mockk<Publisher>(relaxed = true)
        every { rtcSession["createPublisher"](any<List<PublishOption>>()) } returns publisherMock
        return rtcSession to publisherMock
    }

    @Test
    fun `startNoiseCancellation sends the request to the SFU with the session id`() = runTest {
        // Given
        val signalService = mockk<SignalServerService>(relaxed = true)
        val (rtcSession, _) = noiseCancellationSession(signalService)

        // When
        rtcSession.startNoiseCancellation()

        // Then
        coVerify(exactly = 1) {
            signalService.startNoiseCancellation(
                StartNoiseCancellationRequest(session_id = "session-id"),
            )
        }
    }

    @Test
    fun `stopNoiseCancellation sends the request to the SFU with the session id`() = runTest {
        // Given
        val signalService = mockk<SignalServerService>(relaxed = true)
        val (rtcSession, _) = noiseCancellationSession(signalService)

        // When
        rtcSession.stopNoiseCancellation()

        // Then
        coVerify(exactly = 1) {
            signalService.stopNoiseCancellation(
                StopNoiseCancellationRequest(session_id = "session-id"),
            )
        }
    }

    private fun noiseCancellationSession(
        signalService: SignalServerService,
    ): Pair<RtcSession, SfuConnectionModule> {
        val mockModule = mockk<SfuConnectionModule>(relaxed = true) {
            every { api } returns signalService
        }
        val rtcSession = RtcSession(
            client = mockStreamVideo,
            powerManager = mockPowerManager,
            call = mockCall,
            sessionManager = CallSessionManager(),
            sessionId = "session-id",
            apiKey = "api-key",
            lifecycle = mockLifecycle,
            sfuUrl = "https://test-sfu.stream.com",
            sfuWsUrl = "wss://test-sfu.stream.com",
            sfuToken = "fake-sfu-token",
            sfuName = "test-sfu-edge",
            clientImpl = mockVideoClient,
            coroutineScope = testScope,
            rtcSessionScope = testScope,
            remoteIceServers = emptyList(),
            sfuConnectionModuleProvider = { mockModule },
            sfuAnalytics = SfuAnalytics.getFakeSfuAnalytics(),
        )
        return rtcSession to mockModule
    }
}
