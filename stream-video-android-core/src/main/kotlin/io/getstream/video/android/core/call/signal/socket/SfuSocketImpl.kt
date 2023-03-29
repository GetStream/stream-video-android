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

package io.getstream.video.android.core.call.signal.socket

import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import io.getstream.log.taggedLogger
import io.getstream.result.StreamError
import io.getstream.video.android.core.errors.DisconnectCause
import io.getstream.video.android.core.events.ConnectedEvent
import io.getstream.video.android.core.events.HealthCheckResponseEvent
import io.getstream.video.android.core.events.SfuDataEvent
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.socket.internal.EventsParser
import io.getstream.video.android.core.socket.internal.HealthMonitor
import io.getstream.video.android.core.socket.internal.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import stream.video.sfu.event.HealthCheckRequest
import stream.video.sfu.event.JoinRequest
import kotlin.properties.Delegates

internal class SfuSocketImpl(
    private val wssUrl: String,
    private val networkStateProvider: NetworkStateProvider,
    private val sfuSocketFactory: SfuSocketFactory,
    private val coroutineScope: CoroutineScope,
) : SfuSocket {

    private val logger by taggedLogger("Call:SfuSocket")

    private var connectionConf: SfuSocketFactory.ConnectionConf? = null
    private val listeners: MutableList<SfuSocketListener> = mutableListOf()

    private var socket: Socket? = null
    private var eventsParser: SignalEventsParser? = null
    private var sessionId: String = ""

    private var socketConnectionJob: Job? = null
    private val eventUiHandler = Handler(Looper.getMainLooper())

    private val healthMonitor = HealthMonitor(
        object : HealthMonitor.HealthCallback {
            override fun reconnect() {
                if (state is State.DisconnectedTemporarily) {
                    this@SfuSocketImpl.reconnect(connectionConf)
                }
            }

            override fun check() {
                (state as? State.Connected)?.let {
                    socket?.pingCall(
                        HealthCheckRequest()
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

    override fun removeListener(sfuSocketListener: SfuSocketListener) {
        synchronized(listeners) {
            listeners.remove(sfuSocketListener)
        }
    }

    override fun addListener(sfuSocketListener: SfuSocketListener) {
        synchronized(listeners) {
            listeners.add(sfuSocketListener)
        }
    }

    override fun connectSocket() {
        logger.d { "[connectSocket] wssUrl: $wssUrl" }
        connect(SfuSocketFactory.ConnectionConf(wssUrl))
    }

    override fun sendJoinRequest(request: JoinRequest) {
        logger.d { "[sendJoinRequest] socketExists: ${socket != null}, request: $request" }
        this.sessionId = request.session_id
        socket?.joinCall(request)
    }

    override fun reconnect() {
        logger.d { "[reconnect] wssUrl: $wssUrl" }
        reconnect(SfuSocketFactory.ConnectionConf(wssUrl))
    }

    internal fun connect(connectionConf: SfuSocketFactory.ConnectionConf) {
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

    override fun releaseConnection() {
        logger.i { "[releaseConnection] wssUrl: $wssUrl" }
        state = State.DisconnectedByRequest
    }

    override fun onConnectionResolved(event: ConnectedEvent) {
        logger.i { "[onConnectionResolved] event: $event" }
        state = State.Connected(event)
    }

    override fun onEvent(event: SfuDataEvent) {
        healthMonitor.ack()
        callListeners { listener ->
            if (event !is HealthCheckResponseEvent) {
                logger.d { "[onEvent] Sfu Event: $event" }
                listener.onEvent(event)
            }
        }
    }

    private fun reconnect(connectionConf: SfuSocketFactory.ConnectionConf?) {
        logger.d { "[reconnect] conf: $connectionConf" }
        shutdownSocketConnection()
        setupSocket(connectionConf?.asReconnectionConf())
    }

    private fun setupSocket(connectionConf: SfuSocketFactory.ConnectionConf?) {
        logger.d { "[setupSocket] conf: $connectionConf" }
        state = when (connectionConf) {
            null -> State.DisconnectedPermanently(null)
            else -> {
                socketConnectionJob = coroutineScope.launch {
                    socket = sfuSocketFactory.createSocket(
                        createNewEventsParser(),
                        connectionConf
                    )
                }
                State.Connecting
            }
        }
    }

    override fun onSocketError(error: StreamError) {
        logger.e { "[onSocketError] state: $state, error: $error" }
        if (state !is State.DisconnectedPermanently) {
            callListeners { it.onError(error) }
            // (error as? VideoNetworkError)?.let(::onNetworkError) TODO - which errors can we get here
        }
    }

    private fun createNewEventsParser(): SignalEventsParser = SignalEventsParser(this).also {
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

    private fun callListeners(call: (SfuSocketListener) -> Unit) {
        synchronized(listeners) {
            listeners.forEach { listener ->
                eventUiHandler.post { call(listener) }
            }
        }
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

        data class DisconnectedTemporarily(val error: StreamError.NetworkError?) : State()
        data class DisconnectedPermanently(val error: StreamError.NetworkError?) : State()
        object DisconnectedByRequest : State() {
            override fun toString(): String = "DisconnectedByRequest"
        }
    }
}
