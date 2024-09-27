package io.getstream.video.android.core.socket.sfu.state

import io.getstream.result.Error
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.socket.common.ConnectionConf
import stream.video.sfu.models.WebsocketReconnectStrategy

public sealed class SfuSocketStateEvent {

    /**
     * Event to start a new connection.
     */
    data class Connect(
        val connectionConf: ConnectionConf.SfuConnectionConf,
        val connectionType: WebsocketReconnectStrategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_UNSPECIFIED,
    ) : SfuSocketStateEvent()

    /**
     * Event to notify the connection was established.
     */
    data class ConnectionEstablished(val connectedEvent: JoinCallResponseEvent) : SfuSocketStateEvent()

    /**
     * Event to notify some WebSocket event has been lost.
     */
    object WebSocketEventLost : SfuSocketStateEvent() { override fun toString() = "WebSocketEventLost" }

    /**
     * Event to notify Network is not available.
     */
    object NetworkNotAvailable : SfuSocketStateEvent() { override fun toString() = "NetworkNotAvailable" }

    /**
     * Event to notify Network is available.
     */
    object NetworkAvailable : SfuSocketStateEvent() { override fun toString() = "NetworkAvailable" }

    /**
     * Event to notify an Unrecoverable Error happened on the WebSocket connection.
     */
    data class UnrecoverableError(val error: Error.NetworkError) : SfuSocketStateEvent()

    /**
     * Event to notify a network Error happened on the WebSocket connection.
     */
    data class NetworkError(val error: Error.NetworkError, val reconnectStrategy: WebsocketReconnectStrategy) : SfuSocketStateEvent()

    /**
     * Event to stop WebSocket connection required by user.
     */
    object RequiredDisconnection : SfuSocketStateEvent() { override fun toString() = "RequiredDisconnection" }

    /**
     * Event to stop WebSocket connection.
     */
    object Stop : SfuSocketStateEvent() { override fun toString() = "Stop" }

    /**
     * Event to resume WebSocket connection.
     */
    object Resume : SfuSocketStateEvent() { override fun toString() = "Resume" }
}