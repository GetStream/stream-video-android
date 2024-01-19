/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.theme.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.getstream.video.android.compose.utils.textSizeResource
import io.getstream.video.android.ui.common.R

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
    public val titleMedium: TextStyle,
    public val title3: TextStyle,
    public val title3Bold: TextStyle,
    public val body: TextStyle,
    public val bodyItalic: TextStyle,
    public val bodyBold: TextStyle,
    public val footnote: TextStyle,
    public val footnoteItalic: TextStyle,
    public val footnoteBold: TextStyle,
    public val captionBold: TextStyle,
    public val tabBar: TextStyle,
    public val label: TextStyle,
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
            titleMedium = TextStyle(
                fontSize = dimens.textSizeL,
                lineHeight = dimens.lineHeightXl,
                fontFamily = fontFamily,
                fontWeight = FontWeight.W500,
                color = colors.basePrimary,
                textAlign = TextAlign.Center,
            ),
            title3 = TextStyle(
                fontSize = textSizeResource(id = R.dimen.stream_video_title3TextSize),
                lineHeight = textSizeResource(id = R.dimen.stream_video_title3LineHeight),
                fontWeight = FontWeight.W400,
                fontFamily = fontFamily,
            ),
            title3Bold = TextStyle(
                fontSize = textSizeResource(id = R.dimen.stream_video_title3TextSize),
                lineHeight = textSizeResource(id = R.dimen.stream_video_title3LineHeight),
                fontWeight = FontWeight.W500,
                fontFamily = fontFamily,
            ),
            body = TextStyle(
                fontSize = textSizeResource(id = R.dimen.stream_video_bodyTextSize),
                fontWeight = FontWeight.W400,
                fontFamily = fontFamily,
            ),
            bodyItalic = TextStyle(
                fontSize = textSizeResource(id = R.dimen.stream_video_bodyTextSize),
                fontWeight = FontWeight.W400,
                fontStyle = FontStyle.Italic,
                fontFamily = fontFamily,
            ),
            bodyBold = TextStyle(
                fontSize = textSizeResource(id = R.dimen.stream_video_bodyTextSize),
                fontWeight = FontWeight.W500,
                fontFamily = fontFamily,
            ),
            footnote = TextStyle(
                fontSize = textSizeResource(id = R.dimen.stream_video_footnoteTextSize),
                lineHeight = textSizeResource(id = R.dimen.stream_video_footnoteLineHeight),
                fontWeight = FontWeight.W400,
                fontFamily = fontFamily,
            ),
            footnoteItalic = TextStyle(
                fontSize = textSizeResource(id = R.dimen.stream_video_footnoteTextSize),
                lineHeight = textSizeResource(id = R.dimen.stream_video_footnoteLineHeight),
                fontWeight = FontWeight.W400,
                fontStyle = FontStyle.Italic,
                fontFamily = fontFamily,
            ),
            footnoteBold = TextStyle(
                fontSize = textSizeResource(id = R.dimen.stream_video_footnoteTextSize),
                lineHeight = textSizeResource(id = R.dimen.stream_video_footnoteLineHeight),
                fontWeight = FontWeight.W500,
                fontFamily = fontFamily,
            ),
            captionBold = TextStyle(
                fontSize = textSizeResource(id = R.dimen.stream_video_captionTextSize),
                lineHeight = textSizeResource(id = R.dimen.stream_video_captionLineHeight),
                fontWeight = FontWeight.W700,
                fontFamily = fontFamily,
            ),
            tabBar = TextStyle(
                fontSize = textSizeResource(id = R.dimen.stream_video_tabBarTextSize),
                fontWeight = FontWeight.W400,
                fontFamily = fontFamily,
            ),
            label = TextStyle(
                fontSize = dimens.textSizeM,
                lineHeight = dimens.lineHeightL,
                fontWeight = FontWeight.W600,
                color = colors.basePrimary,
            )
        )
    }
}
