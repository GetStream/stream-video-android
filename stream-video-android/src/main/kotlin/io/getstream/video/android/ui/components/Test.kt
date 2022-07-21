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