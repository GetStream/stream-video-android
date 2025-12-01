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

package io.getstream.video.android.core.socket.coordinator

import io.getstream.android.video.generated.models.ConnectedEvent
import io.getstream.android.video.generated.models.ConnectionErrorEvent
import io.getstream.android.video.generated.models.HealthCheckEvent
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.errors.DisconnectCause
import io.getstream.video.android.core.errors.VideoErrorCode
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.lifecycle.ConnectionPolicyLifecycleHandler
import io.getstream.video.android.core.lifecycle.StreamLifecycleObserver
import io.getstream.video.android.core.socket.common.CallAwareConnectionPolicy
import io.getstream.video.android.core.socket.common.ConnectionConf
import io.getstream.video.android.core.socket.common.DISPOSE_SOCKET_REASON
import io.getstream.video.android.core.socket.common.DISPOSE_SOCKET_RECONNECT
import io.getstream.video.android.core.socket.common.HealthMonitor
import io.getstream.video.android.core.socket.common.SocketFactory
import io.getstream.video.android.core.socket.common.SocketListener
import io.getstream.video.android.core.socket.common.SocketStateConnectionPolicy
import io.getstream.video.android.core.socket.common.StreamWebSocket
import io.getstream.video.android.core.socket.common.StreamWebSocketEvent
import io.getstream.video.android.core.socket.common.VideoErrorDetail
import io.getstream.video.android.core.socket.common.VideoParser
import io.getstream.video.android.core.socket.common.fromVideoErrorCode
import io.getstream.video.android.core.socket.common.scope.UserScope
import io.getstream.video.android.core.socket.common.token.TokenManager
import io.getstream.video.android.core.socket.coordinator.state.VideoSocketConnectionType
import io.getstream.video.android.core.socket.coordinator.state.VideoSocketState
import io.getstream.video.android.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import stream.video.sfu.models.WebsocketReconnectStrategy
import kotlin.coroutines.EmptyCoroutineContext

@Suppress("TooManyFunctions", "LongParameterList")
internal open class CoordinatorSocket(
    private val apiKey: String,
    private val wssUrl: String,
    private val tokenManager: TokenManager,
    private val socketFactory: SocketFactory<VideoEvent, VideoParser, ConnectionConf>,
    private val userScope: UserScope,
    private val lifecycleObserver: StreamLifecycleObserver,
    private val networkStateProvider: NetworkStateProvider,
) {
    private var streamWebSocket: StreamWebSocket<VideoEvent, VideoParser>? = null
    open val logger by taggedLogger(TAG)
    private var connectionConf: ConnectionConf? = null
    private val listeners = mutableSetOf<SocketListener<VideoEvent, ConnectedEvent>>()
    private val coordinatorSocketStateService = CoordinatorSocketStateService()
    private var socketStateObserverJob: Job? = null
    private val healthMonitor = HealthMonitor(
        userScope = userScope,
        checkCallback = {
            val connected = coordinatorSocketStateService.currentState as? VideoSocketState.Connected
            connected?.event?.let(::sendEvent)
        },
        reconnectCallback = {
            coordinatorSocketStateService.onWebSocketEventLost()
        },
    )
    private val connectionPolicies = listOf(
        CallAwareConnectionPolicy(StreamVideo.instanceState),
        SocketStateConnectionPolicy(state()),
    )
    private val lifecycleHandler =
        ConnectionPolicyLifecycleHandler(connectionPolicies, coordinatorSocketStateService)
    private val networkStateListener = object : NetworkStateProvider.NetworkStateListener {
        override suspend fun onConnected() {
            coordinatorSocketStateService.onNetworkAvailable()
        }

        override suspend fun onDisconnected() {
            coordinatorSocketStateService.onNetworkNotAvailable()
        }
    }

    @Suppress("ComplexMethod")
    private fun observeSocketStateService(): Job {
        var socketListenerJob: Job? = null

        suspend fun connectUser(connectionConf: ConnectionConf) {
            logger.d { "[connectUser] connectionConf: $connectionConf" }
            userScope.launch { startObservers() }
            this.connectionConf = connectionConf
            socketListenerJob?.cancel()
            when (networkStateProvider.isConnected()) {
                true -> {
                    streamWebSocket = socketFactory.createSocket<VideoEvent>(connectionConf, "#coordinator").apply {
                        listeners.forEach { it.onCreated() }

                        socketListenerJob = listen().onEach {
                            when (it) {
                                is StreamWebSocketEvent.Error -> handleError(it)
                                is StreamWebSocketEvent.VideoMessage -> when (
                                    val event =
                                        it.videoEvent
                                ) {
                                    is ConnectionErrorEvent -> handleError(StreamWebSocketEvent.Error(event.toNetworkError()))
                                    else -> handleEvent(event)
                                }
                                is StreamWebSocketEvent.SfuMessage -> {
                                    logger.v { "Received [SFUMessage] in coordinator socket. Ignoring" }
                                }
                            }
                        }.launchIn(userScope)
                    }
                }

                false -> coordinatorSocketStateService.onNetworkNotAvailable()
            }
        }

        suspend fun reconnect(connectionConf: ConnectionConf) {
            logger.d { "[reconnect] connectionConf: $connectionConf" }
            connectUser(connectionConf.asReconnectionConf())
        }

        return userScope.launch {
            coordinatorSocketStateService.observer { state ->
                logger.i { "[onSocketStateChanged] state: $state" }
                when (state) {
                    is VideoSocketState.RestartConnection -> {
                        connectionConf?.let { coordinatorSocketStateService.onReconnect(it, false) }
                            ?: run {
                                logger.e { "[onSocketStateChanged] #reconnect; connectionConf is null" }
                            }
                    }

                    is VideoSocketState.Connected -> {
                        healthMonitor.ack()
                        callListeners { listener -> listener.onConnected(state.event) }
                    }

                    is VideoSocketState.Connecting -> {
                        callListeners { listener -> listener.onConnecting() }
                        when (state.connectionType) {
                            VideoSocketConnectionType.INITIAL_CONNECTION ->
                                connectUser(state.connectionConf)

                            VideoSocketConnectionType.AUTOMATIC_RECONNECTION ->
                                reconnect(state.connectionConf.asReconnectionConf())

                            VideoSocketConnectionType.FORCE_RECONNECTION ->
                                reconnect(state.connectionConf.asReconnectionConf())
                        }
                    }

                    is VideoSocketState.Disconnected -> {
                        when (state) {
                            is VideoSocketState.Disconnected.DisconnectedByRequest -> {
                                streamWebSocket?.close(
                                    "VideoSocketState.Disconnected.DisconnectedByRequest",
                                )
                                healthMonitor.stop()
                                userScope.launch { disposeObservers() }
                            }

                            is VideoSocketState.Disconnected.NetworkDisconnected -> {
                                streamWebSocket?.close(
                                    "VideoSocketState.Disconnected.NetworkDisconnected",
                                    DISPOSE_SOCKET_RECONNECT,
                                    DISPOSE_SOCKET_REASON,
                                ) // To prevent the emission of ParticipantLeftEvent to other users
                                healthMonitor.stop()
                            }

                            is VideoSocketState.Disconnected.Stopped -> {
                                streamWebSocket?.close("VideoSocketState.Disconnected.Stopped")
                                healthMonitor.stop()
                                disposeNetworkStateObserver()
                            }

                            is VideoSocketState.Disconnected.DisconnectedPermanently -> {
                                streamWebSocket?.close(
                                    "VideoSocketState.Disconnected.DisconnectedPermanently",
                                )
                                healthMonitor.stop()
                                userScope.launch { disposeObservers() }
                            }

                            is VideoSocketState.Disconnected.DisconnectedTemporarily -> {
                                healthMonitor.onDisconnected()
                            }

                            is VideoSocketState.Disconnected.WebSocketEventLost -> {
                                streamWebSocket?.close(
                                    "VideoSocketState.Disconnected.WebSocketEventLost",
                                )
                                connectionConf?.let {
                                    coordinatorSocketStateService.onReconnect(
                                        it,
                                        false,
                                    )
                                }
                            }
                        }
                        callListeners { listener -> listener.onDisconnected(cause = state.cause) }
                    }
                }
            }
        }
    }

    suspend fun connectUser(user: User, isAnonymous: Boolean) {
        logger.d { "[connectUser] user.id: ${user.id}, isAnonymous: $isAnonymous" }
        socketStateObserverJob?.cancel()
        socketStateObserverJob = observeSocketStateService()
        coordinatorSocketStateService.onConnect(
            when (isAnonymous) {
                true -> ConnectionConf.AnonymousConnectionConf(wssUrl, apiKey, user)
                false -> ConnectionConf.UserConnectionConf(wssUrl, apiKey, user)
            },
        )
    }

    suspend fun disconnect() {
        logger.d { "[disconnect] no args" }
        connectionConf = null
        coordinatorSocketStateService.onRequiredDisconnect()
    }

    private suspend fun handleEvent(chatEvent: VideoEvent) {
        when (chatEvent) {
            is ConnectedEvent -> coordinatorSocketStateService.onConnectionEstablished(chatEvent)
            is HealthCheckEvent -> healthMonitor.ack()
            else -> callListeners { listener -> listener.onEvent(chatEvent) }
        }
    }

    private suspend fun startObservers() {
        lifecycleObserver.observe(lifecycleHandler)
        networkStateProvider.subscribe(networkStateListener)
    }

    private suspend fun disposeObservers() {
        lifecycleObserver.dispose(lifecycleHandler)
        disposeNetworkStateObserver()
    }

    private fun disposeNetworkStateObserver() {
        networkStateProvider.unsubscribe(networkStateListener)
    }

    private suspend fun handleError(error: StreamWebSocketEvent.Error) {
        logger.e { "[handleError] error: $error" }
        when (error.streamError) {
            is Error.NetworkError -> onVideoNetworkError(error.streamError, error.reconnectStrategy)
            else -> callListeners { it.onError(error) }
        }
    }

    private suspend fun onVideoNetworkError(error: Error.NetworkError, reconnectStrategy: WebsocketReconnectStrategy?) {
        if (VideoErrorCode.isAuthenticationError(error.serverErrorCode)) {
            tokenManager.expireToken()
        }
        // Noob
        when (error.serverErrorCode) {
            VideoErrorCode.TOKEN_EXPIRED.code,
            -> {
                tokenManager.expireToken()
                logger.d { "load sync START" }
                val token = tokenManager.loadSync()
                tokenManager.updateToken(token)
                if (token.isNotEmpty()) {
                    logger.d { "load sync END: $token" }
                    return
                }
            }
            else -> {}
        }

        when (error.serverErrorCode) {
            VideoErrorCode.UNDEFINED_TOKEN.code,
            VideoErrorCode.INVALID_TOKEN.code,
            VideoErrorCode.API_KEY_NOT_FOUND.code,
            -> {
                logger.d {
                    "One unrecoverable error happened. Error: $error. Error code: ${error.serverErrorCode}"
                }
                coordinatorSocketStateService.onUnrecoverableError(error)
            }

            else -> {
                coordinatorSocketStateService.onNetworkError(error)
            }
        }
    }

    fun removeListener(listener: SocketListener<VideoEvent, ConnectedEvent>) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    fun addListener(listener: SocketListener<VideoEvent, ConnectedEvent>) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    fun state() = coordinatorSocketStateService.currentStateFlow

    /**
     * Attempt to send [event] to the web socket connection.
     * Returns true only if socket is connected and [okhttp3.WebSocket.send] returns true, otherwise false
     *
     * @see [okhttp3.WebSocket.send]
     */
    internal fun sendEvent(event: VideoEvent): Boolean = streamWebSocket?.send(event) ?: false

    /**
     * Send raw data to the web socket connection.
     */
    internal fun senRawData(data: String) {
        streamWebSocket?.sendRaw(data)
    }

    internal fun isConnected(): Boolean = coordinatorSocketStateService.currentState is VideoSocketState.Connected

    /**
     * Awaits until [VideoSocketState.Connected] is set.
     *
     * @param timeoutInMillis Timeout time in milliseconds.
     */
    internal suspend fun awaitConnection(timeoutInMillis: Long = DEFAULT_CONNECTION_TIMEOUT) {
        awaitState<VideoSocketState.Connected>(timeoutInMillis)
    }

    /**
     * Awaits until specified [VideoSocketState] is set.
     *
     * @param timeoutInMillis Timeout time in milliseconds.
     */
    internal suspend inline fun <reified T : VideoSocketState> awaitState(timeoutInMillis: Long) {
        withTimeout(timeoutInMillis) {
            coordinatorSocketStateService.currentStateFlow.first { it is T }
        }
    }

    /**
     * Get connection id of this connection.
     */
    internal fun connectionIdOrError(): String =
        when (val state = coordinatorSocketStateService.currentState) {
            is VideoSocketState.Connected -> state.event.connectionId
            else -> error("This state doesn't contain connectionId")
        }

    suspend fun reconnectUser(user: User, isAnonymous: Boolean, forceReconnection: Boolean) {
        logger.d {
            "[reconnectUser] user.id: ${user.id}, isAnonymous: $isAnonymous, forceReconnection: $forceReconnection"
        }
        coordinatorSocketStateService.onReconnect(
            when (isAnonymous) {
                true -> ConnectionConf.AnonymousConnectionConf(wssUrl, apiKey, user)
                false -> ConnectionConf.UserConnectionConf(wssUrl, apiKey, user)
            },
            forceReconnection,
        )
    }

    private fun callListeners(call: (SocketListener<VideoEvent, ConnectedEvent>) -> Unit) {
        synchronized(listeners) {
            listeners.forEach { listener ->
                val context = if (listener.deliverOnMainThread) {
                    DispatcherProvider.Main
                } else {
                    EmptyCoroutineContext
                }
                userScope.launch(context) { call(listener) }
            }
        }
    }

    private val VideoSocketState.Disconnected.cause
        get() = when (this) {
            is VideoSocketState.Disconnected.DisconnectedByRequest,
            is VideoSocketState.Disconnected.Stopped,
            -> DisconnectCause.ConnectionReleased

            is VideoSocketState.Disconnected.NetworkDisconnected -> DisconnectCause.NetworkNotAvailable
            is VideoSocketState.Disconnected.DisconnectedPermanently -> DisconnectCause.UnrecoverableError(
                error,
            )

            is VideoSocketState.Disconnected.DisconnectedTemporarily -> DisconnectCause.Error(error)
            is VideoSocketState.Disconnected.WebSocketEventLost -> DisconnectCause.WebSocketNotAvailable
        }

    private fun ConnectionErrorEvent.toNetworkError(): Error.NetworkError {
        return error?.let {
            return Error.NetworkError(
                message = it.message + moreInfoTemplate(it.moreInfo) + buildDetailsTemplate(
                    it.details.map { code ->
                        VideoErrorDetail(code, listOf(""))
                    },
                ),
                serverErrorCode = it.code,
                statusCode = it.statusCode,
            )
        } ?: Error.NetworkError.fromVideoErrorCode(VideoErrorCode.NO_ERROR_BODY, cause = null)
    }

    private fun moreInfoTemplate(moreInfo: String): String {
        return if (moreInfo.isNotBlank()) {
            "\nMore information available at $moreInfo"
        } else {
            ""
        }
    }

    private fun buildDetailsTemplate(details: List<VideoErrorDetail>): String {
        return if (details.isNotEmpty()) {
            "\nError details: $details"
        } else {
            ""
        }
    }

    companion object {
        private const val TAG = "Video:Socket"
        private const val DEFAULT_CONNECTION_TIMEOUT = 60_000L
    }
}
