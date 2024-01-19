package io.getstream.video.android.compose.ui.components.base.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.v2.VideoTheme
import io.getstream.video.android.compose.ui.components.base.icons.DefaultIconStyles
import io.getstream.video.android.compose.ui.components.base.icons.IconStyle
import io.getstream.video.android.compose.ui.components.base.icons.IconStyleContainer
import io.getstream.video.android.compose.ui.components.base.texts.StreamTextStyle

public object DefaultStreamButtonStyles: StreamButtonStyles()

@Immutable
public open class StreamButtonStyle(
    public val modifier: Modifier,
    public val elevation: ButtonElevation?,
    public val shape: Shape,
    public val border: BorderStroke?,
    public val colors: ButtonColors,
    public val contentPadding: PaddingValues,
    public val textStyle: StreamTextStyle,
    public val iconStyle: IconStyleContainer,
) {

    /**
     * Standard copy function, to utilize as much as possible from the companion object.
     */
    @Composable
    public fun copy(
        modifier: Modifier = this.modifier,
        elevation: ButtonElevation? = this.elevation,
        shape: Shape = this.shape,
        border: BorderStroke? = this.border,
        colors: ButtonColors = this.colors,
        contentPadding: PaddingValues = this.contentPadding,
        textStyle: TextStyle = this.textStyle,
        iconStyle: IconStyle = this.iconStyle
    ): StreamButtonStyle {
        return StreamButtonStyle(
            modifier, elevation, shape, border, colors, contentPadding, textStyle, iconStyle
        )
    }
}

@Immutable
public open class StreamButtonStyles {

    /**
     * You can create any style with the [StreamButtonStyle].
     * Use it with the [StreamButton] composable
     *
     * @param elevation button elevation
     * @param shape the shape of the button
     * @param border the button border
     * @param
     */
    @Composable
    public fun genericButtonStyle(
        modifier: Modifier = Modifier,
        elevation: ButtonElevation? = null,
        shape: Shape = VideoTheme.shapes.buttonShape,
        border: BorderStroke? = null,
        colors: ButtonColors = ButtonDefaults.buttonColors(),
        contentPadding: PaddingValues = ButtonDefaults.ContentPadding
    ): StreamButtonStyle = StreamButtonStyle(
        modifier,
        elevation,
        shape,
        border,
        colors,
        contentPadding,
        VideoTheme.typography.label,
        DefaultIconStyles.iconStyle()
    )

    @Composable
    public fun primaryButtonStyle(modifier: Modifier = Modifier): StreamButtonStyle =
        genericButtonStyle(
            modifier = modifier,
            shape = VideoTheme.shapes.buttonShape,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = VideoTheme.colors.buttonPrimaryDefault,
                contentColor = VideoTheme.colors.basePrimary,
                disabledBackgroundColor = VideoTheme.colors.buttonPrimaryDisabled
            ),
            contentPadding = PaddingValues(
                start = VideoTheme.dimens.componentPaddingStart,
                end = VideoTheme.dimens.componentPaddingEnd,
                top = VideoTheme.dimens.componentPaddingTop,
                bottom = VideoTheme.dimens.componentPaddingBottom
            )
        )

    @Composable
    public fun secondaryButtonStyle(modifier: Modifier = Modifier): StreamButtonStyle =
        genericButtonStyle(modifier = modifier).copy(
            colors = ButtonDefaults.buttonColors(
                backgroundColor = VideoTheme.colors.buttonBrandDefault,
                contentColor = VideoTheme.colors.basePrimary,
                disabledBackgroundColor = VideoTheme.colors.buttonBrandDisabled
            )
        )

    @Composable
    public fun tetriaryButtonStyle(modifier: Modifier = Modifier): StreamButtonStyle =
        genericButtonStyle(modifier = modifier).copy(
            colors = ButtonDefaults.buttonColors(
                backgroundColor = VideoTheme.colors.baseSheetPrimary,
                contentColor = VideoTheme.colors.basePrimary,
                disabledBackgroundColor = VideoTheme.colors.baseSheetPrimary
            ), border = BorderStroke(1.dp, VideoTheme.colors.baseSenary)
        )
}