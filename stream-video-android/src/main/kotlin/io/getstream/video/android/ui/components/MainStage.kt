package io.getstream.video.android.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack

@Composable
public fun MainStage(
    room: Room,
    track: VideoTrack
) {
    VideoItem(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        room = room,
        videoTrack = track
    )
}