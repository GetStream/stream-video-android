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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import io.getstream.video.android.compose.theme.VideoTheme

public data class DialogStyle(
    public val shape: Shape,
    public val backgroundColor: Color,
    public val titleStyle: TextStyle,
    public val contentTextStyle: TextStyle,
    public val iconStyle: IconStyle,
    public val contentPaddings: PaddingValues,
) : StreamStyle

public open class DialogStyleProvider {
    @Composable
    public fun defaultDialogStyle(): DialogStyle = DialogStyle(
        shape = VideoTheme.shapes.dialog,
        backgroundColor = VideoTheme.colors.baseSheetSecondary,
        titleStyle = StreamTextStyles.defaultTitle(StyleSize.S).default.platform,
        contentTextStyle = StreamTextStyles.defaultBody(StyleSize.S).default.platform,
        iconStyle = IconStyles.defaultIconStyle().default,
        contentPaddings = PaddingValues(VideoTheme.dimens.spacingL),
    )
}

public object StreamDialogStyles : DialogStyleProvider()
