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
import io.getstream.video.android.audio.AudioDevice
import io.getstream.video.android.client.CallClient
import io.getstream.video.android.client.LifecycleHandler
import io.getstream.video.android.client.StreamLifecycleObserver
import io.getstream.video.android.client.user.UserState
import io.getstream.video.android.dispatchers.DispatcherProvider
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallMetadata
import io.getstream.video.android.model.CallSettings
import io.getstream.video.android.model.JoinedCall
import io.getstream.video.android.model.User
import io.getstream.video.android.socket.SocketListener
import io.getstream.video.android.socket.SocketState
import io.getstream.video.android.socket.SocketStateService
import io.getstream.video.android.socket.VideoSocket
import io.getstream.video.android.token.CredentialsProvider
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Result
import io.getstream.video.android.utils.Success
import io.getstream.video.android.utils.toCall
import io.getstream.video.android.webrtc.WebRTCClient
import io.getstream.video.android.webrtc.builder.WebRTCClientBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    private val scope = CoroutineScope(DispatcherProvider.IO)

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
        scope.launch(Dispatchers.Main.immediate) {
            lifecycleObserver.observe()
        }

        socket.connectSocket()
    }

    /**
     * Domain - Coordinator.
     */
    override suspend fun createCall(
        type: String,
        id: String,
        participantIds: List<String>
    ): Result<CallMetadata> {
        return when (val result = callClient.createCall(type, id, participantIds)) {
            is Success -> Success(result.data.call?.call?.toCall()!!)
            is Failure -> Failure(result.error)
        }
    }

    override suspend fun createAndJoinCall(
        type: String,
        id: String,
        participantIds: List<String>
    ): Result<JoinedCall> {
        return callClient.createAndJoinCall(type, id, participantIds)
    }

    override suspend fun joinCall(call: CallMetadata): Result<JoinedCall> {
        return callClient.joinCall(call)
    }

    override fun leaveCall() {
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
        credentialsProvider: CredentialsProvider
    ) {
        credentialsProvider.setSfuToken(userToken)
        val builder = WebRTCClientBuilder(context, credentialsProvider, signalUrl)

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
