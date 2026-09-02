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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.theme.design.StreamTokens

public data class TextFieldStyle(
    val textStyle: StreamTextStyle,
    val placeholderStyle: StreamTextStyle,
    val iconStyle: StreamIconStyle,
    val colors: TextFieldColors,
    val shape: Shape,
    val borderStroke: BorderStroke?,
    val paddings: PaddingValues,
) : StreamStyle

public open class TextFieldStyleProvider {

    @Composable
    public fun defaultTextField(
        styleSize: StyleSize = StyleSize.S,
        textStyle: StreamTextStyle = StreamTextStyles.defaultTextField(styleSize),
        placeholderStyle: StreamTextStyle = StreamTextStyles.defaultSubtitle(styleSize),
        iconStyle: StreamIconStyle = IconStyles.defaultIconStyle(),
    ): TextFieldStyle =
        TextFieldStyle(
            textStyle = textStyle,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                // Background
                backgroundColor = VideoTheme.colors.backgroundCoreApp,
                // Border
                focusedBorderColor = VideoTheme.colors.accentPrimary,
                unfocusedBorderColor = VideoTheme.colors.borderCoreDefault,
                disabledBorderColor = VideoTheme.colors.borderCoreDefault.copy(alpha = 0.16f),
                errorBorderColor = VideoTheme.colors.accentError,
                // Cursor
                cursorColor = VideoTheme.colors.textPrimary,
                errorCursorColor = VideoTheme.colors.accentError,
                // Text
                textColor = VideoTheme.colors.textPrimary,
                disabledTextColor = VideoTheme.colors.textTertiary.copy(alpha = 0.16f),
                errorLabelColor = VideoTheme.colors.accentError,
                focusedLabelColor = VideoTheme.colors.textPrimary,
                unfocusedLabelColor = VideoTheme.colors.textPrimary,
                placeholderColor = VideoTheme.colors.textTertiary,
                disabledPlaceholderColor = VideoTheme.colors.textTertiary.copy(alpha = 0.16f),
            ),
            borderStroke = BorderStroke(2.dp, VideoTheme.colors.borderCoreDefault),
            shape = RoundedCornerShape(StreamTokens.inputRadiusTextInput),
            placeholderStyle = placeholderStyle,
            paddings = PaddingValues(StreamTokens.spacingXs),
            iconStyle = iconStyle,
        )
}

public object StreamTextFieldStyles : TextFieldStyleProvider()
