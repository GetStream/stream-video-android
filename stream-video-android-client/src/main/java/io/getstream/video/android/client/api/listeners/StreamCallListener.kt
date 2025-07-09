package io.getstream.video.android.client.api.listeners

import io.getstream.video.android.client.api.state.state.StreamCallState

public interface StreamCallListener : StreamVideoEventListener {

    /**
     * Called when the state changes.
     *
     * @param state The new state.
     */
    public fun onState(state: StreamCallState)
}