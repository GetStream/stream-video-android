package io.getstream.video.android.client.internal.client.state

import io.getstream.video.android.client.api.state.connection.StreamVideoClientConnectionState

internal data class StreamClientStateImpl(
    private var connectionState: StreamVideoClientConnectionState = StreamVideoClientConnectionState.Disconnected(
        null
    )
) :
    MutableStreamVideoClientState {
    override fun setConnectionState(state: StreamVideoClientConnectionState): Result<StreamVideoClientConnectionState> {
        connectionState = state
        return Result.success(connectionState)
    }

    override fun getConnectionState(): StreamVideoClientConnectionState = connectionState
}