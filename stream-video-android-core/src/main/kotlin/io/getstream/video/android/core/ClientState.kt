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
import io.getstream.video.android.core.events.VideoEvent
import io.getstream.video.android.core.model.Call
import io.getstream.video.android.core.model.User
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

    // TODO: Hide mutability
    public val currentUser: MutableStateFlow<User?> = MutableStateFlow(null)


    /**
     * connectionState shows if we've established a connection with the coordinator
     */
    // TODO: Hide mutability
    public val connection: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.PreConnect())

    /**
     * Incoming call. True when we receive an event or notification with an incoming call
     */
    // TODO: Should be a call object or similar. Not sure what's easiest
    public val incomingCall: MutableStateFlow<Call?> = MutableStateFlow(null)

    /**
     * Active call. The currently active call
     */
    public val activeCall: MutableStateFlow<Call2?> = MutableStateFlow(null)
}
