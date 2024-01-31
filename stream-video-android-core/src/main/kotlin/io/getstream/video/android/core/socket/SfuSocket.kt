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

package io.getstream.video.android.core.socket

import android.os.Build
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.BuildConfig
import io.getstream.video.android.core.call.signal.socket.RTCEventMapper
import io.getstream.video.android.core.call.utils.stringify
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.events.ErrorEvent
import io.getstream.video.android.core.events.ICETrickleEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.SfuSocketError
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.model.IceCandidate
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import okio.ByteString
import stream.video.sfu.event.JoinRequest
import stream.video.sfu.event.Migration
import stream.video.sfu.event.SfuEvent
import stream.video.sfu.event.SfuRequest
import stream.video.sfu.models.ClientDetails
import stream.video.sfu.models.Device
import stream.video.sfu.models.ErrorCode
import stream.video.sfu.models.OS
import stream.video.sfu.models.PeerType
import stream.video.sfu.models.Sdk
import stream.video.sfu.models.SdkType

/**
 * The SFU socket is slightly different from the coordinator socket
 * It sends a JoinRequest to authenticate
 * SFU socket uses binary instead of text
 */
public class SfuSocket(
    private val url: String,
    private val sessionId: String,
    private val token: String,
    private val getSubscriberSdp: suspend () -> String,
    private val scope: CoroutineScope = CoroutineScope(DispatcherProvider.IO),
    private val httpClient: OkHttpClient,
    private val networkStateProvider: NetworkStateProvider,
    private val onFastReconnected: suspend () -> Unit,
) : PersistentSocket<JoinCallResponseEvent> (
    url = url,
    httpClient = httpClient,
    scope = scope,
    networkStateProvider = networkStateProvider,
    onFastReconnected = onFastReconnected,
) {

    override val logger by taggedLogger("PersistentSFUSocket")

    // How many milliseconds can pass since the last disconnect when a fast-reconnect
    // will still be attempted. After this time we do a full-reconnect directly.
    private val maxReconnectWindowTime = if (BuildConfig.DEBUG) { 10000L } else { 3000L }
    private var lastDisconnectTime: Long? = null

    // Only set during SFU migration
    private var migrationData: (suspend () -> Migration)? = null

    private val _pendingPublisherIceCandidates = Channel<IceCandidate>(capacity = 99)
    internal val pendingPublisherIceCandidates = _pendingPublisherIceCandidates.receiveAsFlow()

    private val _pendingSubscriberIceCandidates = Channel<IceCandidate>(capacity = 99)
    internal val pendingSubscriberIceCandidates = _pendingSubscriberIceCandidates.receiveAsFlow()

    private val clientDetails
        get() = ClientDetails(
            os = OS(
                name = "Android",
                version = Build.VERSION.SDK_INT.toString(),
            ),
            device = Device(
                name = "${Build.MANUFACTURER} : ${Build.MODEL}",
            ),
            sdk = Sdk(
                type = SdkType.SDK_TYPE_ANDROID,
                major = BuildConfig.STREAM_VIDEO_VERSION_MAJOR.toString(),
                minor = BuildConfig.STREAM_VIDEO_VERSION_MINOR.toString(),
                patch = BuildConfig.STREAM_VIDEO_VERSION_PATCH.toString(),
            ),
        )

    init {
        scope.launch {
            connectionState.collect {
                if (it is SocketState.Connected) {
                    lastDisconnectTime = null
                } else if (it is SocketState.DisconnectedTemporarily ||
                    it is SocketState.DisconnectedByRequest ||
                    it is SocketState.DisconnectedPermanently
                ) {
                    lastDisconnectTime = System.currentTimeMillis()
                }
            }
        }
    }

    suspend fun connectMigrating(migration: (suspend () -> Migration), invocation: (CancellableContinuation<JoinCallResponseEvent>) -> Unit): JoinCallResponseEvent? {
        migrationData = migration
        return connect(invocation)
    }

    override suspend fun connect(invocation: (CancellableContinuation<JoinCallResponseEvent>) -> Unit): JoinCallResponseEvent? {
        val lastDisconnectTime = lastDisconnectTime
        // Check if too much time hasn't passed since last connect - if yes then a fast reconnect
        // is not possible and we need to do a full reconnect.
        if (lastDisconnectTime != null && (System.currentTimeMillis() - lastDisconnectTime > maxReconnectWindowTime)) {
            logger.d { "[connect] SFU socket - need full-reconnect, too much time passed since disconnect" }
            handleFastReconnectNotPossible()
            return null
        } else {
            return super.connect(invocation)
        }
    }

    override fun authenticate() {
        logger.d { "[authenticate] sessionId: $sessionId" }
        scope.launch {
            // check if we haven't disposed the socket
            if (socket != null) {
                val sdp = getSubscriberSdp()

                val migration = migrationData?.invoke()
                // clear migration data - we only try to migrate once
                migrationData = null

                val request = if (migration == null) {
                    JoinRequest(
                        session_id = sessionId,
                        token = token,
                        subscriber_sdp = sdp,
                        fast_reconnect = reconnectionAttempts > 0,
                        client_details = clientDetails,
                    )
                } else {
                    JoinRequest(
                        session_id = sessionId,
                        token = token,
                        subscriber_sdp = sdp,
                        fast_reconnect = false,
                        migration = migration,
                        client_details = clientDetails,
                    )
                }

                socket?.send(SfuRequest(join_request = request).encodeByteString())
            }
        }
    }

    /** Invoked when a binary (type `0x2`) message has been received. */
    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        if (destroyed) {
            logger.d { "Received a message after being destroyed" }
            return
        }

        val byteBuffer = bytes.asByteBuffer()
        val byteArray = ByteArray(byteBuffer.capacity())
        byteBuffer.get(byteArray)
        scope.launch(singleThreadDispatcher) {
            try {
                val rawEvent = SfuEvent.ADAPTER.decode(byteArray)
                val message = RTCEventMapper.mapEvent(rawEvent)
                if (message is ErrorEvent) {
                    val errorEvent = message as ErrorEvent
                    handleError(SfuSocketError(errorEvent.error))
                }
                ackHealthMonitor()
                events.emit(message)
                if (message is JoinCallResponseEvent) {
                    if (message.isReconnected) {
                        logger.d { "[onMessage] Fast-reconnect possible - requesting ICE restarts" }
                        onFastReconnected()
                        setConnectedStateAndContinue(message)
                    } else if (reconnectionAttempts > 0) {
                        logger.d { "[onMessage] Fast-reconnect request but not possible - doing full-reconnect" }
                        // We are reconnecting and we got reconnected=false, this means that
                        // a fast reconnect is not possible and we need to do a full reconnect.
                        handleFastReconnectNotPossible()
                    } else {
                        // standard connection (it's not a reconnect response)
                        logger.d { "[onMessage] SFU socket connected" }
                        setConnectedStateAndContinue(message)
                    }
                } else if (message is ICETrickleEvent) {
                    handleIceTrickle(message)
                }
            } catch (error: Throwable) {
                coroutineContext.ensureActive()
                logger.e { "[onMessage] failed: $error" }
                handleError(error)
            }
        }
    }

    override fun isPermanentError(error: Throwable): Boolean {
        if (error is SfuSocketError &&
            error.error?.code == ErrorCode.ERROR_CODE_PARTICIPANT_MEDIA_TRANSPORT_FAILURE
        ) {
            logger.w { "Received negotiation failure - disconnecting: $error" }
            return false
        }
        return super.isPermanentError(error)
    }

    private suspend fun handleIceTrickle(event: ICETrickleEvent) {
        logger.d {
            "[handleIceTrickle] #sfu; #${event.peerType.stringify()}; candidate: ${event.candidate}"
        }
        val iceCandidate: IceCandidate = Json.decodeFromString(event.candidate)
        val result = if (event.peerType == PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED) {
            _pendingPublisherIceCandidates.send(iceCandidate)
        } else {
            _pendingSubscriberIceCandidates.send(iceCandidate)
        }
        logger.v { "[handleTrickle] #sfu; #${event.peerType.stringify()}; result: $result" }
    }

    private fun handleFastReconnectNotPossible() {
        val fastReconnectError = Exception("SFU fast-reconnect failed, full reconnect required")
        disconnect(DisconnectReason.PermanentError(fastReconnectError))
    }
}
