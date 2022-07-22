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

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.livekit.android.compose.VideoRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.track.VideoTrack
import io.livekit.android.util.flow

@Composable
public fun Content(
    room: Room
) {
    val remoteParticipants by room::remoteParticipants.flow.collectAsState(emptyMap())
    val remoteParticipantsList = remoteParticipants.values.toList()
    LazyRow {
        items(
            count = remoteParticipantsList.size,
            key = { index -> remoteParticipantsList[index].sid }
        ) { index ->
            ParticipantItem2(room = room, participant = remoteParticipantsList[index])
        }
    }
}

@Composable
public fun ParticipantItem2(
    room: Room,
    participant: Participant,
) {
    val videoTracks by participant::videoTracks.flow.collectAsState(emptyList())
    val subscribedTrack = videoTracks.firstOrNull { (pub) -> pub.subscribed } ?: return
    val videoTrack = subscribedTrack.second as? VideoTrack ?: return

    VideoRenderer(
        room = room,
        videoTrack = videoTrack,
    )
}
