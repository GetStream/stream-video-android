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

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.getstream.video.android.R
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.call.state.CallMediaState
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.LandscapeCallControls
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.model.ScreenSharingSession

/**
 * Represents the landscape screen sharing content.
 *
 * @param call The call containing state.
 * @param session Screen sharing session to render.
 * @param participants List of participants to render under the screen share track.
 * @param callMediaState The state of the media devices for the current user.
 * @param paddingValues Padding values from the parent.
 * @param modifier Modifier for styling.
 * @param isFullscreen If we're currently in fullscreen mode.
 * @param onRender Handler when the video renders.
 * @param onCallAction Handler when the user performs various call actions.
 */
@Composable
public fun LandscapeScreenSharingContent(
    call: Call,
    session: ScreenSharingSession,
    participants: List<CallParticipantState>,
    callMediaState: CallMediaState,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean,
    onRender: (View) -> Unit,
    onCallAction: (CallAction) -> Unit,
) {
    val sharingParticipant = session.participant

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(VideoTheme.colors.screenSharingBackground)
            .padding(paddingValues)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.65f)
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

        if (!isFullscreen) {
            ParticipantsColumn(
                modifier = Modifier
                    .width(125.dp)
                    .fillMaxHeight(),
                call = call,
                participants = participants
            )

            LandscapeCallControls(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(VideoTheme.dimens.landscapeCallControlsSheetWidth)
                    .padding(6.dp),
                callMediaState = callMediaState,
                onCallAction = onCallAction,
                isScreenSharing = true
            )
        }
    }
}
