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

package io.getstream.video.android.socket

import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import io.getstream.video.android.client.user.UserState
import io.getstream.video.android.errors.DisconnectCause
import io.getstream.video.android.errors.VideoError
import io.getstream.video.android.errors.VideoErrorCode
import io.getstream.video.android.errors.VideoNetworkError
import io.getstream.video.android.events.ConnectedEvent
import io.getstream.video.android.network.NetworkStateProvider
import io.getstream.video.android.token.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import stream.video.AuthPayload
import stream.video.Call
import stream.video.CreateUserRequest
import stream.video.Healthcheck
import kotlin.math.pow
import kotlin.properties.Delegates

/**
 * Socket implementation used to handle the lifecycle of a WebSocket and its related state.
 *
 * @property apiKey The key of the application connecting to the API.
 * @property wssUrl Base URL for the API socket.
 * @property tokenManager Wrapper around a token providing service that manages its validity.
 * @property socketFactory Factory used to build new socket instances.
 * @property networkStateProvider Provides the network state and lifecycle handles.
 * @property coroutineScope The scope used to launch any operations.
 */
internal class VideoSocketImpl(
    private val apiKey: String,
    private val wssUrl: String,
    private val tokenManager: TokenManager,
    private val socketFactory: SocketFactory,
    private val networkStateProvider: NetworkStateProvider,
    private val userState: UserState,
    private val coroutineScope: CoroutineScope,
) : VideoSocket {
    private var connectionConf: SocketFactory.ConnectionConf? = null
    private var socket: Socket? = null
    private var eventsParser: EventsParser? = null
    private var clientId: String = ""

    private var socketConnectionJob: Job? = null
    private val listeners = mutableSetOf<SocketListener>()
    private val eventUiHandler = Handler(Looper.getMainLooper())

    /**
     * Call related state.
     */
    private var call: Call? = null

    private val healthMonitor = HealthMonitor(
        object : HealthMonitor.HealthCallback {
            override fun reconnect() {
                if (state is State.DisconnectedTemporarily) {
                    this@VideoSocketImpl.reconnect(connectionConf)
                }
            }

            override fun check() {
                (state as? State.Connected)?.let {
                    sendPing(
                        Healthcheck(
                            user_id = userState.user.value.id,
                            client_id = clientId,
                            call_type = call?.type ?: "",
                            call_id = call?.id ?: "",
                        )
                    )
                }
            }
        }
    )
    private val networkStateListener = object : NetworkStateProvider.NetworkStateListener {
        override fun onConnected() {
            if (state is State.DisconnectedTemporarily || state == State.NetworkDisconnected) {
                reconnect(connectionConf)
            }
        }

        override fun onDisconnected() {
            healthMonitor.stop()
            if (state is State.Connected || state is State.Connecting) {
                state = State.NetworkDisconnected
            }
        }
    }

    private var reconnectionAttempts = 0

    @VisibleForTesting
    internal var state: State by Delegates.observable(
        State.DisconnectedTemporarily(null) as State
    ) { _, oldState, newState ->
        if (oldState != newState) {
            when (newState) {
                is State.Connecting -> {
                    healthMonitor.stop()
                    callListeners { it.onConnecting() }
                }
                is State.Connected -> {
                    healthMonitor.start()
                    callListeners { it.onConnected(newState.event) }
                }
                is State.NetworkDisconnected -> {
                    shutdownSocketConnection()
                    healthMonitor.stop()
                    callListeners { it.onDisconnected(DisconnectCause.NetworkNotAvailable) }
                }
                is State.DisconnectedByRequest -> {
                    shutdownSocketConnection()
                    healthMonitor.stop()
                    callListeners { it.onDisconnected(DisconnectCause.ConnectionReleased) }
                }
                is State.DisconnectedTemporarily -> {
                    shutdownSocketConnection()
                    healthMonitor.onDisconnected()
                    callListeners { it.onDisconnected(DisconnectCause.Error(newState.error)) }
                }
                is State.DisconnectedPermanently -> {
                    shutdownSocketConnection()
                    connectionConf = null
                    networkStateProvider.unsubscribe(networkStateListener)
                    healthMonitor.stop()
                    callListeners { it.onDisconnected(DisconnectCause.UnrecoverableError(newState.error)) }
                }
            }
        }
    }
        private set

    override fun onSocketError(error: VideoError) {
        if (state !is State.DisconnectedPermanently) {
            callListeners { it.onError(error) }
            (error as? VideoNetworkError)?.let(::onNetworkError)
        }
    }

    private fun onNetworkError(error: VideoNetworkError) {
        if (VideoErrorCode.isAuthenticationError(error.streamCode)) {
            tokenManager.expireToken()
        }

        when (error.streamCode) {
            VideoErrorCode.PARSER_ERROR.code,
            VideoErrorCode.CANT_PARSE_CONNECTION_EVENT.code,
            VideoErrorCode.CANT_PARSE_EVENT.code,
            VideoErrorCode.UNABLE_TO_PARSE_SOCKET_EVENT.code,
            VideoErrorCode.NO_ERROR_BODY.code,
            -> {
                if (reconnectionAttempts < RETRY_LIMIT) {
                    coroutineScope.launch {
                        delay(DEFAULT_DELAY * reconnectionAttempts.toDouble().pow(2.0).toLong())
                        reconnect(connectionConf)
                        reconnectionAttempts += 1
                    }
                }
            }
            VideoErrorCode.UNDEFINED_TOKEN.code,
            VideoErrorCode.INVALID_TOKEN.code,
            VideoErrorCode.API_KEY_NOT_FOUND.code,
            VideoErrorCode.VALIDATION_ERROR.code,
            -> {
                state = State.DisconnectedPermanently(error)
            }
            else -> {
                state = State.DisconnectedTemporarily(error)
            }
        }
    }

    override fun removeListener(socketListener: SocketListener) {
        synchronized(listeners) {
            listeners.remove(socketListener)
        }
    }

    override fun addListener(socketListener: SocketListener) {
        synchronized(listeners) {
            listeners.add(socketListener)
        }
    }

    override fun connectSocket() {
        connect(SocketFactory.ConnectionConf(wssUrl))
    }

    override fun authenticateUser() {
        val user = userState.user.value

        socket?.authenticate(
            AuthPayload(
                user = CreateUserRequest(id = user.id, name = user.name),
                token = tokenManager.getToken()
            )
        )
    }

    override fun reconnect() {
        reconnect(SocketFactory.ConnectionConf(wssUrl))
    }

    internal fun connect(connectionConf: SocketFactory.ConnectionConf) {
        val isNetworkConnected = networkStateProvider.isConnected()
        this.connectionConf = connectionConf
        if (isNetworkConnected) {
            setupSocket(connectionConf)
        } else {
            state = State.NetworkDisconnected
        }
        networkStateProvider.subscribe(networkStateListener)
    }

    override fun updateCallState(call: Call?) {
        this.call = call
    }

    override fun getCallState(): Call? = call

    override fun releaseConnection() {
        state = State.DisconnectedByRequest
    }

    override fun onConnectionResolved(event: ConnectedEvent) {
        this.clientId = event.clientId
        state = State.Connected(event)
    }

    override fun onEvent(event: Any) {
        healthMonitor.ack()
        callListeners { listener -> listener.onEvent(event) }
    }

    internal fun sendPing(state: Healthcheck) {
        socket?.ping(state)
    }

    private fun reconnect(connectionConf: SocketFactory.ConnectionConf?) {
        shutdownSocketConnection()
        setupSocket(connectionConf?.asReconnectionConf())
    }

    private fun setupSocket(connectionConf: SocketFactory.ConnectionConf?) {
        state = when (connectionConf) {
            null -> State.DisconnectedPermanently(null)
            else -> {
                socketConnectionJob = coroutineScope.launch {
                    tokenManager.ensureTokenLoaded()

                    socket = socketFactory.createSocket(createNewEventsParser(), connectionConf)
                }
                State.Connecting
            }
        }
    }

    private fun createNewEventsParser(): EventsParser = EventsParser(this).also {
        eventsParser = it
    }

    private fun shutdownSocketConnection() {
        socketConnectionJob?.cancel()
        eventsParser?.closeByClient()
        eventsParser = null
        socket?.close(EventsParser.CODE_CLOSE_SOCKET_FROM_CLIENT, "Connection close by client")
        socket = null
    }

    private fun callListeners(call: (SocketListener) -> Unit) {
        synchronized(listeners) {
            listeners.forEach { listener ->
                eventUiHandler.post { call(listener) }
            }
        }
    }

    private companion object {
        private const val RETRY_LIMIT = 3
        private const val DEFAULT_DELAY = 500
    }

    @VisibleForTesting
    internal sealed class State {
        object Connecting : State()
        data class Connected(val event: ConnectedEvent) : State()
        object NetworkDisconnected : State()
        class DisconnectedTemporarily(val error: VideoNetworkError?) : State()
        class DisconnectedPermanently(val error: VideoNetworkError?) : State()
        object DisconnectedByRequest : State()
    }
}
