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

package io.getstream.video.android.compose.ui.components.base.styling

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme

public object ButtonStyles : ButtonStyleProvider()

@Immutable
public open class StreamButtonStyle(
    public val elevation: ButtonElevation?,
    public val shape: Shape,
    public val border: BorderStroke?,
    public val colors: ButtonColors,
    public val contentPadding: PaddingValues,
    public val textStyle: StreamTextStyle,
    public val iconStyle: StreamIconStyle,
    public val drawableStyle: StreamButtonDrawableStyle,
) {

    /**
     * Standard copy function, to utilize as much as possible from the companion object.
     */
    public fun copy(
        elevation: ButtonElevation? = this.elevation,
        shape: Shape = this.shape,
        border: BorderStroke? = this.border,
        colors: ButtonColors = this.colors,
        contentPadding: PaddingValues = this.contentPadding,
        textStyle: StreamTextStyle = this.textStyle,
        iconStyle: StreamIconStyle = this.iconStyle,
        drawableStyle: StreamButtonDrawableStyle = this.drawableStyle,
    ): StreamButtonStyle {
        return StreamButtonStyle(
            elevation,
            shape,
            border,
            colors,
            contentPadding,
            textStyle,
            iconStyle,
            drawableStyle,
        )
    }
}

/**
 * A style that also suggest a fixed size. Size  is not guaranteed and depends on the implementing composable.
 */
public open class StreamFixedSizeButtonStyle(
    public val width: Dp,
    public val height: Dp,
    elevation: ButtonElevation?,
    shape: Shape,
    border: BorderStroke?,
    colors: ButtonColors,
    contentPadding: PaddingValues,
    textStyle: StreamTextStyle,
    iconStyle: StreamIconStyle,
    drawableStyle: StreamButtonDrawableStyle,
) : StreamButtonStyle(
    elevation,
    shape,
    border,
    colors,
    contentPadding,
    textStyle,
    iconStyle,
    drawableStyle,
) {

    public fun copyFixed(
        width: Dp = this.width,
        height: Dp = this.height,
        elevation: ButtonElevation? = this.elevation,
        shape: Shape = this.shape,
        border: BorderStroke? = this.border,
        colors: ButtonColors = this.colors,
        contentPadding: PaddingValues = this.contentPadding,
        textStyle: StreamTextStyle = this.textStyle,
        iconStyle: StreamIconStyle = this.iconStyle,
        drawableStyle: StreamButtonDrawableStyle = this.drawableStyle,
    ): StreamFixedSizeButtonStyle = StreamFixedSizeButtonStyle(
        width,
        height,
        elevation,
        shape,
        border,
        colors,
        contentPadding,
        textStyle,
        iconStyle,
        drawableStyle,
    )
    public companion object {
        public fun of(
            width: Dp,
            height: Dp,
            origin: StreamButtonStyle,
        ): StreamFixedSizeButtonStyle = StreamFixedSizeButtonStyle(
            width,
            height,
            origin.elevation,
            origin.shape,
            origin.border,
            origin.colors,
            origin.contentPadding,
            origin.textStyle,
            origin.iconStyle,
            origin.drawableStyle,
        )
    }
}

@Immutable
public open class ButtonStyleProvider {

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
        size: StyleSize = StyleSize.L,
        elevation: ButtonElevation? = null,
        shape: Shape = VideoTheme.shapes.button,
        border: BorderStroke? = null,
        colors: ButtonColors = ButtonDefaults.buttonColors(),
        contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    ): StreamButtonStyle = StreamButtonStyle(
        elevation,
        shape,
        border,
        colors,
        contentPadding,
        StreamTextStyles.defaultButtonLabel(size),
        IconStyles.defaultIconStyle(),
        ButtonDrawableStyles.defaultButtonDrawableStyle(),
    )

    @Composable
    public fun primaryButtonStyle(size: StyleSize = StyleSize.L): StreamButtonStyle =
        genericButtonStyle(
            size = size,
            shape = VideoTheme.shapes.button,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = VideoTheme.colors.buttonPrimaryDefault,
                contentColor = VideoTheme.colors.basePrimary,
                disabledBackgroundColor = VideoTheme.colors.buttonPrimaryDisabled,
            ),
            contentPadding = PaddingValues(
                start = VideoTheme.dimens.componentPaddingStart,
                end = VideoTheme.dimens.componentPaddingEnd,
                top = VideoTheme.dimens.componentPaddingTop,
                bottom = VideoTheme.dimens.componentPaddingBottom,
            ),
        )

    @Composable
    public fun secondaryButtonStyle(size: StyleSize = StyleSize.L): StreamButtonStyle =
        genericButtonStyle(size = size).copy(
            colors = ButtonDefaults.buttonColors(
                backgroundColor = VideoTheme.colors.buttonBrandDefault,
                contentColor = VideoTheme.colors.basePrimary,
                disabledBackgroundColor = VideoTheme.colors.buttonBrandDisabled,
            ),
        )

    @Composable
    public fun tertiaryButtonStyle(size: StyleSize = StyleSize.L): StreamButtonStyle =
        genericButtonStyle(size = size).copy(
            colors = ButtonDefaults.buttonColors(
                backgroundColor = VideoTheme.colors.baseSheetPrimary,
                contentColor = VideoTheme.colors.basePrimary,
                disabledBackgroundColor = VideoTheme.colors.baseSheetPrimary,
            ),
            border = BorderStroke(1.dp, VideoTheme.colors.baseSenary),
        )

    @Composable
    public fun toggleButtonStyleOn(size: StyleSize = StyleSize.L): StreamButtonStyle =
        genericButtonStyle(size = size).copy(
            colors = ButtonDefaults.buttonColors(
                backgroundColor = VideoTheme.colors.buttonPrimaryDefault,
                contentColor = VideoTheme.colors.basePrimary,
                disabledBackgroundColor = VideoTheme.colors.buttonPrimaryDisabled,
            ),
            iconStyle = IconStyles.customColorIconStyle(
                color = VideoTheme.colors.brandPrimary,
            ),
        )

    @Composable
    public fun toggleButtonStyleOff(size: StyleSize = StyleSize.L): StreamButtonStyle =
        genericButtonStyle(size = size).copy(
            colors = ButtonDefaults.buttonColors(
                backgroundColor = VideoTheme.colors.baseSheetPrimary,
                contentColor = VideoTheme.colors.basePrimary,
                disabledBackgroundColor = VideoTheme.colors.baseSheetPrimary,
            ),
        )

    @Composable
    public fun alertButtonStyle(size: StyleSize = StyleSize.L): StreamButtonStyle =
        genericButtonStyle(size = size).copy(
            colors = ButtonDefaults.buttonColors(
                backgroundColor = VideoTheme.colors.buttonAlertDefault,
                contentColor = VideoTheme.colors.basePrimary,
                disabledBackgroundColor = VideoTheme.colors.buttonAlertDisabled,
            ),
        )

    @Composable
    public fun primaryIconButtonStyle(size: StyleSize = StyleSize.L): StreamFixedSizeButtonStyle =
        StreamFixedSizeButtonStyle.of(
            width = when (size) {
                StyleSize.XS, StyleSize.S -> VideoTheme.dimens.componentHeightS
                StyleSize.M -> VideoTheme.dimens.componentHeightM
                else -> VideoTheme.dimens.componentHeightL
            },
            height = when (size) {
                StyleSize.XS, StyleSize.S -> VideoTheme.dimens.componentHeightS
                StyleSize.M -> VideoTheme.dimens.componentHeightM
                else -> VideoTheme.dimens.componentHeightL
            },
            origin = primaryButtonStyle(size = size).copy(
                iconStyle = IconStyles.defaultIconStyle(
                    padding = PaddingValues(VideoTheme.dimens.spacingXs),
                ),
            ),
        )

    @Composable
    public fun secondaryIconButtonStyle(size: StyleSize = StyleSize.L): StreamFixedSizeButtonStyle =
        StreamFixedSizeButtonStyle.of(
            width = when (size) {
                StyleSize.XS, StyleSize.S -> VideoTheme.dimens.componentHeightS
                StyleSize.M -> VideoTheme.dimens.componentHeightM
                else -> VideoTheme.dimens.componentHeightL
            },
            height = when (size) {
                StyleSize.XS, StyleSize.S -> VideoTheme.dimens.componentHeightS
                StyleSize.M -> VideoTheme.dimens.componentHeightM
                else -> VideoTheme.dimens.componentHeightL
            },
            origin = secondaryButtonStyle(size = size).copy(
                iconStyle = IconStyles.defaultIconStyle(
                    padding = PaddingValues(VideoTheme.dimens.spacingXs),
                ),
            ),
        )

    @Composable
    public fun tertiaryIconButtonStyle(size: StyleSize = StyleSize.L): StreamFixedSizeButtonStyle =
        StreamFixedSizeButtonStyle.of(
            width = when (size) {
                StyleSize.XS, StyleSize.S -> VideoTheme.dimens.componentHeightS
                StyleSize.M -> VideoTheme.dimens.componentHeightM
                else -> VideoTheme.dimens.componentHeightL
            },
            height = when (size) {
                StyleSize.XS, StyleSize.S -> VideoTheme.dimens.componentHeightS
                StyleSize.M -> VideoTheme.dimens.componentHeightM
                else -> VideoTheme.dimens.componentHeightL
            },
            origin = tertiaryButtonStyle(size = size).copy(
                iconStyle = IconStyles.defaultIconStyle(
                    padding = PaddingValues(VideoTheme.dimens.spacingXs),
                ),
            ),
        )

    @Composable
    public fun onlyIconIconButtonStyle(size: StyleSize = StyleSize.L): StreamFixedSizeButtonStyle =
        StreamFixedSizeButtonStyle.of(
            width = when (size) {
                StyleSize.XS, StyleSize.S -> VideoTheme.dimens.componentHeightS
                StyleSize.M -> VideoTheme.dimens.componentHeightM
                else -> VideoTheme.dimens.componentHeightL
            },
            height = when (size) {
                StyleSize.XS, StyleSize.S -> VideoTheme.dimens.componentHeightS
                StyleSize.M -> VideoTheme.dimens.componentHeightM
                else -> VideoTheme.dimens.componentHeightL
            },
            origin = tertiaryButtonStyle(size = size).copy(
                iconStyle = IconStyles.defaultIconStyle(
                    padding = PaddingValues(VideoTheme.dimens.spacingXs),
                ),
                border = null,
            ),
        )

    @Composable
    public fun alertIconButtonStyle(size: StyleSize = StyleSize.L): StreamFixedSizeButtonStyle =
        StreamFixedSizeButtonStyle.of(
            width = when (size) {
                StyleSize.XS, StyleSize.S -> VideoTheme.dimens.componentHeightS
                StyleSize.M -> VideoTheme.dimens.componentHeightM
                else -> VideoTheme.dimens.componentHeightL
            },
            height = when (size) {
                StyleSize.XS, StyleSize.S -> VideoTheme.dimens.componentHeightS
                StyleSize.M -> VideoTheme.dimens.componentHeightM
                else -> VideoTheme.dimens.componentHeightL
            },
            origin = alertButtonStyle(size = size).copy(
                iconStyle = IconStyles.defaultIconStyle(
                    padding = PaddingValues(VideoTheme.dimens.spacingXs),
                ),
            ),
        )

    @Composable
    public fun primaryDrawableButtonStyle(
        size: StyleSize = StyleSize.L,
    ): StreamFixedSizeButtonStyle =
        StreamFixedSizeButtonStyle.of(
            width = when (size) {
                StyleSize.XS, StyleSize.S -> VideoTheme.dimens.componentHeightS
                StyleSize.M -> VideoTheme.dimens.componentHeightM
                else -> VideoTheme.dimens.componentHeightL
            },
            height = when (size) {
                StyleSize.XS, StyleSize.S -> VideoTheme.dimens.componentHeightS
                StyleSize.M -> VideoTheme.dimens.componentHeightM
                else -> VideoTheme.dimens.componentHeightL
            },
            origin = genericButtonStyle(size = size),
        )

    @Composable
    public fun drawableToggleButtonStyleOn(
        size: StyleSize = StyleSize.L,
    ): StreamFixedSizeButtonStyle =
        StreamFixedSizeButtonStyle.of(
            width = when (size) {
                StyleSize.XS, StyleSize.S -> VideoTheme.dimens.componentHeightS
                StyleSize.M -> VideoTheme.dimens.componentHeightM
                else -> VideoTheme.dimens.componentHeightL
            },
            height = when (size) {
                StyleSize.XS, StyleSize.S -> VideoTheme.dimens.componentHeightS
                StyleSize.M -> VideoTheme.dimens.componentHeightM
                else -> VideoTheme.dimens.componentHeightL
            },
            origin = genericButtonStyle(size = size).copy(
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = VideoTheme.colors.buttonPrimaryDefault,
                    contentColor = VideoTheme.colors.basePrimary,
                    disabledBackgroundColor = VideoTheme.colors.buttonPrimaryDisabled,
                ),
                drawableStyle = ButtonDrawableStyles.defaultButtonDrawableStyle(),
            ),
        )

    @Composable
    public fun drawableToggleButtonStyleOff(
        size: StyleSize = StyleSize.L,
    ): StreamFixedSizeButtonStyle =
        StreamFixedSizeButtonStyle.of(
            width = when (size) {
                StyleSize.XS, StyleSize.S -> VideoTheme.dimens.componentHeightS
                StyleSize.M -> VideoTheme.dimens.componentHeightM
                else -> VideoTheme.dimens.componentHeightL
            },
            height = when (size) {
                StyleSize.XS, StyleSize.S -> VideoTheme.dimens.componentHeightS
                StyleSize.M -> VideoTheme.dimens.componentHeightM
                else -> VideoTheme.dimens.componentHeightL
            },
            origin = genericButtonStyle(size = size).copy(
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = VideoTheme.colors.baseSheetPrimary,
                    contentColor = VideoTheme.colors.basePrimary,
                    disabledBackgroundColor = VideoTheme.colors.baseSheetPrimary,
                ),
                drawableStyle = ButtonDrawableStyles.customColorFilterButtonDrawableStyle(
                    colorFilter = ColorFilter.lighting(Color.Gray, Color.Transparent),
                ),
            ),
        )
}

internal fun StreamFixedSizeButtonStyle.fillCircle(fraction: Float): StreamFixedSizeButtonStyle {
    return this.copyFixed(
        width * fraction,
        height * fraction,
        shape = CircleShape,
    )
}
