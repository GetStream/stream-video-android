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

package io.getstream.video.android.compose.ui.components.connection

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.connection.internal.ConnectionBars
import io.getstream.video.android.compose.ui.components.connection.internal.barColorsFromQuality
import io.getstream.video.android.compose.ui.components.indicator.GenericIndicator
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
        modifier = modifier,
        shape = VideoTheme.shapes.connectionQualityIndicator,
        backgroundColor = VideoTheme.colors.connectionQualityBackground,
    ) {
        ConnectionBars(colors = colors)
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
