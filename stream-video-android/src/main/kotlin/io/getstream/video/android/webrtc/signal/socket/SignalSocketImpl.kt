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

package io.getstream.video.android.webrtc.signal.socket

import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import io.getstream.video.android.errors.DisconnectCause
import io.getstream.video.android.errors.VideoError
import io.getstream.video.android.errors.VideoNetworkError
import io.getstream.video.android.events.ConnectedEvent
import io.getstream.video.android.events.SfuDataEvent
import io.getstream.video.android.network.NetworkStateProvider
import io.getstream.video.android.socket.EventsParser
import io.getstream.video.android.socket.HealthMonitor
import io.getstream.video.android.socket.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

internal class SignalSocketImpl(
    private val wssUrl: String,
    private val networkStateProvider: NetworkStateProvider,
    private val signalSocketFactory: SignalSocketFactory,
    private val coroutineScope: CoroutineScope,
) : SignalSocket {

    private var connectionConf: SignalSocketFactory.ConnectionConf? = null
    private val listeners: MutableList<SignalSocketListener> = mutableListOf()

    private var socket: Socket? = null
    private var eventsParser: SignalEventsParser? = null
    private var clientId: String = ""

    private var socketConnectionJob: Job? = null
    private val eventUiHandler = Handler(Looper.getMainLooper())

    private val healthMonitor = HealthMonitor(
        object : HealthMonitor.HealthCallback {
            override fun reconnect() {
                if (state is State.DisconnectedTemporarily) {
                    this@SignalSocketImpl.reconnect(connectionConf)
                }
            }

            override fun check() {
                (state as? State.Connected)?.let {
                    // TODO - send a ping - proto called HealthcheckRequest
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

    override fun removeListener(signalSocketListener: SignalSocketListener) {
        synchronized(listeners) {
            listeners.remove(signalSocketListener)
        }
    }

    override fun addListener(signalSocketListener: SignalSocketListener) {
        synchronized(listeners) {
            listeners.add(signalSocketListener)
        }
    }

    override fun connectSocket() {
        connect(SignalSocketFactory.ConnectionConf(wssUrl))
    }

    override fun reconnect() {
        reconnect(SignalSocketFactory.ConnectionConf(wssUrl))
    }

    internal fun connect(connectionConf: SignalSocketFactory.ConnectionConf) {
        val isNetworkConnected = networkStateProvider.isConnected()
        this.connectionConf = connectionConf
        if (isNetworkConnected) {
            setupSocket(connectionConf)
        } else {
            state = State.NetworkDisconnected
        }
        networkStateProvider.subscribe(networkStateListener)
    }

    override fun releaseConnection() {
        state = State.DisconnectedByRequest
    }

    override fun onConnectionResolved(event: ConnectedEvent) {
        this.clientId = event.clientId
        state = State.Connected(event)
    }

    override fun onEvent(event: SfuDataEvent) {
        healthMonitor.ack()
        callListeners { listener -> listener.onEvent(event) }
    }

    private fun reconnect(connectionConf: SignalSocketFactory.ConnectionConf?) {
        shutdownSocketConnection()
        setupSocket(connectionConf?.asReconnectionConf())
    }

    private fun setupSocket(connectionConf: SignalSocketFactory.ConnectionConf?) {
        state = when (connectionConf) {
            null -> State.DisconnectedPermanently(null)
            else -> {
                socketConnectionJob = coroutineScope.launch {
                    socket =
                        signalSocketFactory.createSocket(createNewEventsParser(), connectionConf)
                }
                State.Connecting
            }
        }
    }

    override fun onSocketError(error: VideoError) {
        if (state !is State.DisconnectedPermanently) {
            callListeners { it.onError(error) }
            // (error as? VideoNetworkError)?.let(::onNetworkError) TODO - which errors can we get here
        }
    }

    private fun createNewEventsParser(): SignalEventsParser = SignalEventsParser(this).also {
        eventsParser = it
    }

    private fun shutdownSocketConnection() {
        socketConnectionJob?.cancel()
        eventsParser?.closeByClient()
        eventsParser = null
        socket?.close(EventsParser.CODE_CLOSE_SOCKET_FROM_CLIENT, "Connection close by client")
        socket = null
    }

    private fun callListeners(call: (SignalSocketListener) -> Unit) {
        synchronized(listeners) {
            listeners.forEach { listener ->
                eventUiHandler.post { call(listener) }
            }
        }
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
