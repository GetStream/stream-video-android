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

package io.getstream.video.android.compose.ui.components.call.renderer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.indicator.SoundIndicator
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.mock.previewParticipantsList

@Preview
@Composable
private fun CallParticipantPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        ParticipantVideo(
            call = previewCall,
            participant = previewParticipantsList[1],
        )
    }
}

@Preview
@Composable
private fun ParticipantLabelPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        Box {
            ParticipantLabel(
                nameLabel = "The name",
                isPinned = true,
                labelPosition = BottomStart,
                hasAudio = true,
                isSpeaking = true,
                audioLevel = 0f,
                soundIndicatorContent = {
                    SoundIndicator(
                        isSpeaking = true,
                        isAudioEnabled = true,
                        audioLevel = 0.8f,
                        modifier = Modifier
                            .align(CenterVertically)
                            .padding(horizontal = VideoTheme.dimens.spacingS),
                    )
                },
            )
        }
    }
}

@Preview
@Composable
private fun ParticipantLabelPausedPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        Box {
            ParticipantLabel(
                nameLabel = "The name",
                isPinned = true,
                labelPosition = BottomStart,
                hasAudio = true,
                isSpeaking = true,
                isPaused = true,
                audioLevel = 0f,
                soundIndicatorContent = {
                    SoundIndicator(
                        isSpeaking = true,
                        isAudioEnabled = true,
                        audioLevel = 0.8f,
                        modifier = Modifier
                            .align(CenterVertically)
                            .padding(horizontal = VideoTheme.dimens.spacingS),
                    )
                },
            )
        }
    }
}

@Preview
@Composable
private fun ParticipantVideoPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        ParticipantVideoRenderer(
            call = previewCall,
            participant = previewParticipantsList[1],
        )
    }
}
