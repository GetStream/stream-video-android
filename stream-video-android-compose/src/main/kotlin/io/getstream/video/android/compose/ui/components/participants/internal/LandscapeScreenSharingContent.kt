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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.getstream.video.android.common.util.mockVideoTrack
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.internal.OverlayScreenSharingAppBar
import io.getstream.video.android.compose.ui.components.previews.ParticipantsProvider
import io.getstream.video.android.core.call.state.CallAction
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
 * @param onRender Handler when the video renders.
 * @param onCallAction Handler when the user performs various call actions.
 * @param onBackPressed Handler when the user taps back.
 */
@Composable
internal fun LandscapeScreenSharingContent(
    call: Call?,
    session: ScreenSharingSession,
    participants: List<CallParticipantState>,
    primarySpeaker: CallParticipantState?,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    onRender: (View) -> Unit,
    onCallAction: (CallAction) -> Unit,
    onBackPressed: () -> Unit,
) {
    val sharingParticipant = session.participant
    val me = participants.firstOrNull { it.isLocal }

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
            )

            if (me?.id == sharingParticipant.id) {
                OverlayScreenSharingAppBar(sharingParticipant, onBackPressed, onCallAction)
            } else {
                ScreenShareTooltip(
                    modifier = Modifier
                        .align(Alignment.TopStart),
                    sharingParticipant = sharingParticipant
                )
            }
        }

        ParticipantsColumn(
            modifier = Modifier
                .width(156.dp)
                .fillMaxHeight(),
            call = call,
            participants = participants,
            primarySpeaker = primarySpeaker
        )
    }
}

@Preview(
    device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720
)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720
)
@Composable
private fun LandscapeScreenSharingContentPreview(
    @PreviewParameter(ParticipantsProvider::class) callParticipants: List<CallParticipantState>
) {
    VideoTheme {
        LandscapeScreenSharingContent(
            call = null,
            session = ScreenSharingSession(
                track = callParticipants[1].videoTrack ?: mockVideoTrack,
                participant = callParticipants[1]
            ),
            participants = callParticipants,
            primarySpeaker = callParticipants[1],
            paddingValues = PaddingValues(0.dp),
            modifier = Modifier.fillMaxSize(),
            onRender = {},
            onCallAction = {},
            onBackPressed = {}
        )
    }
}

@Preview(
    device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720
)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = Devices.AUTOMOTIVE_1024p, widthDp = 1440, heightDp = 720
)
@Composable
private fun LandscapeScreenSharingMyContentPreview(
    @PreviewParameter(ParticipantsProvider::class) callParticipants: List<CallParticipantState>
) {
    VideoTheme {
        LandscapeScreenSharingContent(
            call = null,
            session = ScreenSharingSession(
                track = callParticipants[0].videoTrack ?: mockVideoTrack,
                participant = callParticipants[0]
            ),
            participants = callParticipants,
            primarySpeaker = callParticipants[0],
            paddingValues = PaddingValues(0.dp),
            modifier = Modifier.fillMaxSize(),
            onRender = {},
            onCallAction = {},
            onBackPressed = {}
        )
    }
}
