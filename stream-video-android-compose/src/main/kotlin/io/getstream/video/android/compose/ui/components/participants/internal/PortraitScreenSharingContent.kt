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

package io.getstream.video.android.compose.ui.components.participants.internal

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.getstream.video.android.common.util.mockVideoTrack
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.previews.ParticipantsProvider
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.model.Call
import io.getstream.video.android.core.model.CallParticipantState
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.ui.common.R

/**
 * Represents the portrait screen sharing content.
 *
 * @param call The call containing state.
 * @param session Screen sharing session to render.
 * @param participants List of participants to render under the screen share track.
 * @param paddingValues Padding values from the parent.
 * @param modifier Modifier for styling.
 * @param onRender Handler when the video renders.
 */
@Composable
internal fun PortraitScreenSharingContent(
    call: Call?,
    session: ScreenSharingSession,
    participants: List<CallParticipantState>,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    onRender: (View) -> Unit,
    onCallAction: (CallAction) -> Unit,
) {
    val sharingParticipant = session.participant

    Column(
        modifier = modifier
            .background(VideoTheme.colors.screenSharingBackground)
            .padding(paddingValues)
    ) {
        Text(
            modifier = Modifier.padding(VideoTheme.dimens.screenSharePresenterPadding),
            text = stringResource(
                id = R.string.stream_video_screen_sharing_title,
                sharingParticipant.name.ifEmpty { sharingParticipant.id }
            ),
            color = VideoTheme.colors.textHighEmphasis,
            style = VideoTheme.typography.title3Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(VideoTheme.dimens.screenSharePresenterMargin))

        ScreenShareContent(
            call = call,
            session = session,
            onRender = onRender,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            isFullscreen = false,
            onCallAction = onCallAction
        )

        Spacer(modifier = Modifier.height(VideoTheme.dimens.screenShareParticipantsScreenShareListMargin))

        ParticipantsRow(
            modifier = Modifier.height(VideoTheme.dimens.screenShareParticipantsRowHeight),
            call = call,
            participants = participants
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PortraitScreenSharingContentPreview(
    @PreviewParameter(ParticipantsProvider::class) callParticipants: List<CallParticipantState>
) {
    VideoTheme {
        PortraitScreenSharingContent(
            call = null,
            session = ScreenSharingSession(
                track = callParticipants.first().videoTrack ?: mockVideoTrack,
                participant = callParticipants.first()
            ),
            participants = callParticipants,
            paddingValues = PaddingValues(0.dp),
            modifier = Modifier.fillMaxSize(),
            onRender = {}
        ) {}
    }
}
