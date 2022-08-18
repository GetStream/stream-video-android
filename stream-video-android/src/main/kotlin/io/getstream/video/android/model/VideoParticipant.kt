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

import io.getstream.video.android.dispatchers.DispatcherProvider
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.track.LocalTrackPublication
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.TrackPublication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import stream.video.User

public data class VideoParticipant(
    private val participant: io.livekit.android.room.participant.Participant? = null,
    private val streamParticipant: stream.video.Participant? = null
) {

    private val coroutineScope = CoroutineScope(context = DispatcherProvider.IO)

    public val value: io.livekit.android.room.participant.Participant?
        get() = participant

    public val videoTracks: List<Pair<TrackPublication, Track?>>
        get() = value?.videoTracks ?: emptyList()

    public val sid: String?
        get() = value?.sid

    public val user: User?
        get() = streamParticipant?.user

    public fun setCameraEnabled(isEnabled: Boolean) {
        (participant as? LocalParticipant)?.let { user ->
            coroutineScope.launch {
                user.setCameraEnabled(isEnabled)
            }
        }
    }

    public fun setMicrophoneEnabled(isEnabled: Boolean) {
        (participant as? LocalParticipant)?.let { user ->
            coroutineScope.launch {
                user.setMicrophoneEnabled(isEnabled)
            }
        }
    }

    public fun getTrackPublication(camera: Track.Source): LocalTrackPublication? {
        return (participant as? LocalParticipant)?.getTrackPublication(camera)
    }

    public fun isLocalParticipant(): Boolean = participant is LocalParticipant

    public fun isRemoteParticipant(): Boolean = participant is RemoteParticipant
}
