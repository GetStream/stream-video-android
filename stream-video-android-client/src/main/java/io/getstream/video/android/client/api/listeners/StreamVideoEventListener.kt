package io.getstream.video.android.client.api.listeners

import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.video.android.client.api.subscribe.StreamSubscriber

public interface StreamVideoEventListener : StreamSubscriber {

    /**
     * Called when a new event is received.
     *
     * @param event The event received.
     */
    public fun onEvent(event: VideoEvent)
}