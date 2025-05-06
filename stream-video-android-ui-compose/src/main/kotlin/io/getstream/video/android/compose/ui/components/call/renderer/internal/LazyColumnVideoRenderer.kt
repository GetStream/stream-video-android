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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.ScreenSharingVideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.VideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.copy
import io.getstream.video.android.compose.ui.extensions.topOrBottomPadding
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipantsList

/**
 * Shows a column of call participants.
 *
 * @param call The state of the call.
 * @param participants List of participants to show.
 * @param modifier Modifier for styling.
 */
@Composable
internal fun LazyColumnVideoRenderer(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    itemModifier: Modifier = Modifier.size(
        VideoTheme.dimens.genericMax * 1.8f,
        VideoTheme.dimens.genericMax,
    ),
    call: Call,
    participants: List<ParticipantState>,
    dominantSpeaker: ParticipantState?,
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
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        verticalArrangement = Arrangement.spacedBy(
            VideoTheme.dimens.spacingXs,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            itemsIndexed(
                items = participants,
                key = { _, it -> it.sessionId },
            ) { index, participant ->
                ListVideoRenderer(
                    modifier = itemModifier.topOrBottomPadding(
                        value = VideoTheme.dimens.spacingXs,
                        index = index,
                        first = 0,
                        last = participants.lastIndex,
                    ),
                    call = call,
                    participant = participant,
                    dominantSpeaker = dominantSpeaker,
                    style = style,
                    videoRenderer = videoRenderer,
                )
            }
        },
    )
}

/**
 * Shows a single call participant in a list.
 *
 * @param call The call state.
 * @param participant The participant to render.
 */
@Composable
private fun ListVideoRenderer(
    call: Call,
    participant: ParticipantState,
    modifier: Modifier = Modifier,
    dominantSpeaker: ParticipantState?,
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
) {
    val isDomSpeakerSpeaking by dominantSpeaker
        ?.speaking
        ?.collectAsStateWithLifecycle(initialValue = false)
        ?: remember { mutableStateOf(false) }

    videoRenderer.invoke(
        modifier,
        call,
        participant,
        style.copy(
            isFocused = isDomSpeakerSpeaking && participant.sessionId == dominantSpeaker?.sessionId,
        ),
    )
}

@Preview
@Composable
private fun ParticipantsColumnPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LazyColumnVideoRenderer(
            call = previewCall,
            participants = previewParticipantsList,
            dominantSpeaker = previewParticipantsList[0],
        )
    }
}
