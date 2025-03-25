/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatarBackground
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.ScreenSharingVideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.VideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.copy
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.model.ScreenSharingSession
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipantsList

/**
 * Represents the portrait screen sharing content.
 *
 * @param call The call containing state.
 * @param session Screen sharing session to render.
 * @param participants List of participants to render under the screen share track.
 * @param modifier Modifier for styling.
 * @param style Defined properties for styling a single video call track.
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
        style: VideoRendererStyle,
    ) -> Unit = { videoModifier, videoCall, videoParticipant, videoStyle ->
        ParticipantVideo(
            modifier = videoModifier,
            call = videoCall,
            participant = videoParticipant,
            style = videoStyle,
        )
    },
    screenSharingFallbackContent: @Composable (ScreenSharingSession) -> Unit = {
        val userName by it.participant.userNameOrId.collectAsStateWithLifecycle()
        val userImage by it.participant.image.collectAsStateWithLifecycle()
        UserAvatarBackground(userImage = userImage, userName = userName)
    },
) {
    val sharingParticipant = session.participant
    val me by call.state.me.collectAsStateWithLifecycle()

    val paddedModifier = modifier.padding(VideoTheme.dimens.spacingXXs)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Fixed(2),
            content = {
                item(span = { GridItemSpan(2) }) {
                    ScreenSharingContent(
                        modifier = modifier,
                        call = call,
                        session = session,
                        isZoomable = isZoomable,
                        me = me,
                        sharingParticipant = sharingParticipant,
                        fallbackContent = screenSharingFallbackContent,
                    )
                }
                items(
                    count = participants.size,
                ) { key ->
                    // make 3 items exactly fit available height
                    val itemHeight = with(LocalDensity.current) {
                        (constraints.maxHeight / 6).toDp()
                    }
                    val participant = participants[key]
                    videoRenderer.invoke(
                        paddedModifier.height(itemHeight),
                        call,
                        participant,
                        style.copy(
                            isFocused = dominantSpeaker?.sessionId == participant.sessionId,
                        ),
                    )
                }
            },
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.ScreenSharingContent(
    modifier: Modifier,
    call: Call,
    session: ScreenSharingSession,
    isZoomable: Boolean,
    me: ParticipantState?,
    sharingParticipant: ParticipantState,
    fallbackContent: @Composable (ScreenSharingSession) -> Unit,
) {
    val itemHeight = with(LocalDensity.current) {
        ((constraints.maxHeight * 0.45).toInt()).toDp()
    }
    Column(
        modifier = modifier
            .padding(VideoTheme.dimens.spacingXXs),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(VideoTheme.colors.baseSheetSecondary)
                .fillMaxWidth()
                .height(itemHeight),
        ) {
            ScreenShareVideoRenderer(
                modifier = Modifier.fillMaxWidth(),
                call = call,
                session = session,
                isZoomable = isZoomable,
                fallbackContent = fallbackContent,
            )

            if (me?.sessionId != sharingParticipant.sessionId) {
                ScreenShareTooltip(
                    modifier = Modifier.align(Alignment.TopStart),
                    sharingParticipant = sharingParticipant,
                )
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PortraitScreenSharingContentPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        PortraitScreenSharingVideoRenderer(
            call = previewCall,
            session = ScreenSharingSession(
                participant = previewParticipantsList[1],
            ),
            participants = previewParticipantsList,
            dominantSpeaker = previewParticipantsList[1],
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
            call = previewCall,
            session = ScreenSharingSession(
                participant = previewParticipantsList[0],
            ),
            participants = previewParticipantsList,
            dominantSpeaker = previewParticipantsList[0],
            modifier = Modifier.fillMaxSize(),
        )
    }
}
