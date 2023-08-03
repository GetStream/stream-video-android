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

package io.getstream.video.android.compose.ui.components.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    val quality = networkQuality.quality

    Box(
        modifier = modifier
            .padding(8.dp)
            .background(
                shape = VideoTheme.shapes.connectionQualityIndicator,
                color = VideoTheme.colors.connectionQualityBackground,
            )
            .padding(6.dp),
    ) {
        Row(
            modifier = Modifier
                .height(height = VideoTheme.dimens.connectionIndicatorBarMaxHeight)
                .align(Alignment.Center),
            verticalAlignment = Alignment.Bottom,
        ) {
            Spacer(
                modifier = Modifier
                    .width(VideoTheme.dimens.connectionIndicatorBarWidth)
                    .fillMaxHeight(0.33f)
                    .background(
                        color = if (quality > 0.33f) {
                            VideoTheme.colors.connectionQualityBarFilled
                        } else {
                            VideoTheme.colors.errorAccent
                        },
                        shape = VideoTheme.shapes.connectionIndicatorBar,
                    ),
            )

            Spacer(modifier = Modifier.width(3.dp))

            Spacer(
                modifier = Modifier
                    .width(VideoTheme.dimens.connectionIndicatorBarWidth)
                    .fillMaxHeight(fraction = 0.66f)
                    .background(
                        color = if (quality >= 0.66f) {
                            VideoTheme.colors.connectionQualityBarFilled
                        } else {
                            VideoTheme.colors.connectionQualityBar
                        },
                        shape = VideoTheme.shapes.connectionIndicatorBar,
                    ),
            )

            Spacer(modifier = Modifier.width(3.dp))

            Spacer(
                modifier = Modifier
                    .width(VideoTheme.dimens.connectionIndicatorBarWidth)
                    .fillMaxHeight(fraction = 1f)
                    .background(
                        color = if (quality >= 1) {
                            VideoTheme.colors.connectionQualityBarFilled
                        } else {
                            VideoTheme.colors.connectionQualityBar
                        },
                        shape = VideoTheme.shapes.connectionIndicatorBar,
                    ),
            )
        }
    }
}

@Preview
@Composable
private fun ConnectionQualityIndicatorPreview() {
    VideoTheme {
        Row {
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
