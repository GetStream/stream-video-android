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

import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import io.getstream.video.android.compose.theme.design.StreamTokens
import io.getstream.video.android.compose.ui.components.base.StreamButton
import io.getstream.video.android.compose.ui.components.base.StreamButtonSize
import io.getstream.video.android.compose.ui.components.base.StreamButtonStyle
import io.getstream.video.android.compose.ui.components.base.StreamButtonStyleDefaults
import io.getstream.video.android.compose.ui.components.base.StreamIconButton

/**
 * A round icon button used for one-shot call actions such as leaving the call.
 *
 * @param modifier The modifier applied to the button.
 * @param icon The icon of the action.
 * @param contentDescription The accessibility description of the action, or null when a parent describes it.
 * @param enabled Whether the action accepts clicks.
 * @param style The colors of the button. See [StreamButtonStyleDefaults].
 * @param size The visual size of the button.
 * @param onAction Called when the action is clicked.
 */
@Composable
public fun GenericAction(
    modifier: Modifier = Modifier,
    icon: Painter,
    contentDescription: String? = null,
    enabled: Boolean = true,
    style: StreamButtonStyle = StreamButtonStyleDefaults.secondarySolid,
    size: StreamButtonSize = StreamButtonSize.Medium,
    onAction: () -> Unit,
): Unit = StreamIconButton(
    onClick = onAction,
    icon = icon,
    contentDescription = contentDescription,
    modifier = modifier,
    enabled = enabled,
    style = style,
    size = size,
)

/**
 * A round icon button used for call actions with an active and an inactive state, such as the microphone.
 *
 * @param modifier The modifier applied to the button.
 * @param isActionActive Whether the action is in its active state, which selects the first icon and [onStyle].
 * @param iconOnOff The icons of the active and the inactive state.
 * @param contentDescription The accessibility description of the action, or null when a parent describes it.
 * @param enabled Whether the action accepts clicks.
 * @param progress Whether a progress indicator replaces the icon while the action is pending.
 * @param onStyle The colors of the active state. See [StreamButtonStyleDefaults].
 * @param offStyle The colors of the inactive state. See [StreamButtonStyleDefaults].
 * @param size The visual size of the button.
 * @param onAction Called when the action is clicked.
 */
@Composable
public fun ToggleAction(
    modifier: Modifier = Modifier,
    isActionActive: Boolean,
    iconOnOff: Pair<Painter, Painter>,
    contentDescription: String? = null,
    enabled: Boolean = true,
    progress: Boolean = false,
    onStyle: StreamButtonStyle = StreamButtonStyleDefaults.secondarySolid,
    offStyle: StreamButtonStyle = StreamButtonStyleDefaults.destructiveSolid,
    size: StreamButtonSize = StreamButtonSize.Medium,
    onAction: () -> Unit,
): Unit = StreamButton(
    onClick = onAction,
    modifier = modifier,
    enabled = enabled,
    style = if (isActionActive) onStyle else offStyle,
    size = size,
) {
    if (progress) {
        CircularProgressIndicator(
            modifier = Modifier.size(size.iconSize),
            color = LocalContentColor.current,
            strokeWidth = StreamTokens.strokeW200,
        )
    } else {
        Icon(
            painter = if (isActionActive) iconOnOff.first else iconOnOff.second,
            contentDescription = contentDescription,
            modifier = Modifier.size(size.iconSize),
        )
    }
}
