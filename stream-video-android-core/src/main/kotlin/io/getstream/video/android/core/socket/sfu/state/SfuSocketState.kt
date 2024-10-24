package io.getstream.video.android.core.socket.sfu.state

import io.getstream.result.Error
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.socket.common.ConnectionConf
import stream.video.sfu.models.WebsocketReconnectStrategy

public sealed class SfuSocketState {

    /**
     * State of socket when connection need to be reestablished.
     */
    data class RestartConnection(val reason: RestartReason, val reconnectStrategy: WebsocketReconnectStrategy) : SfuSocketState()

    /**
     * State of socket when connection is being establishing.
     */
    data class Connecting(
        val connectionConf: ConnectionConf.SfuConnectionConf,
        val connectionType: WebsocketReconnectStrategy = WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_UNSPECIFIED,
    ) : SfuSocketState()

    /**
     * State of socket when the connection is established.
     */
    data class Connected(val event: JoinCallResponseEvent) : SfuSocketState()

    /**
     * State of socket when connection is being disconnected.
     */
    sealed class Disconnected : SfuSocketState() {

        object Rejoin: Disconnected() { override fun toString() = "Disconnected.Rejoin" }

        /**
         * State of socket when is stopped.
         */
        object Stopped : Disconnected() { override fun toString() = "Disconnected.Stopped" }

        /**
         * State of socket when network is disconnected.
         */
        object NetworkDisconnected : Disconnected() { override fun toString() = "Disconnected.Network" }

        /**
         * State of socket when HealthEvent is lost.
         */
        object WebSocketEventLost : Disconnected() { override fun toString() = "Disconnected.InactiveWS" }

        /**
         * State of socket when is disconnected by customer request.
         */
        object DisconnectedByRequest : Disconnected() { override fun toString() = "Disconnected.ByRequest" }

        /**
         * State of socket when a [Error] happens.
         */
        data class DisconnectedTemporarily(val error: Error.NetworkError, val reconnectStrategy: WebsocketReconnectStrategy) : Disconnected()

        /**
         * State of socket when a connection is permanently disconnected.
         */
        data class DisconnectedPermanently(val error: Error.NetworkError) : Disconnected()
    }
}