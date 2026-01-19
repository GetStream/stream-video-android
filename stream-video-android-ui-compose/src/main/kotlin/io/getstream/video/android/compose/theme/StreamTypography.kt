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

package io.getstream.video.android.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Contains all the typography we provide for our components.
 *
 * @param title1 Used for big titles, like the image attachment overlay text.
 * @param title3 Used for empty content text.
 * @param title3Bold Used for titles of app bars and bottom bars.
 * @param body Used for body content, such as messages.
 * @param bodyItalic Used for body content, italicized, like deleted message components.
 * @param bodyBold Used for emphasized body content, like small titles.
 * @param footnote Used for footnote information, like timestamps.
 * @param footnoteItalic Used for footnote information that's less important, like the deleted message text.
 * @param footnoteBold Used for footnote information in certain important items, like the thread reply text,
 * or user info components.
 * @param captionBold Used for unread count indicator.
 */
@Immutable
public data class StreamTypography(
    public val titleL: TextStyle,
    public val titleM: TextStyle,
    public val titleS: TextStyle,
    public val titleXs: TextStyle,
    public val subtitleL: TextStyle,
    public val subtitleM: TextStyle,
    public val subtitleS: TextStyle,
    public val bodyL: TextStyle,
    public val bodyM: TextStyle,
    public val bodyS: TextStyle,
    public val labelL: TextStyle,
    public val labelM: TextStyle,
    public val labelS: TextStyle,
    public val labelXS: TextStyle,
) {

    public companion object {
        /**
         * Builds the default typography set for our theme, with the ability to customize the font family.
         *
         * @param fontFamily The font that the users want to use for the app.
         * @return [StreamTypography] that holds all the default text styles that we support.
         */
        @Composable
        public fun defaultTypography(
            colors: StreamColors,
            dimens: StreamDimens,
            fontFamily: FontFamily? = null,
        ): StreamTypography = StreamTypography(
            titleL = TextStyle(
                fontSize = dimens.textSizeXxl,
                lineHeight = dimens.lineHeightXxl,
                fontFamily = fontFamily,
                fontWeight = FontWeight.W500,
                color = colors.basePrimary,
            ),
            titleM = TextStyle(
                fontSize = dimens.textSizeXl,
                lineHeight = dimens.lineHeightXl,
                fontFamily = fontFamily,
                fontWeight = FontWeight.W500,
                color = colors.basePrimary,
            ),
            titleS = TextStyle(
                fontSize = dimens.textSizeL,
                lineHeight = dimens.lineHeightL,
                fontFamily = fontFamily,
                fontWeight = FontWeight.W500,
                color = colors.basePrimary,
            ),
            subtitleL = TextStyle(
                fontSize = dimens.textSizeL,
                lineHeight = dimens.lineHeightM,
                fontFamily = fontFamily,
                fontWeight = FontWeight.W500,
                color = colors.baseTertiary,
            ),
            subtitleM = TextStyle(
                fontSize = dimens.textSizeM,
                lineHeight = dimens.lineHeightS,
                fontFamily = fontFamily,
                fontWeight = FontWeight.W500,
                color = colors.baseTertiary,
            ),
            subtitleS = TextStyle(
                fontSize = dimens.textSizeS,
                lineHeight = dimens.lineHeightXs,
                fontFamily = fontFamily,
                fontWeight = FontWeight.W500,
                color = colors.baseTertiary,
            ),
            titleXs = TextStyle(
                fontSize = dimens.textSizeXs,
                lineHeight = dimens.lineHeightM,
                fontFamily = fontFamily,
                fontWeight = FontWeight.W600,
                color = colors.baseQuinary,
                letterSpacing = 1.sp,
            ),
            bodyL = TextStyle(
                fontSize = dimens.textSizeM,
                lineHeight = dimens.lineHeightXl,
                fontFamily = fontFamily,
                fontWeight = FontWeight.W400,
                color = colors.baseQuinary,
            ),
            bodyM = TextStyle(
                fontSize = dimens.textSizeS,
                lineHeight = dimens.lineHeightL,
                fontFamily = fontFamily,
                fontWeight = FontWeight.W400,
                color = colors.baseQuinary,
            ),
            bodyS = TextStyle(
                fontSize = dimens.textSizeXs,
                lineHeight = dimens.lineHeightM,
                fontFamily = fontFamily,
                fontWeight = FontWeight.W400,
                color = colors.baseQuinary,
            ),
            labelL = TextStyle(
                fontSize = dimens.textSizeM,
                lineHeight = dimens.lineHeightL,
                fontFamily = fontFamily,
                fontWeight = FontWeight.W600,
                color = colors.basePrimary,
            ),
            labelM = TextStyle(
                fontSize = dimens.textSizeS,
                lineHeight = dimens.lineHeightL,
                fontFamily = fontFamily,
                fontWeight = FontWeight.W600,
                color = colors.basePrimary,
            ),
            labelS = TextStyle(
                fontSize = dimens.textSizeXs,
                lineHeight = dimens.lineHeightS,
                fontFamily = fontFamily,
                fontWeight = FontWeight.W500,
                color = colors.basePrimary,
            ),
            labelXS = TextStyle(
                fontSize = dimens.textSizeXs,
                lineHeight = dimens.lineHeightXs,
                fontFamily = fontFamily,
                fontWeight = FontWeight.W500,
                color = colors.basePrimary,
            ),
        )
    }
}
