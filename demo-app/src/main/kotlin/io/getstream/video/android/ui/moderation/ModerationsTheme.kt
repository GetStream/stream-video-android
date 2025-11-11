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

package io.getstream.video.android.ui.moderation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Provides default configurations for the Moderation Warning UI.
 *
 * The [ModerationDefaults] object contains a predefined instance of [ModerationThemeConfig],
 * which serves as the default styling configuration for the Moderation Warning UI.
 */

internal object ModerationDefaults {
    public val defaultTheme: ModerationThemeConfig = ModerationThemeConfig()
}

/**
 * Defines the configuration for Moderation Warning UI, allowing customization of its layout & styling
 *
 * @param yOffset Vertical offset for the closed captions container. Negative values move the container upwards.
 * @param horizontalMargin Horizontal margin around the container.
 * @param backgroundColor Color used for rendering the background box of the closed captions container.
 * @param titleColor Color used for rendering the caption text.
 * @param messageColor Color used for rendering the caption text.
 * @param roundedCornerShape A shape used for the  caption container.
 *
 */
internal data class ModerationThemeConfig(
    val yOffset: Dp = -100.dp,
    val horizontalMargin: Dp = 16.dp,
    val backgroundColor: Color = Color.White,
    val titleColor: Color = Color.Black,
    val messageColor: Color = Color.Gray,
    val warningStripColor: Color = Color(0xFFFFA500),
    val warningStripWidth: Dp = 12.dp,
    val roundedCornerShape: Shape? = RoundedCornerShape(16.dp),
)
