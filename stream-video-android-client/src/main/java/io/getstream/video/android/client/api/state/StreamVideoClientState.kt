package io.getstream.video.android.client.api.state

import io.getstream.video.android.client.api.state.connection.StreamVideoClientConnectionState

public interface StreamVideoClientState {

    /**
     * Get the connection state.
     *
     * @return The connection state.
     */
    public fun getConnectionState(): StreamVideoClientConnectionState
}

