package io.getstream.video.android.client.api.listeners

import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.video.android.client.api.state.StreamVideoClientState

/**
 * Listener for the client state.
 */
public interface StreamVideoClientListener : StreamVideoEventListener, StreamErrorListener {

    /**
     * Called when the state changes.
     *
     * @param state The new state.
     */
    public fun onState(state: StreamVideoClientState) {}


    /**
     * Called when a new event is received.
     *
     * @param event The event received.
     */
    override fun onEvent(event: VideoEvent) {}

    /**
     * Called when an error occurs.
     *
     * @param error The error.
     */
    override fun onError(error: Throwable) {}
}