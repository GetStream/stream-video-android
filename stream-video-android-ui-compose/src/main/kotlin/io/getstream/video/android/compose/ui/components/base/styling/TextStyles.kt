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

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import io.getstream.video.android.compose.theme.VideoTheme

/**
 * Wrapper for the platform text style.
 */
public data class TextStyleWrapper(
    public val platform: TextStyle,
) : StreamStyle

/**
 * Stream text style
 */
public data class StreamTextStyle(
    override val default: TextStyleWrapper,
    override val disabled: TextStyleWrapper,
    override val pressed: TextStyleWrapper,
) : StreamStateStyle<TextStyleWrapper>

public open class TextStyleProvider {

    @Composable
    public fun defaultLabel(
        size: StyleSize = StyleSize.L,
        default: TextStyleWrapper = when (size) {
            StyleSize.XS, StyleSize.S -> VideoTheme.typography.labelS.wrapper()
            StyleSize.M -> VideoTheme.typography.labelM.wrapper()
            else -> VideoTheme.typography.labelL.wrapper()
        },
        pressed: TextStyleWrapper = default,
        disabled: TextStyleWrapper = default.disabledAlpha(),
    ): StreamTextStyle = StreamTextStyle(default, disabled, pressed)

    @Composable
    public fun defaultButtonLabel(
        size: StyleSize = StyleSize.L,
        default: TextStyleWrapper = when (size) {
            StyleSize.XS, StyleSize.S -> VideoTheme.typography.labelXS.wrapper()
            StyleSize.M -> VideoTheme.typography.labelS.wrapper()
            else -> VideoTheme.typography.labelM.wrapper()
        },
        pressed: TextStyleWrapper = default,
        disabled: TextStyleWrapper = default.disabledAlpha(),
    ): StreamTextStyle = StreamTextStyle(default, disabled, pressed)

    @Composable
    public fun defaultTitle(
        size: StyleSize = StyleSize.L,
        default: TextStyleWrapper = when (size) {
            StyleSize.XS -> VideoTheme.typography.titleXs.wrapper()
            StyleSize.S -> VideoTheme.typography.titleS.wrapper()
            StyleSize.M -> VideoTheme.typography.titleM.wrapper()
            else -> VideoTheme.typography.titleL.wrapper()
        },
        pressed: TextStyleWrapper = default,
        disabled: TextStyleWrapper = default.disabledAlpha(),
    ): StreamTextStyle = StreamTextStyle(default, disabled, pressed)

    @Composable
    public fun defaultSubtitle(
        size: StyleSize = StyleSize.M,
        default: TextStyleWrapper = when (size) {
            StyleSize.XS -> VideoTheme.typography.subtitleS.wrapper()
            StyleSize.S -> VideoTheme.typography.subtitleS.wrapper()
            StyleSize.M -> VideoTheme.typography.subtitleM.wrapper()
            else -> VideoTheme.typography.subtitleL.wrapper()
        },
        pressed: TextStyleWrapper = default,
        disabled: TextStyleWrapper = default.disabledAlpha(),
    ): StreamTextStyle = StreamTextStyle(default, disabled, pressed)

    @Composable
    public fun defaultBody(
        size: StyleSize = StyleSize.L,
        default: TextStyleWrapper = when (size) {
            StyleSize.XS, StyleSize.S, StyleSize.M -> VideoTheme.typography.bodyM.wrapper()
            else -> VideoTheme.typography.bodyL.wrapper()
        },
        pressed: TextStyleWrapper = default,
        disabled: TextStyleWrapper = default.disabledAlpha(),
    ): StreamTextStyle = StreamTextStyle(default, disabled, pressed)

    @Composable
    public fun defaultBadgeTextStyle(
        default: TextStyleWrapper = VideoTheme.typography.labelXS.wrapper(),
        pressed: TextStyleWrapper = default,
        disabled: TextStyleWrapper = default.disabledAlpha(),
    ): StreamTextStyle = StreamTextStyle(default, disabled, pressed)

    @Composable
    public fun defaultTextField(
        size: StyleSize = StyleSize.M,
        default: TextStyleWrapper = when (size) {
            StyleSize.XS -> VideoTheme.typography.subtitleS.withColor(VideoTheme.colors.basePrimary)
            StyleSize.S -> VideoTheme.typography.subtitleS.withColor(VideoTheme.colors.basePrimary)
            StyleSize.M -> VideoTheme.typography.subtitleM.withColor(VideoTheme.colors.basePrimary)
            else -> VideoTheme.typography.subtitleL.withColor(VideoTheme.colors.basePrimary)
        },
        pressed: TextStyleWrapper = default,
        disabled: TextStyleWrapper = default.disabledAlpha(),
    ): StreamTextStyle = StreamTextStyle(default, disabled, pressed)
}

public object StreamTextStyles : TextStyleProvider()

// Utilities
internal fun TextStyle.wrapper(): TextStyleWrapper = TextStyleWrapper(platform = this)

internal fun TextStyle.withColor(color: Color) = TextStyleWrapper(
    platform = this.copy(
        color = color,
    ),
)

internal fun TextStyleWrapper.withAlpha(alpha: Float): TextStyleWrapper = this.platform.copy(
    color = this.platform.color.copy(
        alpha = alpha,
    ),
).wrapper()

internal fun TextStyleWrapper.disabledAlpha() = this.withAlpha(0.16f)
