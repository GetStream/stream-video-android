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

package io.getstream.video.android.compose.ui.components.base.styling

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme

/**
 * Represents an icon style. Consists of color and padding.
 *
 * @param color the color applied as tint to the icon.
 * @param padding the padding applied to the icon.
 */
public data class IconStyle(
    val color: Color,
    val padding: PaddingValues = PaddingValues(0.dp),
) : StreamStyle

/**
 * Contains state styles for the icon.
 */
public data class StreamIconStyle(
    override val default: IconStyle,
    override val pressed: IconStyle,
    override val disabled: IconStyle,
) : StreamStateStyle<IconStyle>

/**
 * Provides default icon style.
 */
public open class IconStyleProvider {

    /**
     * Composable that provides default icon style.
     *
     * @param padding the padding values of the icon.
     * @param default normal color of the icon.
     * @param pressed color of the icon when pressed
     * @param disabled color of the icon when disabled
     *
     * @see [StreamIconStyle]
     */
    @Composable
    public fun defaultIconStyle(
        padding: PaddingValues = PaddingValues(VideoTheme.dimens.spacingM),
        default: Color = VideoTheme.colors.basePrimary,
        pressed: Color = VideoTheme.colors.basePrimary,
        disabled: Color = VideoTheme.colors.baseQuaternary,
    ): StreamIconStyle = StreamIconStyle(
        default = IconStyle(default, padding),
        pressed = IconStyle(pressed, padding),
        disabled = IconStyle(disabled, padding),
    )

    @Composable
    public fun customColorIconStyle(color: Color): StreamIconStyle = defaultIconStyle(
        default = color,
        pressed = color,
        disabled = color.copy(alpha = 0.16f),
    )
}

/**
 * Object accessor for a default [IconStyleProvider]
 */
public object IconStyles : IconStyleProvider()
