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

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.socket.internal.HealthMonitor
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.openapitools.client.models.VideoEvent
import stream.video.sfu.event.HealthCheckRequest
import java.io.EOFException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * PersistentSocket architecture
 *
 * - Healthmonitor that sends a ping every 30 seconds
 * - Automatically reconnects if it encounters a temp failure
 * - Raises the error if there is a permanent failure
 * - Flow to avoid concurrency related bugs
 * - Ability to wait till the socket is connected (important to prevent race conditions)
 */
public open class PersistentSocket<T>(
    /** The URL to connect to */
    private val url: String,
    /** Inject your http client */
    private val httpClient: OkHttpClient,
    /** Inject your network state provider */
    private val networkStateProvider: NetworkStateProvider,
    /** Set the scope everything should run in */
    private val scope: CoroutineScope = CoroutineScope(DispatcherProvider.IO),
    private val onFastReconnected: suspend () -> Unit,
) : WebSocketListener() {
    internal open val logger by taggedLogger("PersistentSocket")

    internal val singleThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    /** Mock the socket for testing */
    internal var mockSocket: WebSocket? = null

    var reconnectTimeout: Long = 500

    /** flow with all the events, listen to this */
    val events = MutableSharedFlow<VideoEvent>(replay = 3)

    /** flow with temporary and permanent errors */
    val errors = MutableSharedFlow<Throwable>()

    private val _connectionState = MutableStateFlow<SocketState>(SocketState.NotConnected)

    /** the current connection state of the socket */
    val connectionState: StateFlow<SocketState> = _connectionState

    /** the connection id */
    internal val _connectionId: MutableStateFlow<String?> = MutableStateFlow(null)
    val connectionId: StateFlow<String?> = _connectionId

    /** Continuation if the socket successfully connected and we've authenticated */
    lateinit var connectContinuation: CancellableContinuation<T>

    // Controls when we can resume the continuation
    private var connectContinuationCompleted: Boolean = false

    internal var socket: WebSocket? = null

    // True if cleanup was called and socket is completely destroyed (intentionally).
    // You need to create a new instance (this is mainly used for the SfuSocket which is tied
    // to a Subscriber SDP and needs to be recreated from scratch on WebRTC session clean up).
    protected var destroyed: Boolean = false

    internal var reconnectionAttempts = 0

    /**
     * Connect the socket, authenticate, start the health monitor and see if the network is online
     * @param invocation Provides a way to extend the [connect] method with additional behavior.
     * This can be useful in cases where additional setup or checks need to be performed
     * once the socket is connected but before the [connect] method returns.
     * To return from [connect] and to resume the enclosing coroutine, use the provided [CancellableContinuation] parameter.
     */
    open suspend fun connect(
        invocation: (CancellableContinuation<T>) -> Unit = {},
    ): T? {
        if (destroyed) {
            logger.d { "[connect] Can't connect socket - it was already destroyed" }
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            logger.i { "[connect]" }
            connectContinuation = continuation

            _connectionState.value = SocketState.Connecting

            // step 1 create the socket
            socket = mockSocket ?: createSocket()

            scope.launch {
                // step 2 authenticate the user/call etc
                authenticate()

                // step 3 monitor for health every 30 seconds

                if (!DispatcherProvider.inTest) {
                    healthMonitor.start()
                }

                // also monitor if we are offline/online
                networkStateProvider.subscribe(networkStateListener)

                // run the invocation
                invocation.invoke(continuation)
            }
        }
    }

    /**
     * Used for testing only - to bring down the socket connection immediately.
     */
    internal fun cancel() {
        socket?.cancel()
    }

    fun cleanup() {
        destroyed = true
        disconnect(DisconnectReason.ByRequest)
    }

    sealed class DisconnectReason {
        data object ByRequest : DisconnectReason()
        data class PermanentError(val error: Throwable) : DisconnectReason()
    }

    /**
     * Disconnect the socket
     */
    fun disconnect(disconnectReason: DisconnectReason) {
        logger.i { "[disconnect]" }

        _connectionState.value = when (disconnectReason) {
            DisconnectReason.ByRequest -> SocketState.DisconnectedByRequest
            is DisconnectReason.PermanentError -> SocketState.DisconnectedPermanently(disconnectReason.error)
        }

        connectContinuationCompleted = false

        disconnectSocket()
        healthMonitor.stop()
        networkStateProvider.unsubscribe(networkStateListener)
    }

    private fun disconnectSocket() {
        socket?.close(CODE_CLOSE_SOCKET_FROM_CLIENT, "Connection close by client")
        socket = null
        _connectionId.value = null
    }

    /**
     * Increment the reconnection attempts, disconnect and reconnect
     */
    suspend fun reconnect(timeout: Long = reconnectTimeout) {
        logger.i { "[reconnect] reconnectionAttempts: $reconnectionAttempts" }
        if (destroyed) {
            logger.d { "[reconnect] Can't reconnect socket - it was already destroyed" }
            return
        }

        if (connectionState.value == SocketState.Connecting) {
            logger.i { "[reconnect] already connecting" }
            return
        }

        // Don't disconnect if we are already disconnected
        disconnectSocket()

        // Stop sending pings
        healthMonitor.stop()

        if (!networkStateProvider.isConnected()) {
            logger.d { "[reconnect] skipping reconnect - disconnected from internet" }
            return
        }

        // reconnect after the timeout
        delay(timeout)

        reconnectionAttempts++

        tryConnect()
    }

    private suspend fun tryConnect() {
        try {
            connectContinuationCompleted = false
            connect()
        } catch (e: Throwable) {
            logger.e { "[reconnect] failed to reconnect: $e" }
        }
    }

    open fun authenticate() { }

    suspend fun onInternetConnected() {
        val state = connectionState.value
        logger.i { "[onNetworkConnected] state: $state" }
        if (state is SocketState.DisconnectedTemporarily || state == SocketState.NetworkDisconnected) {
            // reconnect instantly when the internet is back
            reconnect(0)
        }
    }

    suspend fun onInternetDisconnected() {
        logger.i { "[onNetworkDisconnected] state: $connectionState.value" }
        if (connectionState.value is SocketState.Connected || connectionState.value is SocketState.Connecting) {
            _connectionState.value = SocketState.NetworkDisconnected
        }
    }

    private val networkStateListener = object : NetworkStateProvider.NetworkStateListener {
        override fun onConnected() {
            scope.launch { onInternetConnected() }
        }

        override fun onDisconnected() {
            scope.launch { onInternetDisconnected() }
        }
    }

    private fun createSocket(): WebSocket {
        logger.d { "[createSocket] url: $url" }

        val request = Request.Builder()
            .url(url)
            .addHeader("Connection", "Upgrade")
            .addHeader("Upgrade", "websocket")
            .addHeader("X-Stream-Client", StreamVideo.buildSdkTrackingHeaders())
            .build()

        return httpClient.newWebSocket(request, this)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        logger.d { "[onOpen] response: $response" }
    }

    protected fun ackHealthMonitor() {
        healthMonitor.ack()
    }

    protected fun setConnectedStateAndContinue(message: VideoEvent) {
        _connectionState.value = SocketState.Connected(message)

        if (!connectContinuationCompleted) {
            connectContinuationCompleted = true
            connectContinuation.resume(message as T)
        }
    }

    internal fun handleError(receivedError: Throwable) {
        // onFailure, onClosed and the 2 onMessage can all generate errors
        // temporary errors should be logged and retried
        // permanent errors should be emitted so the app can decide how to handle it
        if (destroyed) {
            logger.d { "[handleError] Ignoring socket error - already closed $receivedError" }
            return
        }
        val error = receivedError.let {
            // TODO Alex: This is a bad assumption, but necessary, needs to be checked against the backend
            if (it is EOFException) {
                ErrorResponse(
                    40,
                    "Unknown error trying to refresh token and reconnect.",
                    statusCode = 401,
                )
            } else {
                it
            }
        }
        val permanentError = isPermanentError(error)
        if (permanentError) {
            logger.e { "[handleError] Permanent error: $error" }

            _connectionState.value = SocketState.DisconnectedPermanently(error)

            // If the connect continuation is not completed, it means the error happened during the connection phase.
            connectContinuationCompleted.not().let { isConnectionPhaseError ->
                if (isConnectionPhaseError) {
                    emitError(error, isConnectionPhaseError = true)
                    resumeConnectionPhaseWithException(error)
                } else {
                    emitError(error, isConnectionPhaseError = false)
                }
            }
        } else {
            logger.w { "[handleError] Temporary error: $error" }

            _connectionState.value = SocketState.DisconnectedTemporarily(error)
            scope.launch { reconnect(reconnectTimeout) }
        }
    }

    internal open fun isPermanentError(error: Throwable): Boolean {
        // errors returned by the server can be permanent. IE an invalid API call
        // or an expired token (required a refresh)
        // or temporary
        var isPermanent = true
        if (error is ErrorResponse) {
            val serverError = error as ErrorResponse
        } else {
            // there are several timeout & network errors that are all temporary
            // code errors are permanent
            isPermanent = when (error) {
                is UnknownHostException -> false
                is SocketTimeoutException -> false
                is InterruptedIOException -> false
                is IOException -> false
                else -> true
            }
        }

        return isPermanent
    }

    private fun emitError(error: Throwable, isConnectionPhaseError: Boolean) {
        scope.launch {
            if (isConnectionPhaseError) {
                errors.emit(
                    ConnectException(
                        "Failed to establish WebSocket connection. Will try to reconnect. Cause: ${error.message}",
                    ),
                )
            } else {
                errors.emit(error)
            }
        }
    }

    private fun resumeConnectionPhaseWithException(error: Throwable) {
        connectContinuationCompleted = true
        connectContinuation.resumeWithException(error)
    }

    /**
     * Invoked when the remote peer has indicated that no more incoming messages will be transmitted.
     */
    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        logger.d { "[onClosing] code: $code, reason: $reason" }
    }

    /**
     * Invoked when both peers have indicated that no more messages will be transmitted and the
     * connection has been successfully released. No further calls to this listener will be made.
     */
    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        logger.d { "[onClosed] code: $code, reason: $reason" }

        if (destroyed) {
            logger.d { "[onClosed] Ignoring onClosed - socket already closed" }
            return
        }

        if (code != CODE_CLOSE_SOCKET_FROM_CLIENT) {
            handleError(IllegalStateException("socket closed by server, this shouldn't happen"))
        }
    }

    /**
     * Invoked when a web socket has been closed due to an error reading from or writing to the
     * network. Both outgoing and incoming messages may have been lost. No further calls to this
     * listener will be made.
     */
    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        logger.d { "[onFailure] t: $t, response: $response" }
        handleError(t)
    }

    internal fun sendHealthCheck() {
        logger.d { "sending health check" }

        val healthCheckRequest = HealthCheckRequest()
        socket?.send(healthCheckRequest.encodeByteString())
    }

    private val healthMonitor = HealthMonitor(
        object : HealthMonitor.HealthCallback {
            override suspend fun reconnect() {
                logger.i { "health monitor triggered a reconnect" }

                val state = connectionState.value
                if (state is SocketState.DisconnectedTemporarily) {
                    this@PersistentSocket.reconnect()
                }
            }

            override fun check() {
                val state = connectionState.value

                logger.d { "health monitor ping. Socket state: $state" }

                (state as? SocketState.Connected)?.let {
                    sendHealthCheck()
                }
            }
        },
        scope,
    )

    internal companion object {
        internal const val CODE_CLOSE_SOCKET_FROM_CLIENT = 1000
    }
}
