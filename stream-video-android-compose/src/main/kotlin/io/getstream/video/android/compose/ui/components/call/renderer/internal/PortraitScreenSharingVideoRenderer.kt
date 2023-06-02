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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.ScreenSharingVideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.VideoRendererStyle
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockCall
import io.getstream.video.android.mock.mockParticipantList

/**
 * Represents the portrait screen sharing content.
 *
 * @param call The call containing state.
 * @param session Screen sharing session to render.
 * @param participants List of participants to render under the screen share track.
 * @param modifier Modifier for styling.
 * @param style Represents a regular video call render styles.
 * @param videoRenderer A single video renderer renders each individual participant.
 */
@Composable
internal fun PortraitScreenSharingVideoRenderer(
    modifier: Modifier = Modifier,
    call: Call,
    session: ScreenSharingSession,
    participants: List<ParticipantState>,
    dominantSpeaker: ParticipantState?,
    isZoomable: Boolean = true,
    style: VideoRendererStyle = ScreenSharingVideoRendererStyle(),
    videoRenderer: @Composable (
        modifier: Modifier,
        call: Call,
        participant: ParticipantState,
        style: VideoRendererStyle
    ) -> Unit = { videoModifier, videoCall, videoParticipant, videoStyle ->
        ParticipantVideo(
            modifier = videoModifier,
            call = videoCall,
            participant = videoParticipant,
            style = videoStyle
        )
    },
) {
    val sharingParticipant = session.participant
    val me by call.state.me.collectAsStateWithLifecycle()

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
                isZoomable = isZoomable
            )

            if (me?.initialUser?.id != sharingParticipant.initialUser.id) {
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
            dominantSpeaker = dominantSpeaker,
            participants = participants,
            style = style,
            videoRenderer = videoRenderer
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PortraitScreenSharingContentPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        PortraitScreenSharingVideoRenderer(
            call = mockCall,
            session = ScreenSharingSession(
                participant = mockParticipantList[1]
            ),
            participants = mockParticipantList,
            dominantSpeaker = mockParticipantList[1],
            modifier = Modifier.fillMaxSize(),
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
                participant = mockParticipantList[0]
            ),
            participants = mockParticipantList,
            dominantSpeaker = mockParticipantList[0],
            modifier = Modifier.fillMaxSize(),
        )
    }
}
