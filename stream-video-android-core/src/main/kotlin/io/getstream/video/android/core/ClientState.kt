/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.core

import io.getstream.video.android.core.errors.VideoError
import io.getstream.video.android.core.events.CallCreatedEvent
import io.getstream.video.android.core.events.ConnectedEvent
import io.getstream.video.android.core.events.VideoEvent
import io.getstream.video.android.core.model.Call
import io.getstream.video.android.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class ConnectionState() {
    // TODO:ConnectionState.Connected is nicer than ConnectionState.Connected()
    object PreConnect : ConnectionState()
    object Loading : ConnectionState()
    object Connected : ConnectionState()
    object Reconnecting : ConnectionState()
    object Disconnected : ConnectionState()
    class Failed(error: VideoError) : ConnectionState()
}

class ClientState(client: StreamVideo) {
    internal val clientImpl = client as StreamVideoImpl

    /**
     * Handles the events for the client state.
     * Most event logic happens in the Call instead of the client
     */

    fun handleEvent(event: VideoEvent) {
        val isConnectedEvent = event is ConnectedEvent
        // mark connected
        if (event is ConnectedEvent) {
            val new = ConnectionState.Connected

            _connection.value = ConnectionState.Connected
        } else if (event is CallCreatedEvent) {
            // what's the right thing to do here?
            // if it's ringing we add it

            // get or create the call and update it
            val (type, id) = event.callCid.split(":")
            val call = clientImpl.call(type, id)
            call.state.updateFromEvent(event)

            if (event.ringing) {
                _incomingCall.value = call
            }
        }
    }

    /**
     * Current user object
     */
    private val _currentUser: MutableStateFlow<User?> = MutableStateFlow(null)
    public val currentUser: StateFlow<User?> = _currentUser

    /**
     * connectionState shows if we've established a connection with the coordinator
     */
    private val _connection: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.PreConnect)
    public val connection: StateFlow<ConnectionState> = _connection

    /**
     * Incoming call. True when we receive an event or notification with an incoming call
     */
    private val _incomingCall: MutableStateFlow<Call2?> = MutableStateFlow(null)
    public val incomingCall: StateFlow<Call2?> = _incomingCall

    /**
     * Active call. The currently active call
     */
    private val _activeCall: MutableStateFlow<Call2?> = MutableStateFlow(null)
    public val activeCall: StateFlow<Call2?> = _activeCall
}
