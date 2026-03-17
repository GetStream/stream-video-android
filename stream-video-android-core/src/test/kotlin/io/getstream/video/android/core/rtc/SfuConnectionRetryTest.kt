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
import io.getstream.video.android.core.internal.module.SfuConnectionModule
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

    private var disconnectCounter = 0

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        mockStreamVideo = mockk(relaxed = true)
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

    private fun disconnectedTemporarily() = SfuSocketState.Disconnected.DisconnectedTemporarily(
        error = Error.NetworkError(
            message = "Connection failed #${++disconnectCounter}",
            serverErrorCode = 1003,
            statusCode = 1003,
        ),
        reconnectStrategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_UNSPECIFIED,
    )

    private fun advance() = testScope.advanceUntilIdle()

    @Test
    fun `first DisconnectedTemporarily does not trigger migrate`() = runTest {
        createRtcSession()
        advance()

        socketStateFlow.value = disconnectedTemporarily()
        advance()

        coVerify(exactly = 0) { mockCall.migrate() }
    }

    @Test
    fun `second DisconnectedTemporarily does not trigger migrate`() = runTest {
        createRtcSession()
        advance()

        socketStateFlow.value = disconnectedTemporarily()
        advance()
        socketStateFlow.value = SfuSocketState.Disconnected.WebSocketEventLost
        advance()
        socketStateFlow.value = disconnectedTemporarily()
        advance()

        coVerify(exactly = 0) { mockCall.migrate() }
    }

    @Test
    fun `third DisconnectedTemporarily triggers migrate`() = runTest {
        createRtcSession()
        advance()

        socketStateFlow.value = disconnectedTemporarily()
        advance()
        socketStateFlow.value = SfuSocketState.Disconnected.WebSocketEventLost
        advance()
        socketStateFlow.value = disconnectedTemporarily()
        advance()
        socketStateFlow.value = SfuSocketState.Disconnected.WebSocketEventLost
        advance()
        socketStateFlow.value = disconnectedTemporarily()
        advance()

        coVerify(exactly = 1) { mockCall.migrate() }
        coVerify(exactly = 1) { mockSocket.disconnect() }
    }

    @Test
    fun `WebSocketEventLost alone does not increment retry counter or trigger migrate`() =
        runTest {
            createRtcSession()
            advance()

            repeat(5) {
                socketStateFlow.value = SfuSocketState.Disconnected.WebSocketEventLost
                advance()
            }

            coVerify(exactly = 0) { mockCall.migrate() }
        }

    @Test
    fun `Connected state resets retry counter`() = runTest {
        val connectedEvent = mockk<JoinCallResponseEvent>(relaxed = true)
        createRtcSession()
        advance()

        socketStateFlow.value = disconnectedTemporarily()
        advance()
        socketStateFlow.value = disconnectedTemporarily()
        advance()

        socketStateFlow.value = SfuSocketState.Connected(connectedEvent)
        advance()

        socketStateFlow.value = disconnectedTemporarily()
        advance()
        socketStateFlow.value = disconnectedTemporarily()
        advance()

        coVerify(exactly = 0) { mockCall.migrate() }
    }

    @Test
    fun `Connected state calls onSfuConnectionEstablished`() = runTest {
        val connectedEvent = mockk<JoinCallResponseEvent>(relaxed = true)
        createRtcSession()
        advance()

        socketStateFlow.value = SfuSocketState.Connected(connectedEvent)
        advance()

        coVerify(exactly = 1) { mockCall.onSfuConnectionEstablished() }
    }

    @Test
    fun `retry counter resets after triggering migrate`() = runTest {
        createRtcSession()
        advance()

        repeat(3) {
            socketStateFlow.value = disconnectedTemporarily()
            advance()
        }
        coVerify(exactly = 1) { mockCall.migrate() }

        socketStateFlow.value = disconnectedTemporarily()
        advance()
        socketStateFlow.value = disconnectedTemporarily()
        advance()

        coVerify(exactly = 1) { mockCall.migrate() }
    }

    @Test
    fun `sfuName is stored correctly in RtcSession`() = runTest {
        val session = createRtcSession(sfuName = "edge-eu-west-1")
        assert(session.sfuName == "edge-eu-west-1")
    }
}
