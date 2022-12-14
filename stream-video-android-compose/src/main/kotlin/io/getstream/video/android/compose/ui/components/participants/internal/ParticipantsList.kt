package io.getstream.video.android.compose.ui.components.participants.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.participants.CallParticipant
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallParticipantState

@Composable
internal fun ParticipantsList(
    call: Call,
    participants: List<CallParticipantState>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = {
            items(participants) { participant ->
                ParticipantListItem(call, participant)
            }
        }
    )
}

@Composable
private fun ParticipantListItem(call: Call, participant: CallParticipantState) {
    val track = participant.videoTrack

    if (track != null) {
        CallParticipant(
            modifier = Modifier
                .size(VideoTheme.dimens.screenShareParticipantItemSize)
                .clip(RoundedCornerShape(16.dp)),
            call = call,
            participant = participant,
            labelPosition = Alignment.BottomStart
        )
    }
}
