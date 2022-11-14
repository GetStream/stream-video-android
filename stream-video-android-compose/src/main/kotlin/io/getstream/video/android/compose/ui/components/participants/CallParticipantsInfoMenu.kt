package io.getstream.video.android.compose.ui.components.participants

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.viewmodel.CallViewModel


@Composable
public fun CallParticipantsInfoMenu( // TODO - rename, expose, build better UI and flow
    callViewModel: CallViewModel,
    participantsState: List<CallParticipantState>
) {
    Box(
        modifier = Modifier
            .background(color = Color.LightGray.copy(alpha = 0.7f))
            .fillMaxSize()
            .clickable { callViewModel.dismissOptions() }
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(16.dp)
                .heightIn(max = 200.dp)
                .widthIn(max = 200.dp)
                .align(Alignment.TopEnd)
                .background(color = Color.White, shape = RoundedCornerShape(16.dp)),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            items(participantsState) {
                ParticipantInfoItem(it)
            }
        }
    }
}

@Composable
private fun ParticipantInfoItem(participant: CallParticipantState) {
    Row(
        modifier = Modifier.wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        val isAudioEnabled = participant.hasAudio
        Icon(
            imageVector = if (isAudioEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
            contentDescription = "User Audio"
        )

        val isVideoEnabled = participant.hasVideo
        Icon(
            imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
            contentDescription = "User Video"
        )

        val userName = when {
            participant.name.isNotBlank() -> participant.name
            participant.id.isNotBlank() -> participant.id
            else -> "Unknown"
        }

        Text(
            text = userName,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
