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

package io.getstream.video.android.ui.closedcaptions

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.StreamColors

/**
 * Provides default configurations for the Closed Captions UI.
 *
 * The [ClosedCaptionsDefaults] object contains a predefined instance of [ClosedCaptionsThemeConfig],
 * which serves as the default styling and behavior configuration for the closed captions UI.
 * Developers can use this default configuration or provide a custom one to override specific values.
 */

public object ClosedCaptionsDefaults {
    /**
     * The default configuration for closed captions, defining layout, styling, and behavior.
     *
     * - `yOffset`: Vertical offset for positioning the closed captions container.
     * - `horizontalMargin`: Horizontal margin around the container.
     * - `boxAlpha`: Opacity of the background box containing the captions.
     * - `boxPadding`: Padding inside the background box.
     * - `speakerColor`: Color used for the speaker's name text.
     * - `textColor`: Color used for the caption text.
     * - `maxVisibleCaptions`: The maximum number of captions to display in the container at once.
     * - `roundedCornerShape`: The corner radius of the background box.
     */
    public val config: ClosedCaptionsThemeConfig = ClosedCaptionsThemeConfig(
        yOffset = -50.dp,
        horizontalMargin = 16.dp,
        boxAlpha = 0.5f,
        boxPadding = 12.dp,
        speakerColor = Color.Yellow,
        textColor = Color.White,
        maxVisibleCaptions = 3,
        roundedCornerShape = RoundedCornerShape(16.dp),
    )

    @Composable
    public fun streamThemeConfig(): ClosedCaptionsThemeConfig {
        val colors = StreamColors.defaultColors()
        return config.copy(
            backgroundColor = colors.baseSheetPrimary,
            speakerColor = colors.baseQuaternary,
            textColor = colors.basePrimary,
        )
    }
}

/**
 * Defines the configuration for Closed Captions UI, allowing customization of its layout, styling, and behavior.
 *
 * This configuration can be used to style the closed captions container and its contents. Developers can
 * customize the appearance by overriding specific values as needed.
 *
 * @param yOffset Vertical offset for the closed captions container. Negative values move the container upwards.
 * @param horizontalMargin Horizontal margin around the container.
 * @param boxAlpha Background opacity of the closed captions container, where `0.0f` is fully transparent
 *                 and `1.0f` is fully opaque.
 * @param boxPadding Padding inside the background box of the closed captions container.
 * @param backgroundColor Color used for rendering the background box of the closed captions container.
 * @param speakerColor Color used for rendering the speaker's name text.
 * @param textColor Color used for rendering the caption text.
 * @param maxVisibleCaptions The maximum number of captions visible at one time in the closed captions container.
 *                           Must be less than or equal to [ClosedCaptionsConfig.maxCaptions] to ensure consistency.
 * @param roundedCornerShape A shape used for the  caption container.
 *
 * Example Usage:
 * ```
 * val customConfig = ClosedCaptionsThemeConfig(
 *     yOffset = -100.dp,
 *     horizontalMargin = 20.dp,
 *     boxAlpha = 0.7f,
 *     boxPadding = 16.dp,
 *     backgroundColor = Color.Black,
 *     speakerColor = Color.Cyan,
 *     textColor = Color.Green,
 *     maxVisibleCaptions = 5,
 *     roundedCornerShape = RounderCornerShape(12.dp),
 * )
 * ```
 */
public data class ClosedCaptionsThemeConfig(
    val yOffset: Dp = -50.dp,
    val horizontalMargin: Dp = 0.dp,
    val boxAlpha: Float = 1f,
    val boxPadding: Dp = 0.dp,
    val backgroundColor: Color = Color.Black,
    val speakerColor: Color = Color.Yellow,
    val textColor: Color = Color.White,
    val maxVisibleCaptions: Int = 3,
    val roundedCornerShape: Shape? = null,
)
