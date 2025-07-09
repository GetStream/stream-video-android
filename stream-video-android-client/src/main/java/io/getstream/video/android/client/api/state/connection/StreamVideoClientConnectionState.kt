package io.getstream.video.android.client.api.state.connection

import io.getstream.android.video.generated.models.APIError
import io.getstream.video.android.client.model.StreamConnectedUser

/**
 * The connection state of the client.
 */
public sealed class StreamVideoClientConnectionState {

    /**
     * The client is connected.
     *
     * @param user The connected user.
     */
    public data class Connected(
        public val user: StreamConnectedUser,
        public val connectionId: String
    ) : StreamVideoClientConnectionState()

    /**
     * The client is disconnected.
     * Some API errors can be retried before failing the connection process
     *
     * @param error The error that caused the disconnection or null if it was intentional.
     * @param apiError The API error that caused the disconnection or null if it was intentional.
     *
     * @see StreamConnectionRetryConfig
     */
    public data class Disconnected(val error: Throwable? = null, val apiError: APIError? = null) :
        StreamVideoClientConnectionState()

    /**
     * Execute the block if the client is connected.
     *
     * @param block The function to apply when the client is connected.
     * @return The connection state.
     */
    public inline fun onConnected(block: (Connected) -> Unit): StreamVideoClientConnectionState {
        if (this is Connected) {
            block(this)
        }
        return this
    }

    /**
     * Execute the block if the client is disconnected.
     *
     * @param block The function to apply when the client is disconnected.
     * @return The connection state.
     */
    public inline fun onDisconnected(block: (Disconnected) -> Unit): StreamVideoClientConnectionState {
        if (this is Disconnected) {
            block(this)
        }
        return this
    }

    /**
     * Fold the connection state.
     *
     * @param connected The function to apply when the client is connected.
     * @param disconnected The function to apply when the client is disconnected.
     * @return The result of the function.
     */
    public inline fun <T> foldCatching(
        connected: (Connected) -> T,
        disconnected: (Disconnected) -> T,
    ): Result<T> = runCatching {
        when (this) {
            is Connected -> connected(this)
            is Disconnected -> disconnected(this)
        }
    }
}




