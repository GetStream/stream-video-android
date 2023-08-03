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

package io.getstream.video.android.compose.ui.components.indicator

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import kotlin.random.Random

/**
 * Used to indicate the active sound levels of a given participant.
 *
 * @param modifier Modifier for styling.
 * @param audioLevels Indicates the audio levels that will be drawn. This list must contains thee float values (0 ~ 1f).
 */
@Composable
public fun AudioVolumeIndicator(
    modifier: Modifier = Modifier,
    audioLevels: List<Float>,
) {
    val activatedColor = VideoTheme.colors.activatedVolumeIndicator
    val deActivatedColor = VideoTheme.colors.deActivatedVolumeIndicator

    if (audioLevels.size < 5) {
        throw IllegalArgumentException(
            "audioLevels list must include five audio float (0 ~ 1f) values.",
        )
    }

    val infiniteAnimation = rememberInfiniteTransition("AudioVolumeIndicator")
    val animations = mutableListOf<State<Float>>()

    repeat(5) {
        val durationMillis = Random.nextInt(500, 1000)
        animations += infiniteAnimation.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "Sounds Levels",
        )
    }

    Row(
        modifier = modifier
            .height(height = VideoTheme.dimens.audioLevelIndicatorBarMaxHeight)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(
            VideoTheme.dimens.audioLevelIndicatorBarSeparatorWidth,
        ),
    ) {
        repeat(5) { index ->
            if (index % 2 == 0) {
                val audioLevel = audioLevels[index]
                val currentSize = animations[index % animations.size].value
                var barHeightPercent = audioLevel + currentSize
                if (barHeightPercent > 1.0f) {
                    val diff = barHeightPercent - 1.0f
                    barHeightPercent = 1.0f - diff
                }

                Spacer(
                    modifier = Modifier
                        .width(VideoTheme.dimens.audioLevelIndicatorBarWidth)
                        .fillMaxHeight(
                            if (audioLevel == 0f) {
                                0.23f
                            } else {
                                barHeightPercent
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
}

@Preview
@Composable
private fun ActiveSoundLevelsPreview() {
    VideoTheme {
        Column {
            AudioVolumeIndicator(audioLevels = listOf(0.86f, 0f, 0.4f, 0f, 0.72f))
            AudioVolumeIndicator(audioLevels = listOf(0f, 0f, 0f, 0f, 0f))
        }
    }
}
