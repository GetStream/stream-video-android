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

package io.getstream.video.android.compose.ui.components.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import io.getstream.video.android.compose.theme.VideoTheme

/**
 * Colors of a [StreamButton] in its enabled and disabled states.
 *
 * A null container or border color means that part is not drawn, which is how the outline
 * and ghost variants are expressed.
 *
 * @param containerColor The background color when enabled, or null for no background.
 * @param contentColor The icon and label color when enabled.
 * @param borderColor The outline color when enabled, or null for no outline.
 * @param disabledContainerColor The background color when disabled, or null for no background.
 * @param disabledContentColor The icon and label color when disabled.
 * @param disabledBorderColor The outline color when disabled, or null for no outline.
 */
@Immutable
public data class StreamButtonStyle(
    val containerColor: Color?,
    val contentColor: Color,
    val borderColor: Color?,
    val disabledContainerColor: Color?,
    val disabledContentColor: Color,
    val disabledBorderColor: Color?,
)

@Stable
internal fun StreamButtonStyle.contentColor(enabled: Boolean): Color =
    if (enabled) contentColor else disabledContentColor

@Stable
internal fun StreamButtonStyle.containerColor(enabled: Boolean): Color? =
    if (enabled) containerColor else disabledContainerColor

@Stable
internal fun StreamButtonStyle.borderColor(enabled: Boolean): Color? =
    if (enabled) borderColor else disabledBorderColor

/**
 * The [StreamButtonStyle] variants of the design system, resolved from [VideoTheme.colors].
 *
 * Each intent (primary, secondary, destructive) comes in a solid, an outline and a ghost variant.
 */
public object StreamButtonStyleDefaults {

    /** Filled button with the brand color. The default call to action. */
    public val primarySolid: StreamButtonStyle
        @Composable
        get() {
            val colors = VideoTheme.colors
            return StreamButtonStyle(
                containerColor = colors.buttonPrimaryBg,
                contentColor = colors.buttonPrimaryTextOnAccent,
                borderColor = null,
                disabledContainerColor = colors.backgroundUtilityDisabled,
                disabledContentColor = colors.textDisabled,
                disabledBorderColor = null,
            )
        }

    /** Outlined button with the brand color. */
    public val primaryOutline: StreamButtonStyle
        @Composable
        get() {
            val colors = VideoTheme.colors
            return StreamButtonStyle(
                containerColor = null,
                contentColor = colors.buttonPrimaryText,
                borderColor = colors.buttonPrimaryBorder,
                disabledContainerColor = null,
                disabledContentColor = colors.textDisabled,
                disabledBorderColor = colors.borderUtilityDisabled,
            )
        }

    /** Text-only button with the brand color. */
    public val primaryGhost: StreamButtonStyle
        @Composable
        get() {
            val colors = VideoTheme.colors
            return StreamButtonStyle(
                containerColor = null,
                contentColor = colors.buttonPrimaryText,
                borderColor = null,
                disabledContainerColor = null,
                disabledContentColor = colors.textDisabled,
                disabledBorderColor = null,
            )
        }

    /** Filled button on a neutral surface. The default style of the call controls. */
    public val secondarySolid: StreamButtonStyle
        @Composable
        get() {
            val colors = VideoTheme.colors
            return StreamButtonStyle(
                containerColor = colors.buttonSecondaryBg,
                contentColor = colors.buttonSecondaryText,
                borderColor = null,
                disabledContainerColor = colors.backgroundUtilityDisabled,
                disabledContentColor = colors.textDisabled,
                disabledBorderColor = null,
            )
        }

    /** Outlined neutral button. */
    public val secondaryOutline: StreamButtonStyle
        @Composable
        get() {
            val colors = VideoTheme.colors
            return StreamButtonStyle(
                containerColor = null,
                contentColor = colors.buttonSecondaryText,
                borderColor = colors.buttonSecondaryBorder,
                disabledContainerColor = null,
                disabledContentColor = colors.textDisabled,
                disabledBorderColor = colors.borderUtilityDisabled,
            )
        }

    /** Text-only neutral button. */
    public val secondaryGhost: StreamButtonStyle
        @Composable
        get() {
            val colors = VideoTheme.colors
            return StreamButtonStyle(
                containerColor = null,
                contentColor = colors.buttonSecondaryText,
                borderColor = null,
                disabledContainerColor = null,
                disabledContentColor = colors.textDisabled,
                disabledBorderColor = null,
            )
        }

    /** Filled button with the error color, for leaving or ending a call. */
    public val destructiveSolid: StreamButtonStyle
        @Composable
        get() {
            val colors = VideoTheme.colors
            return StreamButtonStyle(
                containerColor = colors.buttonDestructiveBg,
                contentColor = colors.buttonDestructiveTextOnAccent,
                borderColor = null,
                disabledContainerColor = colors.backgroundUtilityDisabled,
                disabledContentColor = colors.textDisabled,
                disabledBorderColor = null,
            )
        }

    /** Outlined button with the error color. */
    public val destructiveOutline: StreamButtonStyle
        @Composable
        get() {
            val colors = VideoTheme.colors
            return StreamButtonStyle(
                containerColor = null,
                contentColor = colors.buttonDestructiveText,
                borderColor = colors.buttonDestructiveBorder,
                disabledContainerColor = null,
                disabledContentColor = colors.textDisabled,
                disabledBorderColor = colors.borderUtilityDisabled,
            )
        }

    /** Text-only button with the error color. */
    public val destructiveGhost: StreamButtonStyle
        @Composable
        get() {
            val colors = VideoTheme.colors
            return StreamButtonStyle(
                containerColor = null,
                contentColor = colors.buttonDestructiveText,
                borderColor = null,
                disabledContainerColor = null,
                disabledContentColor = colors.textDisabled,
                disabledBorderColor = null,
            )
        }
}
