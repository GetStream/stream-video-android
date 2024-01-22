package io.getstream.video.android.compose.ui.components.base.texts

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.TextUnit
import io.getstream.video.android.compose.theme.v2.VideoTheme
import io.getstream.video.android.compose.ui.components.base.styling.StreamStateStyle
import io.getstream.video.android.compose.ui.components.base.styling.StreamStyle

/**
 * Wrapper for the platform text style.
 */
public data class TextStyleWrapper(
        public val platform: TextStyle
) : StreamStyle

/**
 * Stream text style
 */
public data class StreamTextStyle(
        override val default: TextStyleWrapper,
        override val disabled: TextStyleWrapper,
        override val pressed: TextStyleWrapper
) : StreamStateStyle<TextStyleWrapper>

public open class TextStyleStyleProvider {

    @Composable
    public fun defaultTextStyle(
        default: TextStyleWrapper = VideoTheme.typography.label.wrapper(),
        pressed: TextStyleWrapper = VideoTheme.typography.label.wrapper(),
        disabled: TextStyleWrapper = VideoTheme.typography.label.copy(
            color = VideoTheme.typography.label.color.copy(alpha = 0.2f)
        ).wrapper()
    ) : StreamTextStyle = StreamTextStyle(default, disabled, pressed)
}

public object StreamTextStyles : TextStyleStyleProvider()

// Utilities
internal fun TextStyle.wrapper() : TextStyleWrapper = TextStyleWrapper(platform = this)