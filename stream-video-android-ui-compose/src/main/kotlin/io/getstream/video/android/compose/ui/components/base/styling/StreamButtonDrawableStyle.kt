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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * Represents a style for drawables used on buttons. Consists of scale and padding.
 *
 * @param scale The aspect ratio scale of the drawable.
 * @param padding The padding applied to the drawable.
 */
public data class ButtonDrawableStyle(
    val scale: ContentScale,
    val alpha: Float,
    val colorFilter: ColorFilter?,
    val padding: PaddingValues,
) : StreamStyle

/**
 * Contains state styles for the button drawable.
 */
public data class StreamButtonDrawableStyle(
    override val default: ButtonDrawableStyle,
    override val pressed: ButtonDrawableStyle,
    override val disabled: ButtonDrawableStyle,
) : StreamStateStyle<ButtonDrawableStyle>

/**
 * Provides default button drawable style.
 */
public open class ButtonDrawableStyleProvider {

    /**
     * Composable that provides the default button drawable style.
     *
     * @param padding The padding applied to the drawable.
     * @param scale The aspect ratio scale of the drawable.
     * @param defaultAlpha The alpha value of the drawable when in default state.
     * @param pressedAlpha The alpha value of the drawable when in pressed state.
     * @param disabledAlpha The alpha value of the drawable when in disabled state.
     * @param defaultColorFilter The color filter applied on the drawable when in default state.
     * @param pressedColorFilter The color filter applied on the drawable when in pressed state.
     * @param disabledColorFilter The color filter applied on the drawable when in disabled state.
     * @return [StreamButtonDrawableStyle]
     *
     * @see [StreamButtonDrawableStyle]
     */
    @Composable
    public fun defaultButtonDrawableStyle(
        padding: PaddingValues = PaddingValues(0.dp),
        scale: ContentScale = ContentScale.Crop,
        defaultAlpha: Float = DefaultAlpha,
        pressedAlpha: Float = DefaultAlpha,
        disabledAlpha: Float = 0.6f,
        defaultColorFilter: ColorFilter? = null,
        pressedColorFilter: ColorFilter? = null,
        disabledColorFilter: ColorFilter? = ColorFilter.colorMatrix(
            ColorMatrix().apply {
                setToSaturation(0f)
            },
        ),
    ): StreamButtonDrawableStyle = StreamButtonDrawableStyle(
        default = ButtonDrawableStyle(scale, defaultAlpha, defaultColorFilter, padding),
        pressed = ButtonDrawableStyle(scale, pressedAlpha, pressedColorFilter, padding),
        disabled = ButtonDrawableStyle(scale, disabledAlpha, disabledColorFilter, padding),
    )

    @Composable
    public fun customColorFilterButtonDrawableStyle(
        colorFilter: ColorFilter,
    ): StreamButtonDrawableStyle =
        defaultButtonDrawableStyle(
            defaultColorFilter = colorFilter,
            pressedColorFilter = colorFilter,
            disabledColorFilter = colorFilter,
        )
}

/**
 * Object accessor for a default [ButtonDrawableStyleProvider]
 */
public object ButtonDrawableStyles : ButtonDrawableStyleProvider()
