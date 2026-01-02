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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.styling.StreamFixedSizeButtonStyle
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.ToggleMicrophone

/**
 * A call action button represents toggling a microphone.
 *
 * @param modifier Optional Modifier for this action button.
 * @param isMicrophoneEnabled Represent is camera enabled.
 * @param enabled Whether or not this action button will handle input events.
 * @param onCallAction A [CallAction] event that will be fired.
 */
@Composable
public fun ToggleMicrophoneAction(
    modifier: Modifier = Modifier,
    isMicrophoneEnabled: Boolean,
    enabled: Boolean = true,
    shape: Shape? = null,
    enabledColor: Color? = null,
    disabledColor: Color? = null,
    enabledIconTint: Color? = null,
    disabledIconTint: Color? = null,
    onStyle: StreamFixedSizeButtonStyle? = null,
    offStyle: StreamFixedSizeButtonStyle? = null,
    onCallAction: (ToggleMicrophone) -> Unit,
): Unit = ToggleAction(
    modifier = modifier,
    enabled = enabled,
    shape = shape,
    enabledColor = enabledColor,
    disabledColor = disabledColor,
    enabledIconTint = enabledIconTint,
    disabledIconTint = disabledIconTint,
    isActionActive = isMicrophoneEnabled,
    onStyle = onStyle,
    offStyle = offStyle,
    iconOnOff = Pair(Icons.Default.Mic, Icons.Default.MicOff),
) {
    onCallAction(ToggleMicrophone(isMicrophoneEnabled.not()))
}

@Preview
@Composable
public fun ToggleMicrophoneActionPreview() {
    VideoTheme {
        Column {
            Row {
                ToggleMicrophoneAction(isMicrophoneEnabled = false) {
                }

                ToggleMicrophoneAction(isMicrophoneEnabled = true) {
                }
            }
        }
    }
}
