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

import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.track.LocalTrackPublication
import io.livekit.android.room.track.Track

public data class LocalParticipant(
    private val localValue: LocalParticipant
) : Participant {
    override val value: io.livekit.android.room.participant.Participant
        get() = localValue

    public val isAudioEnabled: Boolean
        get() = localValue.isMicrophoneEnabled()

    public val isVideoEnabled: Boolean
        get() = localValue.isCameraEnabled()

    public suspend fun setMicrophoneEnabled(isEnabled: Boolean) {
        localValue.setMicrophoneEnabled(isEnabled)
    }

    public suspend fun setCameraEnabled(isEnabled: Boolean) {
        localValue.setCameraEnabled(isEnabled)
    }

    public fun getTrackPublication(camera: Track.Source): LocalTrackPublication? {
        return localValue.getTrackPublication(camera)
    }
}
