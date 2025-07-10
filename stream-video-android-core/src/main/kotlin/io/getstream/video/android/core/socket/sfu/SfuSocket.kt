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

package io.getstream.video.android.core.socket.sfu

import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.errors.DisconnectCause
import io.getstream.video.android.core.errors.VideoErrorCode
import io.getstream.video.android.core.events.ErrorEvent
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.SFUHealthCheckEvent
import io.getstream.video.android.core.events.SfuDataEvent
import io.getstream.video.android.core.events.SfuDataRequest
import io.getstream.video.android.core.events.UnknownEvent
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.lifecycle.NoOpLifecycleHandler
import io.getstream.video.android.core.lifecycle.StreamLifecycleObserver
import io.getstream.video.android.core.socket.common.ConnectionConf
import io.getstream.video.android.core.socket.common.HealthMonitor
import io.getstream.video.android.core.socket.common.SfuParser
import io.getstream.video.android.core.socket.common.SocketFactory
import io.getstream.video.android.core.socket.common.SocketListener
import io.getstream.video.android.core.socket.common.StreamWebSocket
import io.getstream.video.android.core.socket.common.StreamWebSocketEvent
import io.getstream.video.android.core.socket.common.fromVideoErrorCode
import io.getstream.video.android.core.socket.common.scope.UserScope
import io.getstream.video.android.core.socket.common.token.TokenManager
import io.getstream.video.android.core.socket.coordinator.state.VideoSocketState
import io.getstream.video.android.core.socket.sfu.state.SfuSocketState
import io.getstream.video.android.core.utils.safeCallWithResult
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import stream.video.sfu.event.HealthCheckRequest
import stream.video.sfu.event.JoinRequest
import stream.video.sfu.event.SfuRequest
import stream.video.sfu.models.WebsocketReconnectStrategy
import java.util.UUID
import kotlin.coroutines.EmptyCoroutineContext

internal open class SfuSocket(
    private val wssUrl: String,
    private val apiKey: ApiKey,
    private val tokenManager: TokenManager,
    private val socketFactory:
    SocketFactory<SfuDataRequest, SfuParser, ConnectionConf.SfuConnectionConf>,
    private val userScope: UserScope,
    private val lifecycleObserver: StreamLifecycleObserver,
    private val networkStateProvider: NetworkStateProvider,
) {
    private var streamWebSocket: StreamWebSocket<SfuDataRequest, SfuParser>? = null
    open val logger by taggedLogger(TAG)
    private val socketId = safeCallWithResult { UUID.randomUUID().toString() }
    private var connectionConf: ConnectionConf.SfuConnectionConf? = null
    private val listeners = mutableSetOf<SocketListener<SfuDataEvent, JoinCallResponseEvent>>()
    private val sfuSocketStateService = SfuSocketStateService()
    private var socketStateObserverJob: Job? = null
    private val healthMonitor = HealthMonitor(
        monitorInterval = 5000L,
        noEventIntervalThreshold = 15000L,
        userScope = userScope,
        checkCallback = {
            sendEvent(
                SfuDataRequest(
                    SfuRequest(health_check_request = HealthCheckRequest()),
                ),
            )
        },
        reconnectCallback = { sfuSocketStateService.onWebSocketEventLost() },
    )
    private val lifecycleHandler = NoOpLifecycleHandler()

    private val networkStateListener = object : NetworkStateProvider.NetworkStateListener {
        override suspend fun onConnected() {
            sfuSocketStateService.onNetworkAvailable()
        }

        override suspend fun onDisconnected() {
            sfuSocketStateService.onNetworkNotAvailable()
        }
    }

    @Suppress("ComplexMethod")
    private fun observeSocketStateService(): Job {
        var socketListenerJob: Job? = null

        suspend fun connectUser(connectionConf: ConnectionConf.SfuConnectionConf) {
            logger.d { "[connectUser] connectionConf: $connectionConf" }
            userScope.launch { startObservers() }
            this.connectionConf = connectionConf
            socketListenerJob?.cancel()
            when (networkStateProvider.isConnected()) {
                true -> {
                    streamWebSocket =
                        socketFactory.createSocket<SfuDataEvent>(connectionConf, "#sfu").apply {
                            listeners.forEach { it.onCreated() }

                            logger.d { "[connectUser] send join request = ${connectionConf.joinRequest}" }
                            send(
                                SfuDataRequest(
                                    SfuRequest(
                                        join_request = connectionConf.joinRequest,
                                    ),
                                ),
                            )

                            socketListenerJob = listen().onEach {
                                when (it) {
                                    is StreamWebSocketEvent.Error -> handleError(it)
                                    is StreamWebSocketEvent.SfuMessage -> when (
                                        val event =
                                            it.sfuEvent
                                    ) {
                                        is ErrorEvent -> handleError(event.toNetworkError())
                                        else -> handleEvent(event)
                                    }

                                    else -> {
                                        handleEvent(UnknownEvent(it))
                                    }
                                }
                            }.launchIn(userScope)
                        }
                }

                false -> sfuSocketStateService.onNetworkNotAvailable()
            }
        }

        return userScope.launch {
            sfuSocketStateService.observer { state ->
                logger.i { "[onSocketStateChanged] state: $state" }
                when (state) {
                    is SfuSocketState.RestartConnection -> {
                        connectionConf?.let { sfuSocketStateService.onReconnect(it) }
                            ?: run {
                                logger.e { "[onSocketStateChanged] #reconnect; connectionConf is null" }
                            }
                    }

                    is SfuSocketState.Connected -> {
                        healthMonitor.ack()
                        callListeners { listener -> listener.onConnected(state.event) }
                    }

                    is SfuSocketState.Connecting -> {
                        connectUser(state.connectionConf)
                        callListeners { listener -> listener.onConnecting() }
                    }

                    is SfuSocketState.Disconnected -> {
                        when (state) {
                            is SfuSocketState.Disconnected.DisconnectedByRequest -> {
                                streamWebSocket?.close()
                                healthMonitor.stop()
                                userScope.launch { disposeObservers() }
                            }

                            is SfuSocketState.Disconnected.NetworkDisconnected -> {
                                streamWebSocket?.close()
                                healthMonitor.stop()
                            }

                            is SfuSocketState.Disconnected.Rejoin -> {
                                streamWebSocket?.close()
                                healthMonitor.stop()
                                disposeNetworkStateObserver()
                            }

                            is SfuSocketState.Disconnected.Stopped -> {
                                streamWebSocket?.close()
                                healthMonitor.stop()
                                disposeNetworkStateObserver()
                            }

                            is SfuSocketState.Disconnected.DisconnectedPermanently -> {
                                streamWebSocket?.close()
                                healthMonitor.stop()
                                userScope.launch { disposeObservers() }
                            }

                            is SfuSocketState.Disconnected.DisconnectedTemporarily -> {
                                healthMonitor.onDisconnected()
                            }

                            is SfuSocketState.Disconnected.WebSocketEventLost -> {
                                streamWebSocket?.close()
                                connectionConf?.let {
                                    sfuSocketStateService.onReconnect(
                                        it,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun connect(joinRequest: JoinRequest) {
        logger.d { "[connect] request: ${joinRequest.client_details}" }
        socketStateObserverJob?.cancel()
        socketStateObserverJob = observeSocketStateService()
        sfuSocketStateService.onConnect(
            ConnectionConf.SfuConnectionConf(
                wssUrl,
                apiKey,
                User.anonymous(),
                joinRequest,
                tokenManager.getToken(),
            ),
        )
    }

    suspend fun disconnect() {
        logger.d { "[disconnect] no args" }
        connectionConf = null
        sfuSocketStateService.onRequiredDisconnect()
    }

    private suspend fun handleEvent(sfuEvent: SfuDataEvent) {
        when (sfuEvent) {
            is JoinCallResponseEvent -> sfuSocketStateService.onConnectionEstablished(sfuEvent)
            is SFUHealthCheckEvent -> {
                if (isConnected()) {
                    healthMonitor.ack()
                }
            }

            else -> {
                // Ignore, maybe handled on upper levels.
            }
        }
        listeners.forEach { listener -> listener.onEvent(sfuEvent) }
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

    private suspend fun onVideoNetworkError(
        error: Error.NetworkError,
        reconnectStrategy: WebsocketReconnectStrategy?,
    ) {
        if (VideoErrorCode.isAuthenticationError(error.serverErrorCode)) {
            tokenManager.expireToken()
        }

        when (error.serverErrorCode) {
            VideoErrorCode.UNDEFINED_TOKEN.code,
            VideoErrorCode.INVALID_TOKEN.code,
            VideoErrorCode.API_KEY_NOT_FOUND.code,
            VideoErrorCode.VALIDATION_ERROR.code,
            VideoErrorCode.SOCKET_CLOSED.code,
            -> {
                logger.d {
                    "One unrecoverable error happened. Error: $error. Error code: ${error.serverErrorCode}"
                }
                sfuSocketStateService.onUnrecoverableError(error)
            }

            else -> sfuSocketStateService.onNetworkError(
                error,
                reconnectStrategy
                    ?: WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_UNSPECIFIED,
            )
        }
    }

    fun removeListener(listener: SocketListener<SfuDataEvent, JoinCallResponseEvent>) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    fun addListener(listener: SocketListener<SfuDataEvent, JoinCallResponseEvent>) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    fun state() = sfuSocketStateService.currentStateFlow

    /**
     * Attempt to send [event] to the web socket connection.
     * Returns true only if socket is connected and [okhttp3.WebSocket.send] returns true, otherwise false
     *
     * @see [okhttp3.WebSocket.send]
     */
    /**
     * Attempt to send [event] to the web socket connection.
     * Returns true only if socket is connected and [okhttp3.WebSocket.send] returns true, otherwise false
     *
     * @see [okhttp3.WebSocket.send]
     */
    internal fun sendEvent(event: SfuDataRequest): Boolean {
        logger.d { "[sendEvent] event: $event" }
        return streamWebSocket?.send(event) ?: false
    }

    /**
     * Send raw data to the web socket connection.
     */
    internal fun sendRawData(data: String) {
        logger.d { "[sendRawData] data: $data" }
        streamWebSocket?.sendRaw(data) ?: Unit
    }

    internal fun isConnected(): Boolean =
        sfuSocketStateService.currentState is SfuSocketState.Connected

    /**
     * Awaits until [VideoSocketState.Connected] is set.
     *
     * @param timeoutInMillis Timeout time in milliseconds.
     */
    /**
     * Awaits until [VideoSocketState.Connected] is set.
     *
     * @param timeoutInMillis Timeout time in milliseconds.
     */
    internal suspend fun awaitConnection(timeoutInMillis: Long = DEFAULT_CONNECTION_TIMEOUT) {
        awaitState<SfuSocketState.Connected>(timeoutInMillis)
    }

    /**
     * Awaits until specified [VideoSocketState] is set.
     *
     * @param timeoutInMillis Timeout time in milliseconds.
     */
    /**
     * Awaits until specified [VideoSocketState] is set.
     *
     * @param timeoutInMillis Timeout time in milliseconds.
     */
    internal suspend inline fun <reified T : SfuSocketState> awaitState(timeoutInMillis: Long) {
        withTimeout(timeoutInMillis) {
            sfuSocketStateService.currentStateFlow.first { it is T }
        }
    }

    /**
     * Get connection id of this connection.
     */
    /**
     * Get connection id of this connection.
     */
    internal fun connectionIdOrError(): String =
        when (sfuSocketStateService.currentState) {
            is SfuSocketState.Connected -> socketId.getOrThrow()
            else -> error("This state doesn't contain connectionId")
        }

    private fun callListeners(call: (SocketListener<SfuDataEvent, JoinCallResponseEvent>) -> Unit) {
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

    private val SfuSocketState.Disconnected.cause
        get() = when (this) {
            is SfuSocketState.Disconnected.DisconnectedByRequest,
            is SfuSocketState.Disconnected.Stopped,
            -> DisconnectCause.ConnectionReleased

            is SfuSocketState.Disconnected.NetworkDisconnected -> DisconnectCause.NetworkNotAvailable
            is SfuSocketState.Disconnected.DisconnectedPermanently -> DisconnectCause.UnrecoverableError(
                error,
            )

            is SfuSocketState.Disconnected.DisconnectedTemporarily -> DisconnectCause.Error(error)
            is SfuSocketState.Disconnected.WebSocketEventLost -> DisconnectCause.WebSocketNotAvailable
            SfuSocketState.Disconnected.Rejoin -> DisconnectCause.ConnectionReleased
        }

    private fun ErrorEvent.toNetworkError(): StreamWebSocketEvent.Error {
        val error = error?.let {
            Error.NetworkError(
                message = it.message,
                serverErrorCode = it.code.value,
                statusCode = it.code.value,
            )
        } ?: Error.NetworkError.fromVideoErrorCode(VideoErrorCode.NO_ERROR_BODY, cause = null)
        return StreamWebSocketEvent.Error(error, reconnectStrategy)
    }

    companion object {
        private const val TAG = "Video:SfuSocket"
        private const val DEFAULT_CONNECTION_TIMEOUT = 2000L
    }
}
