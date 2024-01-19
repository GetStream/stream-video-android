package io.getstream.video.android.compose.ui.components.base.texts

import androidx.compose.ui.graphics.Color
import io.getstream.video.android.compose.theme.v2.VideoTheme
import io.getstream.video.android.compose.ui.components.base.styling.StreamStateStyle
import io.getstream.video.android.compose.ui.components.base.styling.StreamStyle
import java.time.format.TextStyle

/**
 * Wrapper for the platform text style.
 */
public data class TextStyleWrapper(
        public val androidTextStyle: TextStyle
) : StreamStyle

/**
 * Stream text style
 */
public data class StreamTextStyle(
        override val default: TextStyleWrapper,
        override val disabled: TextStyleWrapper,
        override val pressed: TextStyleWrapper
) : StreamStateStyle<TextStyleWrapper>

public class TextStyleStyleProvider {
    public fun defaultTextStyle(
        default: Color = VideoTheme.colors.basePrimary,
        pressed: Color = VideoTheme.colors.baseTertiary,
        disabled: Color = VideoTheme.colors.baseSecondary
    )
}

