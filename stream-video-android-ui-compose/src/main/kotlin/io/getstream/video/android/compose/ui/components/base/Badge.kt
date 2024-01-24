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

package io.getstream.video.android.compose.ui.components.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.v2.VideoTheme
import io.getstream.video.android.compose.ui.components.base.styling.BadgeStyle
import io.getstream.video.android.compose.ui.components.base.styling.DefaultStreamButtonStyles
import io.getstream.video.android.compose.ui.components.base.styling.StreamBadgeStyles

@Composable
public fun StreamBadgeOn(
    modifier: Modifier = Modifier,
    text: String? = null,
    style: BadgeStyle,
    content: @Composable BoxScope.(Modifier) -> Unit,
) {
    WithBadge(text = text, style = style) {
        content(modifier)
    }
}

@Composable
private fun WithBadge(
    modifier: Modifier = Modifier,
    style: BadgeStyle,
    text: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        content()
        // Badge content
        BadgeContent(style = style, text = text)
    }
}

@Composable
private fun BoxScope.BadgeContent(style: BadgeStyle, text: String? = null) {
    Box(
        modifier = Modifier
            .size(style.size)
            .align(Alignment.TopEnd)
            .background(style.color, CircleShape),
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center).wrapContentSize(),
            text = text ?: "",
            style = style.textStyle,
        )
    }
}

@Preview
@Composable
private fun ButtonWithBadgePreview() {
    VideoTheme {
        Column {
            StreamBadgeOn(
                text = "!",
                style = StreamBadgeStyles.defaultBadgeStyle(),
            ) {
                StreamIconButton(
                    icon = Icons.Default.AddAlert,
                    style = DefaultStreamButtonStyles.secondaryIconButtonStyle(),
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
            StreamBadgeOn(
                text = "10",
                style = StreamBadgeStyles.defaultBadgeStyle(),
            ) {
                StreamButton(
                    icon = Icons.Default.Info,
                    text = "Secondary Button",
                    style = DefaultStreamButtonStyles.secondaryButtonStyle(),
                )
            }
        }
    }
}
