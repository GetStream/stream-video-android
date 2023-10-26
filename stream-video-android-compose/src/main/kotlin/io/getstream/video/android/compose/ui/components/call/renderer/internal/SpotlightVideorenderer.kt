/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.renderer.ParticipantVideo
import io.getstream.video.android.compose.ui.components.call.renderer.VideoRendererStyle
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

@Composable
internal fun SpotlightVideoRenderer(
    call: Call,
    speaker: ParticipantState?,
    participants: List<ParticipantState>,
    orientation: Int,
    modifier: Modifier,
    isZoomable: Boolean,
    style: VideoRendererStyle,
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
    val derivedParticipants by remember(key1 = participants) {
        derivedStateOf {
            participants.filterNot {
                it.sessionId == speaker?.sessionId
            }
        }
    }
    val listState =
        lazyStateWithVisibilityNotification(call = call, original = rememberLazyListState())

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (ORIENTATION_LANDSCAPE == orientation) {
            val spotlightFractions = Pair(0.8f, 0.2f)
            Row {
                this@BoxWithConstraints.SpotlightContentLandscape(
                    modifier = modifier,
                    fractionWidth = spotlightFractions.first,
                    background = VideoTheme.colors.participantContainerBackground,
                ) {
                    SpeakerSpotlight(speaker, videoRenderer, isZoomable, call, style)
                }
                LazyColumnVideoRenderer(
                    state = listState,
                    modifier = Modifier
                        .wrapContentHeight()
                        .align(CenterVertically)
                        .fillMaxWidth(spotlightFractions.second),
                    call = call,
                    participants = derivedParticipants,
                    dominantSpeaker = speaker,
                    style = style,
                    videoRenderer = videoRenderer,
                )
            }
        } else {
            val spotlightFractions = Pair(0.9f, 0.2f)
            Column {
                this@BoxWithConstraints.SpotlightContentPortrait(
                    modifier = modifier,
                    fractionHeight = spotlightFractions.first,
                    background = VideoTheme.colors.participantContainerBackground,
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
                    modifier = Modifier
                        .wrapContentWidth()
                        .offset(y = (-1).dp)
                        .align(CenterHorizontally)
                        .fillMaxHeight(spotlightFractions.second),
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
