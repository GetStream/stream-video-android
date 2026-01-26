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

package io.getstream.video.android.compose.ui.components.indicator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme

/**
 * A composable that wraps its content into a rounded semi-transparent background.
 */
@Composable
internal fun GenericIndicator(
    modifier: Modifier = Modifier,
    shape: Shape = VideoTheme.shapes.indicator,
    backgroundColor: Color = VideoTheme.colors.baseSheetQuarternary,
    content: @Composable BoxScope.() -> Unit,
) {
    // val alphaColor = backgroundColor.copy(alpha = 0.3f)
    Box(
        modifier = modifier.size(VideoTheme.dimens.componentHeightM),
    ) {
        // Ensure content is center aligned and padded
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    color = backgroundColor,
                    shape = shape,
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(4.dp),
        ) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                content(this)
            }
        }
    }
}

@Preview
@Composable
private fun PreviewIndicatorBackground() {
    VideoTheme {
        Column {
            GenericIndicator {
                AudioVolumeIndicator(audioLevels = 0.5f)
            }
            GenericIndicator {
                MicrophoneIndicator(isMicrophoneEnabled = false)
            }
            GenericIndicator {
                MicrophoneIndicator(isMicrophoneEnabled = true)
            }
        }
    }
}
