/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.SpotlightVideoRendererStyle
import io.getstream.video.android.compose.ui.components.call.renderer.VideoRendererStyle
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipant
import io.getstream.video.android.mock.previewParticipantsList
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import java.lang.Integer.max

@Composable
internal fun SpotlightVideoRenderer(
    modifier: Modifier = Modifier,
    call: Call,
    speaker: ParticipantState?,
    participants: List<ParticipantState>,
    orientation: Int = ORIENTATION_PORTRAIT,
    isZoomable: Boolean = true,
    style: VideoRendererStyle = SpotlightVideoRendererStyle(),
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
    if (participants.size == 1) {
        // Just display the one participant
        videoRenderer.invoke(
            modifier.fillMaxSize().padding(VideoTheme.dimens.spacingS),
            call,
            participants[0],
            style,
        )
        return
    }

    val derivedParticipants by remember(key1 = participants, key2 = speaker) {
        derivedStateOf {
            participants.filterNot {
                it.sessionId == speaker?.sessionId
            }
        }
    }
    val listState =
        lazyStateWithVisibilityNotification(call = call, original = rememberLazyListState())

    Box(modifier = modifier.fillMaxSize()) {
        if (ORIENTATION_LANDSCAPE == orientation) {
            Row {
                SpotlightContentLandscape(
                    modifier = modifier.weight(0.7f),
                    background = VideoTheme.colors.baseSheetSecondary,
                ) {
                    SpeakerSpotlight(speaker, videoRenderer, isZoomable, call, style)
                }
                LazyColumnVideoRenderer(
                    state = listState,
                    itemModifier = Modifier.fillHeightIfParticipantsCount(3, participants.size),
                    modifier = Modifier
                        .align(CenterVertically)
                        .wrapContentSize(),
                    call = call,
                    participants = derivedParticipants,
                    dominantSpeaker = speaker,
                    style = style,
                    videoRenderer = videoRenderer,
                )
            }
        } else {
            // *2 to account for the controls
            Column(
                modifier = Modifier.padding(bottom = VideoTheme.dimens.spacingXXs * 2),
            ) {
                SpotlightContentPortrait(
                    modifier = modifier.weight(1f),
                    background = VideoTheme.colors.baseSheetSecondary,
                ) {
                    SpeakerSpotlight(
                        speaker = speaker,
                        videoRenderer = videoRenderer,
                        isZoomable = isZoomable,
                        call = call,
                        style = style,
                    )
                }
                LazyRowVideoRenderer(
                    state = listState,
                    itemModifier = Modifier.fillWidthIfParticipantCount(3, participants.size),
                    modifier = Modifier
                        .wrapContentWidth()
                        .align(CenterHorizontally)
                        .height(VideoTheme.dimens.genericMax),
                    call = call,
                    participants = derivedParticipants,
                    dominantSpeaker = speaker,
                    style = style,
                    videoRenderer = videoRenderer,
                )
            }
        }
    }
}

@Composable
private fun SpeakerSpotlight(
    speaker: ParticipantState?,
    videoRenderer: @Composable (
        modifier: Modifier,
        call: Call,
        participant: ParticipantState,
        style: VideoRendererStyle,
    ) -> Unit,
    isZoomable: Boolean,
    call: Call,
    style: VideoRendererStyle,
) {
    if (speaker != null) {
        videoRenderer.invoke(
            Modifier
                .fillMaxSize()
                .zoomable(rememberZoomableState(), isZoomable),
            call,
            speaker,
            style,
        )
    }
}

private fun Modifier.fillWidthIfParticipantCount(fillCount: Int, totalCount: Int): Modifier = composed {
    // -1 because one user is in spotlight
    val itemWidth = LocalConfiguration.current.screenWidthDp / max(fillCount - 1, 1)
    when (totalCount) {
        fillCount -> this.fillMaxHeight().width(itemWidth.dp)
        else -> this.size(
            VideoTheme.dimens.genericMax * 1.5f,
            VideoTheme.dimens.genericMax,
        )
    }
}
private fun Modifier.fillHeightIfParticipantsCount(
    fillCount: Int,
    totalCount: Int,
): Modifier = composed {
    // -1 because one user is in spotlight
    val itemHeight = LocalConfiguration.current.screenHeightDp / max(fillCount - 1, 1)
    when (totalCount) {
        fillCount -> this.size(
            VideoTheme.dimens.genericMax * 1.5f,
            itemHeight.dp,
        )
        else -> this.size(
            VideoTheme.dimens.genericMax * 1.5f,
            VideoTheme.dimens.genericMax,
        )
    }
}

@Preview
@Composable
private fun SpotlightParticipantsPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        SpotlightVideoRenderer(
            call = previewCall,
            speaker = previewParticipant,
            participants = previewParticipantsList,
        )
    }
}

@Preview
@Composable
private fun SpotlightTwoParticipantsPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        SpotlightVideoRenderer(
            call = previewCall,
            speaker = previewParticipant,
            participants = previewParticipantsList.take(3),
        )
    }
}

@Preview(
    device = Devices.AUTOMOTIVE_1024p,
    widthDp = 1440,
    heightDp = 720,
)
@Composable
private fun SpotlightParticipantsLandscapePreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        SpotlightVideoRenderer(
            call = previewCall,
            orientation = ORIENTATION_LANDSCAPE,
            speaker = previewParticipant,
            participants = previewParticipantsList,
        )
    }
}

@Preview(
    device = Devices.AUTOMOTIVE_1024p,
    widthDp = 1440,
    heightDp = 720,
)
@Composable
private fun SpotlightThreeParticipantsLandscapePreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        SpotlightVideoRenderer(
            call = previewCall,
            orientation = ORIENTATION_LANDSCAPE,
            speaker = previewParticipant,
            participants = previewParticipantsList.take(3),
        )
    }
}
