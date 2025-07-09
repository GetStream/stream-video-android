package io.getstream.video.android.client.internal.socket.common

import io.getstream.log.TaggedLogger
import io.getstream.video.android.client.api.subscribe.StreamSubscription
import io.getstream.video.android.client.internal.common.StreamSubscriptionManager
import io.getstream.video.android.client.internal.config.StreamSocketConfig
import io.getstream.video.android.client.internal.log.provideLogger
import io.getstream.video.android.client.internal.socket.common.factory.StreamWebSocketFactory
import io.getstream.video.android.client.internal.socket.common.listeners.StreamWebSocketListener
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.IOException

internal interface StreamWebSocket<T : StreamWebSocketListener> : StreamSubscriptionManager<T> {

    companion object {
        /**
         * Close code for the socket.
         */
        internal const val CLOSE_SOCKET_CODE = 1000

        /**
         * Close reason for the socket.
         */
        internal const val CLOSE_SOCKET_REASON = "Closed by client"
    }

    fun open(config: StreamSocketConfig): Result<Unit>

    /**
     * Close the socket.
     */
    fun close(): Result<Unit>

    /**
     * Send raw data to the socket.
     *
     * @param data The data to be sent.
     */
    fun send(data: ByteArray): Result<ByteArray>
}

/**
 * Implementation of the [StreamWebSocket] interface.
 */
internal open class StreamWebSocketImpl<T : StreamWebSocketListener>(
    private val logger: TaggedLogger = provideLogger(tag = "StreamWebSocket"),
    private val socketFactory: StreamWebSocketFactory,
    private val subscriptionManager: StreamSubscriptionManager<T>
) : WebSocketListener(), StreamWebSocket<T> {

    private lateinit var socket: WebSocket

    override fun open(config: StreamSocketConfig): Result<Unit> = runCatching {
        socket = socketFactory.createSocket(config, this).onFailure {
            logger.e { "[open] SocketFactory failed to create socket. ${it.message}" }
        }.getOrThrow()
    }

    override fun close(): Result<Unit> = catchingWithSocket {
        logger.d { "[close] Closing socket" }
        socket.close(StreamWebSocket.CLOSE_SOCKET_CODE, StreamWebSocket.CLOSE_SOCKET_REASON)
    }

    override fun send(data: ByteArray): Result<ByteArray> = catchingWithSocket {
        logger.v { "[send] Sending data: $data" }
        if (data.isNotEmpty()) {
            val result = socket.send(data.toByteString(0, data.size))
            if (!result) {
                val message = "[send] socket.send() returned false"
                logger.e { message }
                throw IOException(message)
            }
            data
        } else {
            logger.e { "[send] Empty data!" }
            throw IllegalStateException("Empty raw data!")
        }
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        logger.d { "[onOpen] Socket is open" }
        forEach {
            it.onOpen(response)
        }
        super.onOpen(webSocket, response)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        logger.v { "[onMessage] Socket message: $bytes" }
        forEach {
            it.onMessage(bytes)
        }
        super.onMessage(webSocket, bytes)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        logger.v { "[onMessage] Socket message: $text" }
        forEach {
            it.onMessage(text)
        }
        super.onMessage(webSocket, text)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        logger.e(t) { "[onFailure] Socket failure" }
        forEach {
            it.onFailure(t, response)
        }
        super.onFailure(webSocket, t, response)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        logger.d { "[onClosed] Socket closed. Code: $code, Reason: $reason" }
        forEach {
            it.onClosed(code, reason)
        }
        super.onClosed(webSocket, code, reason)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        logger.d { "[onClosing] Socket closing. Code: $code, Reason: $reason" }
        forEach {
            it.onClosing(code, reason)
        }
        super.onClosing(webSocket, code, reason)
    }

    override fun subscribe(listener: T): Result<StreamSubscription> =
        subscriptionManager.subscribe(listener)

    override fun clear(): Result<Unit> = subscriptionManager.clear()

    override fun forEach(block: (T) -> Unit): Result<Unit> =
        subscriptionManager.forEach(block)

    private inline fun <V> catchingWithSocket(block: (WebSocket) -> V) = runCatching {
        if (::socket.isInitialized) {
            block(socket)
        } else {
            val message =
                "[withSocket] The socket tried to use the internal web socket, but its not initialized. Call `open()` first."
            logger.e { message }
            throw IllegalStateException(message)
        }
    }
}