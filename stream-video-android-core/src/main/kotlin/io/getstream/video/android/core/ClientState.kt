package io.getstream.video.android.core

import io.getstream.video.android.core.errors.VideoError
import io.getstream.video.android.core.events.VideoEvent
import io.getstream.video.android.core.model.Call
import kotlinx.coroutines.flow.MutableStateFlow



sealed class ConnectionState {
    class PreConnect : ConnectionState()
    class Loading : ConnectionState()
    class Connected : ConnectionState()
    class Reconnecting : ConnectionState()
    class Failed(error: VideoError) : ConnectionState()
}

class ClientState {
    fun handleEvent(event: VideoEvent) {

    }

    /**
     * connectionState shows if we've established a connection with the coordinator
     */
    // TODO: Hide mutability
    private val connection: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.PreConnect())

    /**
     * Incoming call. True when we receive an event or notification with an incoming call
     */
    // TODO: Should be a call object or similar. Not sure what's easiest
    private val incomingCall: MutableStateFlow<Call?> = MutableStateFlow(null)

    /**
     * Active call. The currently active call
     */
    private val activeCall: MutableStateFlow<Call2?> = MutableStateFlow(null)


}