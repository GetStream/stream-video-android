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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.model.NetworkQuality
import stream.video.sfu.models.ConnectionQuality

/**
 * Shows the quality of the user's connection depending on the [ConnectionQuality] level.
 *
 * @param networkQuality The quality level.
 * @param modifier Modifier for styling.
 */
@Composable
public fun NetworkQualityIndicator(
    networkQuality: NetworkQuality,
    modifier: Modifier = Modifier,
) {
    val colors = barColorsFromQuality(networkQuality)
    GenericIndicator(
        shape = RoundedCornerShape(topStart = VideoTheme.dimens.roundnessM),
        modifier = modifier,
    ) {
        ConnectionBars(colors = colors)
    }
}

@Composable
internal fun barColorsFromQuality(
    networkQuality: NetworkQuality,
): Triple<Color, Color, Color> = when (networkQuality) {
    is NetworkQuality.Excellent -> Triple(
        VideoTheme.colors.brandGreen,
        VideoTheme.colors.brandGreen,
        VideoTheme.colors.brandGreen,
    )
    is NetworkQuality.Good -> Triple(
        VideoTheme.colors.brandYellow,
        VideoTheme.colors.brandYellow,
        VideoTheme.colors.basePrimary,
    )
    is NetworkQuality.Poor -> Triple(
        VideoTheme.colors.brandRed,
        VideoTheme.colors.basePrimary,
        VideoTheme.colors.basePrimary,
    )
    is NetworkQuality.UnSpecified -> Triple(
        VideoTheme.colors.basePrimary,
        VideoTheme.colors.basePrimary,
        VideoTheme.colors.basePrimary,
    )
}

@Composable
internal fun ConnectionBars(modifier: Modifier = Modifier, colors: Triple<Color, Color, Color>) {
    val shape = RoundedCornerShape(VideoTheme.dimens.roundnessM)
    Row(
        modifier = modifier
            .height(height = VideoTheme.dimens.genericS),
        verticalAlignment = Alignment.Bottom,
    ) {
        Spacer(
            modifier = Modifier
                .width(VideoTheme.dimens.genericXXs)
                .fillMaxHeight(0.4f)
                .background(
                    color = colors.first,
                    shape = shape,
                ),
        )
        Spacer(modifier = Modifier.width(VideoTheme.dimens.genericXXs))
        Spacer(
            modifier = Modifier
                .width(VideoTheme.dimens.genericXXs)
                .fillMaxHeight(fraction = 0.7f)
                .background(
                    color = colors.second,
                    shape = shape,
                ),
        )
        Spacer(modifier = Modifier.width(VideoTheme.dimens.genericXXs))
        Spacer(
            modifier = Modifier
                .width(VideoTheme.dimens.genericXXs)
                .fillMaxHeight(fraction = 1f)
                .background(
                    color = colors.third,
                    shape = shape,
                ),
        )
    }
}

@Preview
@Composable
private fun ConnectionQualityIndicatorPreview() {
    VideoTheme {
        Row {
            NetworkQualityIndicator(
                networkQuality = NetworkQuality.UnSpecified(),
            )
            NetworkQualityIndicator(
                networkQuality = NetworkQuality.Poor(),
            )
            NetworkQualityIndicator(
                networkQuality = NetworkQuality.Good(),
            )
            NetworkQualityIndicator(
                networkQuality = NetworkQuality.Excellent(),
            )
        }
    }
}
