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

package io.getstream.video.android.compose.ui.components.indicator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme

/**
 * Used to indicate the active sound levels of a given participant.
 *
 * @param modifier Modifier for styling.
 * @param audioLevels Indicates the audio levels that will be drawn. This list must contains thee float values (0 ~ 1f).
 */
@Composable
public fun AudioVolumeIndicator(
    modifier: Modifier = Modifier,
    audioLevels: Float,
) {
    val activatedColor = VideoTheme.colors.activatedVolumeIndicator
    val deActivatedColor = VideoTheme.colors.deActivatedVolumeIndicator

    val defaultBarHeight = 0.23f

    Row(
        modifier = modifier
            .height(height = VideoTheme.dimens.audioLevelIndicatorBarMaxHeight)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            VideoTheme.dimens.audioLevelIndicatorBarSeparatorWidth,
        ),
    ) {
        repeat(3) { index ->

            // First bar 60%, second 100%, third 33%
            val audioLevel =
                when (index) {
                    0 -> {
                        audioLevels * 0.6f
                    }

                    2 -> {
                        audioLevels * 0.33f
                    }

                    else -> {
                        audioLevels
                    }
                }

            Spacer(
                modifier = Modifier
                    .width(VideoTheme.dimens.audioLevelIndicatorBarWidth)
                    .fillMaxHeight(
                        if (audioLevel == 0f) {
                            defaultBarHeight
                        } else {
                            (audioLevel + defaultBarHeight).coerceAtMost(1f)
                        },
                    )
                    .background(
                        color = if (audioLevel == 0f) deActivatedColor else activatedColor,
                        shape = RoundedCornerShape(16.dp),
                    ),
            )
        }
    }
}

@Preview
@Composable
private fun ActiveSoundLevelsPreview() {
    VideoTheme {
        Column {
            AudioVolumeIndicator(audioLevels = 0f)
            AudioVolumeIndicator(audioLevels = 0.3f)
        }
    }
}
