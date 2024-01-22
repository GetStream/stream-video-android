package io.getstream.video.android.compose.ui.components.base.icons

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.v2.VideoTheme
import io.getstream.video.android.compose.ui.components.base.styling.StreamStateStyle
import io.getstream.video.android.compose.ui.components.base.styling.StreamStyle

/**
 * Represents an icon style. Consists of color and padding.
 *
 * @param color the color applied as tint to the icon.
 * @param padding the padding applied to the icon.
 */
public data class IconStyle(
    val color: Color,
    val padding: PaddingValues = PaddingValues(0.dp)
) : StreamStyle

/**
 * Contains state styles for the icons.
 */
public data class StreamIconStyle(
    override val default: IconStyle,
    override val pressed: IconStyle,
    override val disabled: IconStyle,
) : StreamStateStyle<IconStyle>

/**
 * Provides default icon style.
 */
public open class StreamIconStyleProvider {

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
        disabled: Color = VideoTheme.colors.baseQuaternary
    ): StreamIconStyle = StreamIconStyle(
        default = IconStyle(default, padding),
        pressed = IconStyle(pressed, padding),
        disabled = IconStyle(disabled, padding)
    )
}

/**
 * Object accessor for a default [StreamIconStyleProvider]
 */
public object StreamIconStyles : StreamIconStyleProvider()

