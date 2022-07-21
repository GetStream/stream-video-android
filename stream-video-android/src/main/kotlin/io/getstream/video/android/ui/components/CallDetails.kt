package io.getstream.video.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.livekit.android.room.Room
import io.livekit.android.room.participant.Participant

@Composable
public fun CallDetails(
    room: Room,
    participants: List<Participant>,
    modifier: Modifier = Modifier
) {

    Column(modifier = modifier) {
        ParticipantsList(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            room = room,
            participants = participants
        )

        Row(modifier = Modifier.fillMaxWidth()) {

        }
    }
}