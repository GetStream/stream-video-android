package io.getstream.video.android.core.socket.common

import io.getstream.result.Error
import io.getstream.video.android.core.events.SfuDataEvent
import org.openapitools.client.models.VideoEvent
import stream.video.sfu.models.WebsocketReconnectStrategy

public sealed class StreamWebSocketEvent {
    data class Error(val streamError: io.getstream.result.Error, val reconnectStrategy: WebsocketReconnectStrategy? = null) : StreamWebSocketEvent()
    data class VideoMessage(val videoEvent: VideoEvent) : StreamWebSocketEvent()
    data class SfuMessage(val sfuEvent: SfuDataEvent) : StreamWebSocketEvent()
}