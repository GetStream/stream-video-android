package io.getstream.video.android.core.socket.common

import io.getstream.video.android.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SocketActions<EventIn, EventOut, Error, State, Token, ConnectData> {
    companion object {
        internal const val DEFAULT_SOCKET_TIMEOUT: Long = 10000L
    }

    /**
     * State of the socket as [StateFlow]
     */
    public fun state(): StateFlow<State>

    /**
     * Socket events as [Flow]
     */
    public fun events(): Flow<EventOut>

    /**
     * Socket errors as [Flow]
     */
    public fun errors(): Flow<Error>

    /**
     * Send raw data to the socket. If you already have a parsed event that can be sent.
     */
    fun sendData(data: String)

    /**
     * Send event to the socket.
     */
    public suspend fun sendEvent(event: EventIn): Boolean

    /**
     * Connect the user.
     */
    public suspend fun connect(connectData: ConnectData)

    /**
     * Reconnect the user to the socket.
     */
    public suspend fun reconnect(data: ConnectData, force: Boolean = false)

    /**
     * Disconnect the socket.
     */
    public suspend fun disconnect()

    /**
     * Update the token from the outside.
     */
    fun updateToken(token: Token)

    /**
     * Get the connection id.
     */
    fun connectionId(): StateFlow<String?>

    /**
     * When connected to the socket.
     */
    fun whenConnected(
        connectionTimeout: Long = DEFAULT_SOCKET_TIMEOUT,
        connected: suspend (connectionId: String) -> Unit
    )
}
