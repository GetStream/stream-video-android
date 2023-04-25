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

package io.getstream.video.android.compose.ui.components.call.renderer.internal

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.common.util.MockUtils
import io.getstream.video.android.common.util.mockCall
import io.getstream.video.android.common.util.mockParticipants
import io.getstream.video.android.common.util.mockVideoTrackWrapper
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.internal.OverlayScreenSharingAppBar
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.call.state.CallAction
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
internal fun LandscapeScreenSharingVideoRenderer(
    call: Call,
    session: ScreenSharingSession,
    participants: List<ParticipantState>,
    primarySpeaker: ParticipantState?,
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
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.65f)
        ) {
            ScreenShareVideoRenderer(
                modifier = Modifier.fillMaxSize(),
                call = call,
                session = session,
                onRender = onRender,
            )

            if (me?.initialUser?.id == sharingParticipant.initialUser.id) {
                OverlayScreenSharingAppBar(sharingParticipant, onBackPressed, onCallAction)
            } else {
                ScreenShareTooltip(
                    modifier = Modifier.align(Alignment.TopStart),
                    sharingParticipant = sharingParticipant
                )
            }
        }

        LazyColumnVideoRenderer(
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
    device = Devices.AUTOMOTIVE_1024p,
    widthDp = 1440,
    heightDp = 720
)
@Composable
private fun LandscapeScreenSharingContentPreview() {
    MockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LandscapeScreenSharingVideoRenderer(
            call = mockCall,
            session = ScreenSharingSession(
                track = mockParticipants[1].videoTrack.value ?: mockVideoTrackWrapper,
                participant = mockParticipants[1]
            ),
            participants = mockParticipants,
            primarySpeaker = mockParticipants[1],
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
    device = Devices.AUTOMOTIVE_1024p,
    widthDp = 1440,
    heightDp = 720
)
@Composable
private fun LandscapeScreenSharingMyContentPreview() {
    MockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LandscapeScreenSharingVideoRenderer(
            call = mockCall,
            session = ScreenSharingSession(
                track = mockParticipants[0].videoTrack.value ?: mockVideoTrackWrapper,
                participant = mockParticipants[0]
            ),
            participants = mockParticipants,
            primarySpeaker = mockParticipants[0],
            modifier = Modifier.fillMaxSize(),
            onRender = {},
            onCallAction = {},
            onBackPressed = {}
        )
    }
}
