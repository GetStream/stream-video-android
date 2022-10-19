/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android

import android.content.Context
import androidx.lifecycle.Lifecycle
import io.getstream.logging.StreamLog
import io.getstream.video.android.audio.AudioDevice
import io.getstream.video.android.client.CallClient
import io.getstream.video.android.client.LifecycleHandler
import io.getstream.video.android.client.StreamLifecycleObserver
import io.getstream.video.android.client.user.UserState
import io.getstream.video.android.dispatchers.DispatcherProvider
import io.getstream.video.android.engine.StreamCallEngineImpl
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallMetadata
import io.getstream.video.android.model.CallSettings
import io.getstream.video.android.model.IceServer
import io.getstream.video.android.model.JoinedCall
import io.getstream.video.android.model.User
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.socket.SocketListener
import io.getstream.video.android.socket.SocketState
import io.getstream.video.android.socket.SocketStateService
import io.getstream.video.android.socket.VideoSocket
import io.getstream.video.android.token.CredentialsProvider
import io.getstream.video.android.utils.Result
import io.getstream.video.android.utils.flatMap
import io.getstream.video.android.utils.map
import io.getstream.video.android.utils.onError
import io.getstream.video.android.utils.onSuccess
import io.getstream.video.android.webrtc.WebRTCClient
import io.getstream.video.android.webrtc.builder.WebRTCClientBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import stream.video.coordinator.client_v1_rpc.UserEventType

/**
 * @param lifecycle The lifecycle used to observe changes in the process. // TODO - docs
 */
public class StreamCallsImpl(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val loggingLevel: LoggingLevel,
    private val callClient: CallClient,
    private val credentialsProvider: CredentialsProvider,
    private val socket: VideoSocket,
    private val socketStateService: SocketStateService,
    private val userState: UserState
) : StreamCalls {

    private val logger = StreamLog.getLogger("Call:StreamCalls")

    private val scope = CoroutineScope(DispatcherProvider.IO)

    private val engine = StreamCallEngineImpl(scope) {
        credentialsProvider.getUserCredentials()
    }

    /**
     * Observes the app lifecycle and attempts to reconnect/release the socket connection.
     */
    private val lifecycleObserver = StreamLifecycleObserver(
        lifecycle,
        object : LifecycleHandler {
            override fun resume() = reconnectSocket()
            override fun stopped() {
                socket.releaseConnection()
            }
        }
    )

    init {
        socket.addListener(engine)
        scope.launch(Dispatchers.Main.immediate) {
            lifecycleObserver.observe()
        }

        socket.connectSocket()
    }

    override val callState: StateFlow<StreamCallState> = engine.callState

    /**
     * Domain - Coordinator.
     */
    override suspend fun createCall(
        type: String,
        id: String,
        participantIds: List<String>,
        ringing: Boolean
    ): Result<CallMetadata> {
        logger.d { "[createCall] type: $type, id: $id, participantIds: $participantIds" }
        engine.onCallStarting(type, id, participantIds, ringing, forcedNewCall = true)
        return callClient.createCall(type, id, participantIds, ringing)
            .onSuccess { engine.onCallStarted(it.call) }
            .onError { engine.onCallFailed(it) }
            .map { it.call }
            .also { logger.v { "[createCall] result: $it" } }
    }

    // caller: DIAL and wait answer
    override suspend fun getOrCreateCall(
        type: String,
        id: String,
        participantIds: List<String>,
        ringing: Boolean
    ): Result<CallMetadata> {
        logger.d { "[getOrCreateCall] type: $type, id: $id, participantIds: $participantIds" }
        engine.onCallStarting(type, id, participantIds, ringing, forcedNewCall = false)
        return callClient.getOrCreateCall(type, id, participantIds, ringing)
            .onSuccess { engine.onCallStarted(it.call) }
            .onError { engine.onCallFailed(it) }
            .map { it.call }
            .also { logger.v { "[getOrCreateCall] result: $it" } }
    }

    // caller: JOIN after accepting incoming call by callee
    override suspend fun joinCall(call: CallMetadata): Result<JoinedCall> {
        logger.d { "[joinCallOnly] call: $call" }
        engine.onCallJoining(call)
        return callClient.joinCall(call)
            .onSuccess { data -> engine.onCallJoined(data) }
            .onError { engine.onCallFailed(it) }
            .also { logger.v { "[joinCallOnly] result: $it" } }
    }

    // caller/callee: CREATE/JOIN meeting
    override suspend fun createAndJoinCall(
        type: String,
        id: String,
        participantIds: List<String>,
        ringing: Boolean
    ): Result<JoinedCall> {
        logger.d { "[getOrCreateAndJoinCall] type: $type, id: $id, participantIds: $participantIds" }
        engine.onCallStarting(type, id, participantIds, ringing, forcedNewCall = false)
        return callClient.getOrCreateCall(type, id, participantIds, ringing)
            .onSuccess { engine.onCallJoining(it.call) }
            .flatMap { callClient.joinCall(it.call) }
            .onSuccess { engine.onCallJoined(it) }
            .onError { engine.onCallFailed(it) }
            .also { logger.v { "[getOrCreateAndJoinCall] result: $it" } }
    }

    // callee: ACCEPT incoming call
    override suspend fun joinCall(type: String, id: String): Result<JoinedCall> {
        logger.d { "[joinCall] type: $type, id: $id" }
        return createAndJoinCall(type, id, participantIds = emptyList(), ringing = false)
            .also { logger.v { "[joinCall] result: $it" } }
    }

    // callee: SEND Accepted or Rejected
    override suspend fun sendEvent(
        callType: String,
        callId: String,
        // TODO replace UserEventType with domain model
        eventType: UserEventType
    ): Result<Boolean> {
        logger.d { "[sendEvent] callType: $callType, callId: $callId, eventType: $eventType" }
        engine.onCallEventSending(callType, callId, eventType)
        return callClient.sendUserEvent(callType = callType, callId = callId, eventType = eventType)
            .onSuccess { engine.onCallEventSent(callType, callId, eventType) }
            .also { logger.v { "[sendEvent] result: $it" } }
    }

    override fun clearCallState() {
        credentialsProvider.setSfuToken(null)
        socket.updateCallState(null)
        webRTCClient?.clear()
    }

    /**
     * Attempts to reconnect the socket if it's in a disconnected state and the user is available.
     */
    private fun reconnectSocket() {
        val user = userState.user.value

        if (socketStateService.state !is SocketState.Connected && user.id.isNotBlank()) {
            socket.reconnect()
        }
    }

    override fun getUser(): User = callClient.getUser()

    override fun addSocketListener(socketListener: SocketListener): Unit =
        socket.addListener(socketListener)

    override fun removeSocketListener(socketListener: SocketListener): Unit =
        socket.removeListener(socketListener)

    /**
     * Domain - WebRTC.
     */
    private var webRTCClient: WebRTCClient? = null

    override fun createCallClient(
        signalUrl: String,
        userToken: String,
        iceServers: List<IceServer>,
        credentialsProvider: CredentialsProvider
    ) {
        credentialsProvider.setSfuToken(userToken)
        val builder = WebRTCClientBuilder(context, credentialsProvider, signalUrl, iceServers)

        builder.loggingLevel(loggingLevel)

        this.webRTCClient = builder.build()
    }

    override suspend fun connectToCall(
        sessionId: String,
        autoPublish: Boolean,
        callSettings: CallSettings
    ): Result<Call> {
        val client = requireClient()

        return client.connectToCall(sessionId, autoPublish, callSettings)
    }

    override fun startCapturingLocalVideo(position: Int) {
        val client = requireClient()

        client.startCapturingLocalVideo(position)
    }

    override fun setCameraEnabled(isEnabled: Boolean) {
        val client = requireClient()

        client.setCameraEnabled(isEnabled)
    }

    override fun setMicrophoneEnabled(isEnabled: Boolean) {
        val client = requireClient()

        client.setMicrophoneEnabled(isEnabled)
    }

    override fun flipCamera() {
        val client = requireClient()

        client.flipCamera()
    }

    override fun getAudioDevices(): List<AudioDevice> {
        val client = requireClient()

        return client.getAudioDevices()
    }

    override fun selectAudioDevice(device: AudioDevice) {
        val client = requireClient()

        client.selectAudioDevice(device)
    }

    private fun requireClient(): WebRTCClient {
        return webRTCClient
            ?: throw IllegalStateException(
                "Cannot connect to a call without a WebRTC Client. " +
                    "Make sure to initialize the client first, using `createCallClient`"
            )
    }
}
