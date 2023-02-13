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

package io.getstream.video.android.core.socket.internal

import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.coordinator.state.UserState
import io.getstream.video.android.core.errors.DisconnectCause
import io.getstream.video.android.core.errors.VideoError
import io.getstream.video.android.core.errors.VideoErrorCode
import io.getstream.video.android.core.errors.VideoNetworkError
import io.getstream.video.android.core.events.ConnectedEvent
import io.getstream.video.android.core.events.VideoEvent
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.model.CallMetadata
import io.getstream.video.android.core.socket.SocketListener
import io.getstream.video.android.core.socket.VideoSocket
import io.getstream.video.android.core.token.internal.CredentialsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.openapitools.client.models.UserObjectRequest
import org.openapitools.client.models.VideoWSAuthMessageRequest
import stream.video.coordinator.client_v1_rpc.WebsocketHealthcheck
import kotlin.math.pow
import kotlin.properties.Delegates

/**
 * Socket implementation used to handle the lifecycle of a WebSocket and its related state.
 *
 * @property wssUrl Base URL for the API socket.
 * @property credentialsManager Wrapper around a token providing service that manages its validity.
 * @property socketFactory Factory used to build new socket instances.
 * @property networkStateProvider Provides the network state and lifecycle handles.
 * @property coroutineScope The scope used to launch any operations.
 */
internal class VideoSocketImpl(
    private val wssUrl: String,
    private val credentialsManager: CredentialsManager,
    private val socketFactory: SocketFactory,
    private val networkStateProvider: NetworkStateProvider,
    private val userState: UserState,
    private val coroutineScope: CoroutineScope,
) : VideoSocket {

    private val logger by taggedLogger("Call:CoordSocket")

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
    private var call: CallMetadata? = null

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
                        WebsocketHealthcheck(
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
            logger.i { "[onNetworkConnected] state: $state" }
            if (state is State.DisconnectedTemporarily || state == State.NetworkDisconnected) {
                reconnect(connectionConf)
            }
        }

        override fun onDisconnected() {
            logger.i { "[onNetworkDisconnected] state: $state" }
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
        logger.i { "[onStateChanged] $newState <= $oldState" }
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
        logger.e { "[onSocketError] state: $state, error: $error" }
        if (state !is State.DisconnectedPermanently) {
            callListeners { it.onError(error) }
            (error as? VideoNetworkError)?.let(::onNetworkError)
        }
    }

    private fun onNetworkError(error: VideoNetworkError) {
        if (VideoErrorCode.isAuthenticationError(error.streamCode)) {
            credentialsManager.expireToken()
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
        logger.d { "[connectSocket] wssUrl: $wssUrl" }
        connect(SocketFactory.ConnectionConf(wssUrl, credentialsManager.getApiKey(), ""))
    }

    override fun authenticateUser() {
        val user = userState.user.value
        logger.d { "[authenticateUser] user: $user" }

        socket?.authenticate(
            VideoWSAuthMessageRequest( // TODO - add user device request
                token = user.token,
                userDetails = UserObjectRequest(
                    id = user.id,
                    role = user.role
                )
            )
        )
    }

    override fun reconnect() {
        logger.d { "[reconnect] wssUrl: $wssUrl" }
        reconnect(SocketFactory.ConnectionConf(wssUrl, credentialsManager.getApiKey(), ""))
    }

    internal fun connect(connectionConf: SocketFactory.ConnectionConf) {
        val isNetworkConnected = networkStateProvider.isConnected()
        logger.d { "[connect] conf: $connectionConf, isNetworkConnected: $isNetworkConnected" }
        this.connectionConf = connectionConf
        if (isNetworkConnected) {
            setupSocket(connectionConf)
        } else {
            state = State.NetworkDisconnected
        }
        networkStateProvider.subscribe(networkStateListener)
    }

    override fun updateCallState(call: CallMetadata?) {
        logger.v { "[updateCallState] call: $call" }
        this.call = call
    }

    override fun getCallState(): CallMetadata? = call

    override fun releaseConnection() {
        logger.i { "[releaseConnection] wssUrl: $wssUrl" }
        state = State.DisconnectedByRequest
    }

    override fun onConnectionResolved(event: ConnectedEvent) {
        logger.i { "[onConnectionResolved] event: $event" }
        this.clientId = event.clientId
        state = State.Connected(event)
    }

    override fun onEvent(event: VideoEvent) {
        healthMonitor.ack()
        callListeners { listener -> listener.onEvent(event) }
    }

    internal fun sendPing(state: WebsocketHealthcheck) {
        socket?.ping(state)
    }

    private fun reconnect(connectionConf: SocketFactory.ConnectionConf?) {
        logger.d { "[reconnect] conf: $connectionConf" }
        shutdownSocketConnection()
        setupSocket(connectionConf?.asReconnectionConf())
    }

    private fun setupSocket(connectionConf: SocketFactory.ConnectionConf?) {
        logger.d { "[setupSocket] conf: $connectionConf" }
        state = when (connectionConf) {
            null -> State.DisconnectedPermanently(null)
            else -> {
                socketConnectionJob = coroutineScope.launch {
                    credentialsManager.ensureTokenLoaded()

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
        logger.i { "[shutdownSocketConnection] state: $state" }
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
        object Connecting : State() {
            override fun toString(): String = "Connecting"
        }

        data class Connected(val event: ConnectedEvent) : State()
        object NetworkDisconnected : State() {
            override fun toString(): String = "NetworkDisconnected"
        }

        data class DisconnectedTemporarily(val error: VideoNetworkError?) : State()
        data class DisconnectedPermanently(val error: VideoNetworkError?) : State()
        object DisconnectedByRequest : State() {
            override fun toString(): String = "DisconnectedByRequest"
        }
    }
}
