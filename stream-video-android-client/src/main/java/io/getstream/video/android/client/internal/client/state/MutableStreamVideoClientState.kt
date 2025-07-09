package io.getstream.video.android.client.internal.client.state

import io.getstream.video.android.client.api.state.StreamVideoClientState
import io.getstream.video.android.client.api.state.connection.StreamVideoClientConnectionState
import io.getstream.video.android.client.model.StreamConnectedUser

internal interface MutableStreamVideoClientState : StreamVideoClientState {

    /**
     * Set the current user.
     *
     * @param user The current user.
     */
    fun setConnectionState(state: StreamVideoClientConnectionState) : Result<StreamVideoClientConnectionState>
}