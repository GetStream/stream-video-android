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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
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
    val spotlightFractions = if (ORIENTATION_LANDSCAPE == orientation) {
        Pair(0.6f, 0.4f)
    } else {
        Pair(0.8f, 0.2f)
    }

    val listState =
        lazyStateWithVisibilityNotification(call = call, original = rememberLazyListState())
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Column {
            this@BoxWithConstraints.SpotlightContentPortrait(
                modifier = modifier,
                fractionHeight = spotlightFractions.first,
                background = VideoTheme.colors.participantContainerBackground,
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
            LazyRowVideoRenderer(
                state = listState,
                modifier = Modifier.wrapContentWidth().align(
                    CenterHorizontally,
                ).fillMaxHeight(spotlightFractions.second),
                call = call,
                participants = derivedParticipants,
                dominantSpeaker = speaker,
                style = style,
                videoRenderer = videoRenderer,
            )
        }
    }
}
