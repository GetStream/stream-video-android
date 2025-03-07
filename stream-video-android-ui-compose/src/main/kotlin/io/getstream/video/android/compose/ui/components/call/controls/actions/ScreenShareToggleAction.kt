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

package io.getstream.video.android.compose.ui.components.call.controls.actions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MobileScreenShare
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.call.state.ClosedCaptionsAction

/**
 * A call action button for screen sharing.
 */
@Composable
public fun ScreenShareToggleAction(
    modifier: Modifier = Modifier,
    active: Boolean,
    enabled: Boolean = true,
    shape: Shape? = null,
    enabledColor: Color? = null,
    disabledColor: Color? = null,
    onCallAction: (ClosedCaptionsAction) -> Unit,
): Unit = ToggleAction(
    isActionActive = active,
    iconOnOff =
    Pair(Icons.AutoMirrored.Filled.MobileScreenShare, Icons.AutoMirrored.Filled.MobileScreenShare),
    modifier = modifier,
    enabled = enabled, shape = shape,
    enabledColor = enabledColor, disabledColor = disabledColor,
    offStyle = VideoTheme.styles.buttonStyles.primaryIconButtonStyle(),
    onStyle = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle(),
) {
    onCallAction(ClosedCaptionsAction(!active))
}
