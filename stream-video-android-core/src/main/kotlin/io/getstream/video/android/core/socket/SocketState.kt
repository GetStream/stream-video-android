package io.getstream.video.android.core.socket

import io.getstream.result.Error
import io.getstream.video.android.core.events.VideoEvent

public sealed class SocketState {
    /** We haven't started to connect yet */
    object NotConnected : SocketState() {
        override fun toString(): String = "Not Connected"
    }

    /** Connection is in progress */
    object Connecting : SocketState() {
        override fun toString(): String = "Connecting"
    }

    /** We are connected, the most common state */
    data class Connected(val event: VideoEvent) : SocketState()

    /** There is no internet available */
    object NetworkDisconnected : SocketState() {
        override fun toString(): String = "NetworkDisconnected"
    }

    /** A temporary error broken the connection, socket will retry */
    data class DisconnectedTemporarily(val error: Error.NetworkError?) : SocketState()
    /** A permanent error broken the connection, socket will not retry */
    data class DisconnectedPermanently(val error: Error.NetworkError?) : SocketState()

    /** You called socket.disconnect(), socket is disconnected */
    object DisconnectedByRequest : SocketState() {
        override fun toString(): String = "DisconnectedByRequest"
    }
}