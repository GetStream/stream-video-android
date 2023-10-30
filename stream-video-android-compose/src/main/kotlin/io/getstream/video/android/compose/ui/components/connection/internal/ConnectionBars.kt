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

package io.getstream.video.android.compose.ui.components.connection.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.model.NetworkQuality

@Composable
internal fun barColorsFromQuality(
    networkQuality: NetworkQuality,
): Triple<Color, Color, Color> = when (networkQuality) {
    is NetworkQuality.Excellent -> Triple(
        VideoTheme.colors.connectionQualityBarFilled,
        VideoTheme.colors.connectionQualityBarFilled,
        VideoTheme.colors.connectionQualityBarFilled,
    )
    is NetworkQuality.Good -> Triple(
        VideoTheme.colors.connectionQualityBarFilled,
        VideoTheme.colors.connectionQualityBarFilled,
        VideoTheme.colors.connectionQualityBar,
    )
    is NetworkQuality.Poor -> Triple(
        VideoTheme.colors.connectionQualityBarFilledPoor,
        VideoTheme.colors.connectionQualityBar,
        VideoTheme.colors.connectionQualityBar,
    )
    is NetworkQuality.UnSpecified -> Triple(
        VideoTheme.colors.connectionQualityBar,
        VideoTheme.colors.connectionQualityBar,
        VideoTheme.colors.connectionQualityBar,
    )
}

@Composable
internal fun ConnectionBars(modifier: Modifier = Modifier, colors: Triple<Color, Color, Color>) {
    Row(
        modifier = modifier
            .padding(VideoTheme.dimens.connectionIndicatorBarWidth)
            .height(height = VideoTheme.dimens.connectionIndicatorBarMaxHeight),
        verticalAlignment = Alignment.Bottom,
    ) {
        Spacer(
            modifier = Modifier
                .width(VideoTheme.dimens.connectionIndicatorBarWidth)
                .fillMaxHeight(0.4f)
                .background(
                    color = colors.first,
                    shape = VideoTheme.shapes.connectionIndicatorBar,
                ),
        )
        Spacer(modifier = Modifier.width(VideoTheme.dimens.connectionIndicatorBarSeparatorWidth))
        Spacer(
            modifier = Modifier
                .width(VideoTheme.dimens.connectionIndicatorBarWidth)
                .fillMaxHeight(fraction = 0.7f)
                .background(
                    color = colors.second,
                    shape = VideoTheme.shapes.connectionIndicatorBar,
                ),
        )
        Spacer(modifier = Modifier.width(VideoTheme.dimens.connectionIndicatorBarSeparatorWidth))
        Spacer(
            modifier = Modifier
                .width(VideoTheme.dimens.connectionIndicatorBarWidth)
                .fillMaxHeight(fraction = 1f)
                .background(
                    color = colors.third,
                    shape = VideoTheme.shapes.connectionIndicatorBar,
                ),
        )
    }
}

@Preview
@Composable
private fun BarsPreview() {
    VideoTheme {
        ConnectionBars(colors = Triple(Color.Red, Color.Blue, Color.Green))
    }
}
