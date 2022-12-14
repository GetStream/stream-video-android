package io.getstream.video.android.compose.ui.components.participants

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.getstream.video.android.R
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.participants.internal.ParticipantsList
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.model.ScreenSharingSession

@Composable
public fun ScreenSharingCallParticipantsContent(
    call: Call,
    session: ScreenSharingSession,
    participants: List<CallParticipantState>,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onRender: (View) -> Unit = {}
) {
    val sharingParticipant = session.participant.name.ifEmpty { session.participant.id }
    Column(
        modifier = modifier
            .background(VideoTheme.colors.screenSharingBackground)
            .padding(paddingValues)
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = stringResource(id = R.string.stream_screen_sharing_title, sharingParticipant),
            color = VideoTheme.colors.textHighEmphasis,
            style = VideoTheme.typography.title3Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        VideoRenderer(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ScreenShareAspectRatio, false),
            call = call,
            videoTrack = session.track,
            onRender = onRender
        )

        Spacer(modifier = Modifier.height(32.dp))

        ParticipantsList(
            call = call,
            participants = participants
        )
    }
}

private const val ScreenShareAspectRatio: Float = 16f / 9f