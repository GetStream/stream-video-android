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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.getstream.video.android.common.util.mockVideoTrack
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.internal.DefaultCallControlsContent
import io.getstream.video.android.compose.ui.components.internal.OverlayScreenSharingAppBar
import io.getstream.video.android.compose.ui.components.previews.ParticipantsProvider
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallMediaState
import io.getstream.video.android.core.model.Call
import io.getstream.video.android.core.model.CallParticipantState
import io.getstream.video.android.core.model.ScreenSharingSession

/**
 * Represents the landscape screen sharing content.
 *
 * @param call The call containing state.
 * @param session Screen sharing session to render.
 * @param participants List of participants to render under the screen share track.
 * @param paddingValues Padding values from the parent.
 * @param modifier Modifier for styling.
 * @param isFullscreen If we're currently in fullscreen mode.
 * @param onRender Handler when the video renders.
 * @param onCallAction Handler when the user performs various call actions.
 * @param onBackPressed Handler when the user taps back.
 * @param callControlsContent Content shown that allows users to trigger different actions.
 */
@Composable
internal fun LandscapeScreenSharingContent(
    call: Call?,
    session: ScreenSharingSession,
    participants: List<CallParticipantState>,
    primarySpeaker: CallParticipantState?,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean,
    onRender: (View) -> Unit,
    onCallAction: (CallAction) -> Unit,
    onBackPressed: () -> Unit,
    callControlsContent: @Composable () -> Unit
) {
    val sharingParticipant = session.participant

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(VideoTheme.colors.screenSharingBackground)
            .padding(paddingValues)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.65f)
        ) {
            ScreenShareContent(
                modifier = Modifier.fillMaxSize(),
                call = call,
                session = session,
                onRender = onRender,
                isFullscreen = isFullscreen,
                onCallAction = onCallAction
            )

            OverlayScreenSharingAppBar(sharingParticipant, onBackPressed, onCallAction)
        }

        if (!isFullscreen) {
            ParticipantsColumn(
                modifier = Modifier
                    .width(125.dp)
                    .fillMaxHeight(),
                call = call,
                participants = participants,
                primarySpeaker = primarySpeaker
            )

            callControlsContent()
        }
    }
}

@Preview
@Composable
private fun LandscapeScreenSharingContentPreview(
    @PreviewParameter(ParticipantsProvider::class) callParticipants: List<CallParticipantState>
) {
    VideoTheme {
        LandscapeScreenSharingContent(
            call = null,
            session = ScreenSharingSession(
                track = callParticipants.first().videoTrack ?: mockVideoTrack,
                participant = callParticipants.first()
            ),
            participants = callParticipants,
            primarySpeaker = callParticipants[0],
            paddingValues = PaddingValues(0.dp),
            modifier = Modifier.fillMaxSize(),
            isFullscreen = true,
            onRender = {},
            onCallAction = {},
            onBackPressed = {}
        ) {
            DefaultCallControlsContent(
                call = null,
                callMediaState = CallMediaState(),
            ) {}
        }
    }
}
