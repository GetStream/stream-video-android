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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.VideoTheme

public data class BadgeStyle(
    public val size: Dp,
    public val color: Color,
    public val textStyle: TextStyle,
    public val contentPaddings: PaddingValues,
) : StreamStyle

public open class BadgeStyleProvider {
    @Composable
    public fun defaultBadgeStyle(): BadgeStyle = BadgeStyle(
        color = VideoTheme.colors.alertSuccess,
        size = 16.dp,
        textStyle = TextStyle(
            fontSize = 8.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.W600,
            color = VideoTheme.colors.baseTertiary,
        ),
        contentPaddings = PaddingValues(VideoTheme.dimens.genericXs, 0.dp),
    )
}

public object StreamBadgeStyles : BadgeStyleProvider()
