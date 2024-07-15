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

package io.getstream.video.android.core.socket.common

import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.errors.DisconnectCause
import io.getstream.video.android.core.errors.VideoErrorCode
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.lifecycle.LifecycleHandler
import io.getstream.video.android.core.lifecycle.StreamLifecycleObserver
import io.getstream.video.android.core.socket.common.VideoSocketStateService.State
import io.getstream.video.android.core.socket.common.scope.UserScope
import io.getstream.video.android.core.socket.common.token.TokenManager
import io.getstream.video.android.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.openapitools.client.models.ConnectedEvent
import org.openapitools.client.models.ConnectionErrorEvent
import org.openapitools.client.models.HealthCheckEvent
import org.openapitools.client.models.VideoEvent
import kotlin.coroutines.EmptyCoroutineContext

@Suppress("TooManyFunctions", "LongParameterList")
internal open class VideoSocket(
    private val apiKey: String,
    private val wssUrl: String,
    private val tokenManager: TokenManager,
    private val socketFactory: SocketFactory,
    private val userScope: UserScope,
    private val lifecycleObserver: StreamLifecycleObserver,
    private val networkStateProvider: NetworkStateProvider,
) {
    private var streamWebSocket: StreamWebSocket? = null
    open val logger by taggedLogger(TAG)
    private var connectionConf: SocketFactory.ConnectionConf? = null
    private val listeners = mutableSetOf<SocketListener<VideoEvent>>()
    private val videoSocketStateService = VideoSocketStateService()
    private var socketStateObserverJob: Job? = null
    private val healthMonitor = HealthMonitor(
        userScope = userScope,
        checkCallback = {
            (videoSocketStateService.currentState as? State.Connected)?.event?.let(::sendEvent)
        },
        reconnectCallback = { videoSocketStateService.onWebSocketEventLost() },
    )
    private val lifecycleHandler = object : LifecycleHandler {
        override suspend fun resume() {
            videoSocketStateService.onResume()
        }

        override suspend fun stopped() {
            videoSocketStateService.onStop()
        }
    }
    private val networkStateListener = object : NetworkStateProvider.NetworkStateListener {
        override suspend fun onConnected() {
            videoSocketStateService.onNetworkAvailable()
        }

        override suspend fun onDisconnected() {
            videoSocketStateService.onNetworkNotAvailable()
        }
    }

    @Suppress("ComplexMethod")
    private fun observeSocketStateService(): Job {
        var socketListenerJob: Job? = null

        suspend fun connectUser(connectionConf: SocketFactory.ConnectionConf) {
            logger.d { "[connectUser] connectionConf: $connectionConf" }
            userScope.launch { startObservers() }
            this.connectionConf = connectionConf
            socketListenerJob?.cancel()
            when (networkStateProvider.isConnected()) {
                true -> {
                    streamWebSocket = socketFactory.createSocket(connectionConf).apply {
                        listeners.forEach { it.onCreated() }

                        socketListenerJob = listen().onEach {
                            when (it) {
                                is StreamWebSocketEvent.Error -> handleError(it.streamError)
                                is StreamWebSocketEvent.Message -> when (
                                    val event =
                                        it.videoEvent
                                ) {
                                    is ConnectionErrorEvent -> handleError(event.toNetworkError())
                                    else -> handleEvent(event)
                                }
                            }
                        }.launchIn(userScope)
                    }
                }

                false -> videoSocketStateService.onNetworkNotAvailable()
            }
        }

        suspend fun reconnect(connectionConf: SocketFactory.ConnectionConf) {
            logger.d { "[reconnect] connectionConf: $connectionConf" }
            connectUser(connectionConf.asReconnectionConf())
        }

        return userScope.launch {
            videoSocketStateService.observer { state ->
                logger.i { "[onSocketStateChanged] state: $state" }
                when (state) {
                    is State.RestartConnection -> {
                        connectionConf?.let { videoSocketStateService.onReconnect(it, false) }
                            ?: run {
                                logger.e { "[onSocketStateChanged] #reconnect; connectionConf is null" }
                            }
                    }

                    is State.Connected -> {
                        healthMonitor.ack()
                        callListeners { listener -> listener.onConnected(state.event) }
                    }

                    is State.Connecting -> {
                        callListeners { listener -> listener.onConnecting() }
                        when (state.connectionType) {
                            VideoSocketStateService.ConnectionType.INITIAL_CONNECTION ->
                                connectUser(state.connectionConf)

                            VideoSocketStateService.ConnectionType.AUTOMATIC_RECONNECTION ->
                                reconnect(state.connectionConf.asReconnectionConf())

                            VideoSocketStateService.ConnectionType.FORCE_RECONNECTION ->
                                reconnect(state.connectionConf.asReconnectionConf())
                        }
                    }

                    is State.Disconnected -> {
                        when (state) {
                            is State.Disconnected.DisconnectedByRequest -> {
                                streamWebSocket?.close()
                                healthMonitor.stop()
                                userScope.launch { disposeObservers() }
                            }

                            is State.Disconnected.NetworkDisconnected -> {
                                streamWebSocket?.close()
                                healthMonitor.stop()
                            }

                            is State.Disconnected.Stopped -> {
                                streamWebSocket?.close()
                                healthMonitor.stop()
                                disposeNetworkStateObserver()
                            }

                            is State.Disconnected.DisconnectedPermanently -> {
                                streamWebSocket?.close()
                                healthMonitor.stop()
                                userScope.launch { disposeObservers() }
                            }

                            is State.Disconnected.DisconnectedTemporarily -> {
                                healthMonitor.onDisconnected()
                            }

                            is State.Disconnected.WebSocketEventLost -> {
                                streamWebSocket?.close()
                                connectionConf?.let {
                                    videoSocketStateService.onReconnect(
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
        videoSocketStateService.onConnect(
            when (isAnonymous) {
                true -> SocketFactory.ConnectionConf.AnonymousConnectionConf(wssUrl, apiKey, user)
                false -> SocketFactory.ConnectionConf.UserConnectionConf(wssUrl, apiKey, user)
            },
        )
    }

    suspend fun disconnect() {
        logger.d { "[disconnect] no args" }
        connectionConf = null
        videoSocketStateService.onRequiredDisconnect()
    }

    private suspend fun handleEvent(chatEvent: VideoEvent) {
        when (chatEvent) {
            is ConnectedEvent -> videoSocketStateService.onConnectionEstablished(chatEvent)
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

    private suspend fun handleError(error: Error) {
        logger.e { "[handleError] error: $error" }
        when (error) {
            is Error.NetworkError -> onVideoNetworkError(error)
            else -> callListeners { it.onError(error) }
        }
    }

    private suspend fun onVideoNetworkError(error: Error.NetworkError) {
        if (VideoErrorCode.isAuthenticationError(error.serverErrorCode)) {
            tokenManager.expireToken()
        }

        when (error.serverErrorCode) {
            VideoErrorCode.UNDEFINED_TOKEN.code,
            VideoErrorCode.INVALID_TOKEN.code,
            VideoErrorCode.API_KEY_NOT_FOUND.code,
            VideoErrorCode.VALIDATION_ERROR.code,
            -> {
                logger.d {
                    "One unrecoverable error happened. Error: $error. Error code: ${error.serverErrorCode}"
                }
                videoSocketStateService.onUnrecoverableError(error)
            }

            else -> videoSocketStateService.onNetworkError(error)
        }
    }

    fun removeListener(listener: SocketListener<VideoEvent>) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    fun addListener(listener: SocketListener<VideoEvent>) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    /**
     * Attempt to send [event] to the web socket connection.
     * Returns true only if socket is connected and [okhttp3.WebSocket.send] returns true, otherwise false
     *
     * @see [okhttp3.WebSocket.send]
     */
    internal fun sendEvent(event: VideoEvent): Boolean = streamWebSocket?.send(event) ?: false

    internal fun isConnected(): Boolean = videoSocketStateService.currentState is State.Connected

    /**
     * Awaits until [State.Connected] is set.
     *
     * @param timeoutInMillis Timeout time in milliseconds.
     */
    internal suspend fun awaitConnection(timeoutInMillis: Long = DEFAULT_CONNECTION_TIMEOUT) {
        awaitState<State.Connected>(timeoutInMillis)
    }

    /**
     * Awaits until specified [State] is set.
     *
     * @param timeoutInMillis Timeout time in milliseconds.
     */
    internal suspend inline fun <reified T : State> awaitState(timeoutInMillis: Long) {
        withTimeout(timeoutInMillis) {
            videoSocketStateService.currentStateFlow.first { it is T }
        }
    }

    /**
     * Get connection id of this connection.
     */
    internal fun connectionIdOrError(): String =
        when (val state = videoSocketStateService.currentState) {
            is State.Connected -> state.event.connectionId
            else -> error("This state doesn't contain connectionId")
        }

    suspend fun reconnectUser(user: User, isAnonymous: Boolean, forceReconnection: Boolean) {
        logger.d {
            "[reconnectUser] user.id: ${user.id}, isAnonymous: $isAnonymous, forceReconnection: $forceReconnection"
        }
        videoSocketStateService.onReconnect(
            when (isAnonymous) {
                true -> SocketFactory.ConnectionConf.AnonymousConnectionConf(wssUrl, apiKey, user)
                false -> SocketFactory.ConnectionConf.UserConnectionConf(wssUrl, apiKey, user)
            },
            forceReconnection,
        )
    }

    private fun callListeners(call: (SocketListener<VideoEvent>) -> Unit) {
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

    private val State.Disconnected.cause
        get() = when (this) {
            is State.Disconnected.DisconnectedByRequest,
            is State.Disconnected.Stopped,
            -> DisconnectCause.ConnectionReleased

            is State.Disconnected.NetworkDisconnected -> DisconnectCause.NetworkNotAvailable
            is State.Disconnected.DisconnectedPermanently -> DisconnectCause.UnrecoverableError(
                error,
            )

            is State.Disconnected.DisconnectedTemporarily -> DisconnectCause.Error(error)
            is State.Disconnected.WebSocketEventLost -> DisconnectCause.WebSocketNotAvailable
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

    /**
     * The error detail.
     *
     * @property code The error code.
     * @property messages The error messages.
     */
    public data class VideoErrorDetail(
        public val code: Int,
        public val messages: List<String>,
    )

    companion object {
        private const val TAG = "Video:Socket"
        private const val DEFAULT_CONNECTION_TIMEOUT = 60_000L
    }
}
