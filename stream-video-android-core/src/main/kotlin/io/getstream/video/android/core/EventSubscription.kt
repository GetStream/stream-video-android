package io.getstream.video.android.core

import io.getstream.video.android.core.events.VideoEventListener
import org.openapitools.client.models.VideoEvent

class EventSubscription(
    public val listener: VideoEventListener<VideoEvent>,
    public val filter: ((VideoEvent) -> Boolean)? = null,
) {
    var isDisposed: Boolean = false

    fun dispose() {
        isDisposed = true
    }
}