package io.getstream.video.android.compose.ui.components.participants

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallParticipantState

@Composable
public fun FloatingParticipantItem(
    call: Call,
    localParticipant: CallParticipantState,
    modifier: Modifier = Modifier
) {
    val track = localParticipant.videoTrack

    if (track != null) {

        Card(
            elevation = 8.dp,
            modifier = modifier.padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            VideoRenderer(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                call = call,
                videoTrack = track
            )
        }
    }
}