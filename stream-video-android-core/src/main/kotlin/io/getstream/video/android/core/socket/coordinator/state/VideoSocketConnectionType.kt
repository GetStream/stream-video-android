package io.getstream.video.android.core.socket.coordinator.state

enum class VideoSocketConnectionType {
    INITIAL_CONNECTION,
    AUTOMATIC_RECONNECTION,
    FORCE_RECONNECTION,
}