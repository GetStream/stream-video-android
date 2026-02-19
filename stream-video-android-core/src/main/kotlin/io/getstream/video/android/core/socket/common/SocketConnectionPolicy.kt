/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.socket.common

import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.socket.coordinator.state.VideoSocketState
import kotlinx.coroutines.flow.StateFlow

interface SocketConnectionPolicy {
    fun shouldConnect(): Boolean
    fun shouldDisconnect(): Boolean
}

class CallAwareConnectionPolicy(private val streamVideo: StateFlow<StreamVideo?>) : SocketConnectionPolicy {
    override fun shouldConnect(): Boolean = true

    override fun shouldDisconnect(): Boolean {
        val streamVideo = streamVideo.value

        return streamVideo == null || !streamVideo.state.hasActiveOrRingingCall()
    }
}

class SocketStateConnectionPolicy(private val currentStateFlow: StateFlow<VideoSocketState>) : SocketConnectionPolicy {
    override fun shouldConnect(): Boolean {
        val state = currentStateFlow.value

        return state is VideoSocketState.Disconnected.DisconnectedPermanently ||
            state is VideoSocketState.Disconnected.DisconnectedTemporarily ||
            state is VideoSocketState.Disconnected.NetworkDisconnected ||
            state is VideoSocketState.Disconnected.DisconnectedByRequest ||
            state is VideoSocketState.Disconnected.Stopped
    }

    override fun shouldDisconnect(): Boolean {
        val state = currentStateFlow.value

        return state is VideoSocketState.Connected ||
            state is VideoSocketState.Connecting ||
            state is VideoSocketState.Disconnected.DisconnectedTemporarily
    }
}
