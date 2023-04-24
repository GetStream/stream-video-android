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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
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
 * Represents the portrait screen sharing content.
 *
 * @param call The call containing state.
 * @param session Screen sharing session to render.
 * @param participants List of participants to render under the screen share track.
 * @param modifier Modifier for styling.
 * @param onRender Handler when the video renders.
 */
@Composable
internal fun PortraitScreenSharingVideoRenderer(
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
    val me by call.state.me.collectAsState()

    Column(
        modifier = modifier.background(VideoTheme.colors.screenSharingBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            ScreenShareVideoRenderer(
                modifier = Modifier.fillMaxWidth(),
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

        Spacer(modifier = Modifier.height(VideoTheme.dimens.screenShareParticipantsScreenShareListMargin))

        LazyRowVideoRenderer(
            modifier = Modifier.height(VideoTheme.dimens.screenShareParticipantsRowHeight),
            call = call,
            primarySpeaker = primarySpeaker,
            participants = participants
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PortraitScreenSharingContentPreview() {
    MockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        PortraitScreenSharingVideoRenderer(
            call = mockCall,
            session = ScreenSharingSession(
                track = mockVideoTrackWrapper,
                participant = mockParticipants[1]
            ),
            participants = mockParticipants,
            primarySpeaker = mockParticipants[1],
            modifier = Modifier.fillMaxSize(),
            onRender = {},
            onBackPressed = {},
            onCallAction = {}
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PortraitScreenSharingMyContentPreview() {
    VideoTheme {
        PortraitScreenSharingVideoRenderer(
            call = mockCall,
            session = ScreenSharingSession(
                track = mockVideoTrackWrapper,
                participant = mockParticipants[0]
            ),
            participants = mockParticipants,
            primarySpeaker = mockParticipants[0],
            modifier = Modifier.fillMaxSize(),
            onRender = {},
            onBackPressed = {},
            onCallAction = {}
        )
    }
}
