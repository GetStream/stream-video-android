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
            StyleSize.XS, StyleSize.S -> VideoTheme.typography.captionEmphasis.withColor(
                VideoTheme.colors.textPrimary,
            )
            StyleSize.M -> VideoTheme.typography.bodyEmphasis.withColor(
                VideoTheme.colors.textPrimary,
            )
            else -> VideoTheme.typography.headingLarge.withColor(VideoTheme.colors.textPrimary)
        },
        pressed: TextStyleWrapper = default,
        disabled: TextStyleWrapper = default.disabledAlpha(),
    ): StreamTextStyle = StreamTextStyle(default, disabled, pressed)

    @Composable
    public fun defaultButtonLabel(
        size: StyleSize = StyleSize.L,
        default: TextStyleWrapper = when (size) {
            StyleSize.XS, StyleSize.S -> VideoTheme.typography.metadataEmphasis.withColor(
                VideoTheme.colors.textPrimary,
            )
            StyleSize.M -> VideoTheme.typography.captionEmphasis.withColor(
                VideoTheme.colors.textPrimary,
            )
            else -> VideoTheme.typography.bodyEmphasis.withColor(VideoTheme.colors.textPrimary)
        },
        pressed: TextStyleWrapper = default,
        disabled: TextStyleWrapper = default.disabledAlpha(),
    ): StreamTextStyle = StreamTextStyle(default, disabled, pressed)

    @Composable
    public fun defaultTitle(
        size: StyleSize = StyleSize.L,
        default: TextStyleWrapper = when (size) {
            StyleSize.XS -> VideoTheme.typography.headingExtraSmall.withColor(
                VideoTheme.colors.textPrimary,
            )
            else -> VideoTheme.typography.headingLarge.withColor(VideoTheme.colors.textPrimary)
        },
        pressed: TextStyleWrapper = default,
        disabled: TextStyleWrapper = default.disabledAlpha(),
    ): StreamTextStyle = StreamTextStyle(default, disabled, pressed)

    @Composable
    public fun defaultSubtitle(
        size: StyleSize = StyleSize.M,
        default: TextStyleWrapper = when (size) {
            StyleSize.XS, StyleSize.S -> VideoTheme.typography.bodyDefault.withColor(
                VideoTheme.colors.textSecondary,
            )
            StyleSize.M -> VideoTheme.typography.headingSmall.withColor(
                VideoTheme.colors.textTertiary,
            )
            else -> VideoTheme.typography.headingMedium.withColor(VideoTheme.colors.textTertiary)
        },
        pressed: TextStyleWrapper = default,
        disabled: TextStyleWrapper = default.disabledAlpha(),
    ): StreamTextStyle = StreamTextStyle(default, disabled, pressed)

    @Composable
    public fun defaultBody(
        size: StyleSize = StyleSize.L,
        default: TextStyleWrapper = VideoTheme.typography.bodyDefault.withColor(
            VideoTheme.colors.textSecondary,
        ),
        pressed: TextStyleWrapper = default,
        disabled: TextStyleWrapper = default.disabledAlpha(),
    ): StreamTextStyle = StreamTextStyle(default, disabled, pressed)

    @Composable
    public fun defaultBadgeTextStyle(
        default: TextStyleWrapper = VideoTheme.typography.metadataEmphasis.withColor(
            VideoTheme.colors.textPrimary,
        ),
        pressed: TextStyleWrapper = default,
        disabled: TextStyleWrapper = default.disabledAlpha(),
    ): StreamTextStyle = StreamTextStyle(default, disabled, pressed)

    @Composable
    public fun defaultTextField(
        size: StyleSize = StyleSize.M,
        default: TextStyleWrapper = when (size) {
            StyleSize.XS, StyleSize.S -> VideoTheme.typography.bodyDefault.withColor(
                VideoTheme.colors.textPrimary,
            )
            StyleSize.M -> VideoTheme.typography.headingSmall.withColor(
                VideoTheme.colors.textPrimary,
            )
            else -> VideoTheme.typography.headingMedium.withColor(VideoTheme.colors.textPrimary)
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
