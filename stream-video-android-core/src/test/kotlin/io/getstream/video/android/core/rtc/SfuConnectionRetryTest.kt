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
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.base.DispatcherRule
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.internal.module.CoordinatorConnectionModule
import io.getstream.video.android.core.internal.module.SfuConnectionModule
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.model.IceServer
import io.getstream.video.android.core.socket.sfu.SfuSocketConnection
import io.getstream.video.android.core.socket.sfu.state.SfuSocketState
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.unmockkAll
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
import stream.video.sfu.models.WebsocketReconnectStrategy

/**
 * Tests that [RtcSession]'s stateJob correctly delegates SFU socket-state
 * transitions to [Call.reconnect] (the unified reconnection entry point).
 *
 * The retry-loop escalation logic (FAST → REJOIN, MIGRATE → REJOIN, etc.)
 * is tested separately; this class only verifies the thin forwarding layer
 * inside RtcSession.
 */
class SfuConnectionRetryTest {

    @get:Rule
    val dispatcherRule = DispatcherRule()

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

    private lateinit var socketStateFlow: MutableStateFlow<SfuSocketState>
    private lateinit var mockSocket: SfuSocketConnection
    private lateinit var mockModule: SfuConnectionModule
    private lateinit var mockNetworkStateProvider: NetworkStateProvider

    private var disconnectCounter = 0

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        mockStreamVideo = mockk(relaxed = true)
        mockNetworkStateProvider = mockk(relaxed = true)
        every { mockNetworkStateProvider.isConnected() } returns true
        val mockCoordinatorModule = mockk<CoordinatorConnectionModule>(relaxed = true)
        every { mockCoordinatorModule.networkStateProvider } returns mockNetworkStateProvider
        every { mockVideoClient.coordinatorConnectionModule } returns mockCoordinatorModule

        every { mockCall.state } returns mockCallState
        every { mockCall.scope } returns testScope
        every { mockCall.mediaManager } returns mockMediaManager
        every { mockCall.peerConnectionFactory } returns mockk(relaxed = true) {
            every { makePeerConnection(any(), any(), any(), any()) } returns mockk(relaxed = true)
        }
        every { mockCallState.ownCapabilities } returns ownCapabilitiesFlow
        every { mockCallState.participants } returns participantsFlow
        every { mockCallState.remoteParticipants } returns remoteParticipantsFlow
        every { mockCallState.replaceParticipants(any()) } answers { }
        every { mockCallState.me.value } returns null
        StreamVideo.install(mockStreamVideo)
        disconnectCounter = 0

        socketStateFlow = MutableStateFlow<SfuSocketState>(SfuSocketState.Disconnected.Stopped)
        mockSocket = mockk(relaxed = true)
        every { mockSocket.state() } returns socketStateFlow
        every { mockSocket.errors() } returns MutableSharedFlow()
        every { mockSocket.events() } returns MutableSharedFlow()
        mockModule = mockk(relaxed = true) {
            every { socketConnection } returns mockSocket
        }
    }

    @After
    fun tearDown() {
        StreamVideo.removeClient()
        unmockkAll()
    }

    private fun createRtcSession(sfuName: String = "sfu-edge-1"): RtcSession {
        return RtcSession(
            client = mockStreamVideo,
            powerManager = mockPowerManager,
            call = mockCall,
            sessionId = "test-session-id",
            apiKey = "test-api-key",
            lifecycle = mockLifecycle,
            sfuUrl = "https://test-sfu.stream.com",
            sfuWsUrl = "wss://test-sfu.stream.com",
            sfuToken = "fake-sfu-token",
            sfuName = sfuName,
            clientImpl = mockVideoClient,
            coroutineScope = testScope,
            rtcSessionScope = testScope,
            remoteIceServers = emptyList<IceServer>(),
            sfuConnectionModuleProvider = { mockModule },
        )
    }

    private fun disconnectedTemporarily(
        strategy: WebsocketReconnectStrategy =
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_UNSPECIFIED,
    ) = SfuSocketState.Disconnected.DisconnectedTemporarily(
        error = Error.NetworkError(
            message = "Connection failed #${++disconnectCounter}",
            serverErrorCode = 1003,
            statusCode = 1003,
        ),
        reconnectStrategy = strategy,
    )

    private fun advance() = testScope.advanceUntilIdle()

    // -- Forwarding tests: stateJob delegates to call.reconnect --

    @Test
    fun `UNSPECIFIED strategy forwards to call reconnect`() = runTest {
        createRtcSession()
        advance()

        socketStateFlow.value = disconnectedTemporarily()
        advance()

        coVerify(exactly = 1) {
            mockCall.reconnect(
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_UNSPECIFIED,
                any(),
            )
        }
    }

    @Test
    fun `MIGRATE strategy forwards to call reconnect`() = runTest {
        createRtcSession()
        advance()

        socketStateFlow.value = disconnectedTemporarily(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE,
        )
        advance()

        coVerify(exactly = 1) {
            mockCall.reconnect(
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE,
                any(),
            )
        }
    }

    @Test
    fun `REJOIN strategy forwards to call reconnect`() = runTest {
        createRtcSession()
        advance()

        socketStateFlow.value = disconnectedTemporarily(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
        )
        advance()

        coVerify(exactly = 1) {
            mockCall.reconnect(
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
                any(),
            )
        }
    }

    @Test
    fun `FAST strategy forwards to call reconnect`() = runTest {
        createRtcSession()
        advance()

        socketStateFlow.value = disconnectedTemporarily(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
        )
        advance()

        coVerify(exactly = 1) {
            mockCall.reconnect(
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
                any(),
            )
        }
    }

    @Test
    fun `DISCONNECT strategy forwards to call reconnect`() = runTest {
        createRtcSession()
        advance()

        socketStateFlow.value = disconnectedTemporarily(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_DISCONNECT,
        )
        advance()

        coVerify(exactly = 1) {
            mockCall.reconnect(
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_DISCONNECT,
                any(),
            )
        }
    }

    // -- Connected state --

    @Test
    fun `Connected state calls onSfuConnectionEstablished`() = runTest {
        val connectedEvent = mockk<JoinCallResponseEvent>(relaxed = true)
        createRtcSession()
        advance()

        socketStateFlow.value = SfuSocketState.Connected(connectedEvent)
        advance()

        coVerify(exactly = 1) { mockCall.onSfuConnectionEstablished() }
    }

    // -- WebSocketEventLost --

    @Test
    fun `WebSocketEventLost triggers FAST reconnect`() = runTest {
        createRtcSession()
        advance()

        socketStateFlow.value = SfuSocketState.Disconnected.WebSocketEventLost
        advance()

        coVerify(exactly = 1) {
            mockCall.reconnect(
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
                match { it.contains("healthcheck-timeout") },
            )
        }
    }

    // -- Network-aware guard --
    // Network availability is checked inside Call.reconnect, not in listenToSfuSocket.
    // The stateJob always forwards DisconnectedTemporarily to call.reconnect regardless
    // of network state.

    @Test
    fun `DisconnectedTemporarily forwards to reconnect even when network is down`() = runTest {
        every { mockNetworkStateProvider.isConnected() } returns false
        createRtcSession()
        advance()

        socketStateFlow.value = disconnectedTemporarily(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
        )
        advance()

        coVerify(exactly = 1) {
            mockCall.reconnect(
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
                any(),
            )
        }
    }

    // -- DisconnectedPermanently --
    // DisconnectedPermanently is not handled in listenToSfuSocket (falls through
    // to the else branch). The socket is considered unrecoverable at this layer;
    // reconnect is NOT triggered from the stateJob.

    @Test
    fun `DisconnectedPermanently does not trigger call reconnect`() = runTest {
        createRtcSession()
        advance()

        socketStateFlow.value = SfuSocketState.Disconnected.DisconnectedPermanently(
            error = Error.NetworkError(
                message = "Socket closed (1002)",
                serverErrorCode = 1002,
                statusCode = 1002,
            ),
        )
        advance()

        coVerify(exactly = 0) { mockCall.reconnect(any(), any()) }
    }

    @Test
    fun `DisconnectedPermanently does not trigger reconnect even when network is down`() = runTest {
        every { mockNetworkStateProvider.isConnected() } returns false
        createRtcSession()
        advance()

        socketStateFlow.value = SfuSocketState.Disconnected.DisconnectedPermanently(
            error = Error.NetworkError(
                message = "Socket closed (1002)",
                serverErrorCode = 1002,
                statusCode = 1002,
            ),
        )
        advance()

        coVerify(exactly = 0) { mockCall.reconnect(any(), any()) }
    }

    // -- sfuName --

    @Test
    fun `sfuName is stored correctly in RtcSession`() = runTest {
        val session = createRtcSession(sfuName = "edge-eu-west-1")
        assert(session.sfuName == "edge-eu-west-1")
    }
}
