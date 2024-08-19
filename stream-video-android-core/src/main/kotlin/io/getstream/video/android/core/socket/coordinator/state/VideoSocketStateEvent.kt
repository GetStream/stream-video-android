package io.getstream.video.android.core.socket.coordinator.state

import io.getstream.result.Error
import io.getstream.video.android.core.socket.common.ConnectionConf
import org.openapitools.client.models.ConnectedEvent

public sealed class VideoSocketStateEvent {

    /**
     * Event to start a new connection.
     */
    data class Connect(
        val connectionConf: ConnectionConf,
        val connectionType: VideoSocketConnectionType,
    ) : VideoSocketStateEvent()

    /**
     * Event to notify the connection was established.
     */
    data class ConnectionEstablished(val connectedEvent: ConnectedEvent) : VideoSocketStateEvent()

    /**
     * Event to notify some WebSocket event has been lost.
     */
    object WebSocketEventLost : VideoSocketStateEvent() { override fun toString() = "WebSocketEventLost" }

    /**
     * Event to notify Network is not available.
     */
    object NetworkNotAvailable : VideoSocketStateEvent() { override fun toString() = "NetworkNotAvailable" }

    /**
     * Event to notify Network is available.
     */
    object NetworkAvailable : VideoSocketStateEvent() { override fun toString() = "NetworkAvailable" }

    /**
     * Event to notify an Unrecoverable Error happened on the WebSocket connection.
     */
    data class UnrecoverableError(val error: Error.NetworkError) : VideoSocketStateEvent()

    /**
     * Event to notify a network Error happened on the WebSocket connection.
     */
    data class NetworkError(val error: Error.NetworkError) : VideoSocketStateEvent()

    /**
     * Event to stop WebSocket connection required by user.
     */
    object RequiredDisconnection : VideoSocketStateEvent() { override fun toString() = "RequiredDisconnection" }

    /**
     * Event to stop WebSocket connection.
     */
    object Stop : VideoSocketStateEvent() { override fun toString() = "Stop" }

    /**
     * Event to resume WebSocket connection.
     */
    object Resume : VideoSocketStateEvent() { override fun toString() = "Resume" }
}