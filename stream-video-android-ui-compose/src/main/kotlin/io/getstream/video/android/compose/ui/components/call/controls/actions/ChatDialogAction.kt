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

package io.getstream.video.android.compose.ui.components.call.controls.actions

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import io.getstream.video.android.compose.R
import io.getstream.video.android.compose.ui.components.base.StreamBadgeBox
import io.getstream.video.android.compose.ui.components.base.StreamButtonSize
import io.getstream.video.android.compose.ui.components.base.StreamButtonStyle
import io.getstream.video.android.compose.ui.components.base.StreamButtonStyleDefaults
import io.getstream.video.android.core.call.state.ChatDialog

/**
 * Opens the in-call chat, with an unread count badge.
 *
 * @param modifier The modifier applied to the button.
 * @param enabled Whether the action accepts clicks.
 * @param messageCount The unread message count shown in the badge, or null to hide the badge.
 * @param icon The icon of the action, or null for the default one.
 * @param style The colors of the button. See [StreamButtonStyleDefaults].
 * @param size The visual size of the button.
 * @param onCallAction Called with [ChatDialog] when the action is clicked.
 */
@Composable
public fun ChatDialogAction(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    messageCount: Int? = null,
    icon: Painter? = null,
    style: StreamButtonStyle = StreamButtonStyleDefaults.secondarySolid,
    size: StreamButtonSize = StreamButtonSize.Medium,
    onCallAction: (ChatDialog) -> Unit,
): Unit = StreamBadgeBox(
    showWithoutValue = false,
    text = messageCount?.toString(),
) {
    GenericAction(
        modifier = modifier,
        icon = icon ?: painterResource(R.drawable.stream_design_ic_message_bubbles_fill),
        enabled = enabled,
        style = style,
        size = size,
        onAction = { onCallAction(ChatDialog) },
    )
}
