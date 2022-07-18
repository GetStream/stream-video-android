package io.getstream.video.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.livekit.android.room.Room
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.track.VideoTrack

@Composable
public fun ParticipantsList(
    room: Room,
    participants: List<Participant>
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(participants) { participant ->
            ParticipantItem(
                room,
                participant
            )
        }
    }
}

@Composable
public fun ParticipantItem(room: Room, participant: Participant) {
    val track = participant.videoTracks.firstOrNull()?.second as? VideoTrack ?: return

    VideoItem(room = room, videoTrack = track)
}