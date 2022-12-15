package io.getstream.video.android.compose.ui.components.participants.internal

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.getstream.video.android.R
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.model.ScreenSharingSession

/**
 * Represents the landscape screen sharing content.
 *
 * @param call The call containing state.
 * @param session Screen sharing session to render.
 * @param participants List of participants to render under the screen share track.
 * @param paddingValues Padding values from the parent.
 * @param modifier Modifier for styling.
 * @param onRender Handler when the video renders.
 */
@Composable
public fun LandscapeScreenSharingContent(
    call: Call,
    session: ScreenSharingSession,
    participants: List<CallParticipantState>,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean,
    onRender: (View) -> Unit,
    onCallAction: (CallAction) -> Unit,
) {
    val sharingParticipant = session.participant

    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxWidth(0.5f)
                .background(VideoTheme.colors.screenSharingBackground)
                .padding(paddingValues)
        ) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = stringResource(
                    id = R.string.stream_screen_sharing_title,
                    sharingParticipant.name.ifEmpty { sharingParticipant.id }
                ),
                color = VideoTheme.colors.textHighEmphasis,
                style = VideoTheme.typography.title3Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            ScreenShareContent(
                modifier = Modifier.fillMaxSize(),
                call = call,
                session = session,
                onRender = onRender,
                isFullscreen = isFullscreen,
                onCallAction = onCallAction
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        ParticipantsColumn(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f),
                call = call,
            participants = participants
        )
    }
}