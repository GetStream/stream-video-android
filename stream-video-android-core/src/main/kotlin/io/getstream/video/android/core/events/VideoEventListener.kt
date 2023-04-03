package io.getstream.video.android.core.events

public fun interface VideoEventListener<EventT : VideoEvent> {
    public fun onEvent(event: EventT)
}