package io.getstream.video.android.engine.adapter

import io.getstream.video.android.engine.StreamCallEngine
import io.getstream.video.android.events.VideoEvent
import io.getstream.video.android.socket.SocketListener

internal class CoordinatorSocketListenerAdapter(
    private val engine: StreamCallEngine
) : SocketListener {

    override fun onEvent(event: VideoEvent) {
        engine.onCoordinatorEvent(event)
    }

}