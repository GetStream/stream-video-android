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

package io.getstream.video.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.livekit.android.compose.VideoRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.track.VideoTrack

@Composable
public fun ParticipantsList(
    room: Room,
    participants: List<Participant>,
    modifier: Modifier = Modifier,
    primarySpeaker: Participant?
) {
    val secondarySpeakers = participants.filter { it.sid != primarySpeaker?.sid }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(secondarySpeakers) { participant ->
            ParticipantItem(
                room,
                participant
            )
        }
    }
}

@Composable
public fun ParticipantItem(room: Room, participant: Participant) {
    val track = participant.videoTracks.firstOrNull()?.second as? VideoTrack

    if (track != null) {
        VideoRenderer(
            modifier = Modifier.size(150.dp),
            room = room,
            videoTrack = track
        )
    }
}
