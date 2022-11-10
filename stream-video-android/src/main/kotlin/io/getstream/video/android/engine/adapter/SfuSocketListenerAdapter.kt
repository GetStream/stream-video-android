package io.getstream.video.android.engine.adapter

import io.getstream.video.android.call.signal.socket.SignalSocketListener
import io.getstream.video.android.engine.StreamCallEngine
import io.getstream.video.android.events.SfuDataEvent

internal class SfuSocketListenerAdapter(
    private val engine: StreamCallEngine
) : SignalSocketListener {

    override fun onEvent(event: SfuDataEvent) {
        engine.onSfuEvent(event)
    }

}