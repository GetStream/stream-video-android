/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.model

import io.getstream.video.android.socket.VideoSocket
import io.livekit.android.room.Room

public data class VideoRoom(
    public val value: Room,
    private val socket: VideoSocket // TODO figure this out
) {
    public val localParticipant: LocalParticipant
        get() = LocalParticipant(value.localParticipant)

    public fun updateCallState(
        callId: String,
        callType: String,
        audioEnabled: Boolean,
        videoEnabled: Boolean
    ) {
        socket.updateCallState(
            callId = callId,
            callType = callType,
            audioEnabled = audioEnabled,
            videoEnabled = videoEnabled
        )
    }

    public fun updateAudioState(isEnabled: Boolean) {
    }

    public suspend fun connect(url: String, token: String) {
        value.connect(url, token)
    }

    public fun disconnect() {
        value.disconnect()
    }
}
