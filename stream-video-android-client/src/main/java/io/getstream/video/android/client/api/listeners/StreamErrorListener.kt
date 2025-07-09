package io.getstream.video.android.client.api.listeners

import io.getstream.video.android.client.api.subscribe.StreamSubscriber

/**
 * Listener for errors.
 */
public interface StreamErrorListener : StreamSubscriber {

    /**
     * Called when an error occurs.
     *
     * @param error The error.
     */
    public fun onError(error: Throwable)
}