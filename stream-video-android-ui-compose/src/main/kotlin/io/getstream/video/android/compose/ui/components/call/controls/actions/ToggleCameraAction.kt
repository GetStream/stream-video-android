/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.ui.common.R

/**
 * A call action button represents toggling a camera.
 *
 * @param modifier Optional Modifier for this action button.
 * @param isCameraEnabled Represent is camera enabled.
 * @param enabled Whether or not this action button will handle input events.
 * @param onCallAction A [CallAction] event that will be fired.
 */
@Composable
public fun ToggleCameraAction(
    modifier: Modifier = Modifier,
    isCameraEnabled: Boolean,
    enabled: Boolean = true,
    shape: Shape = VideoTheme.shapes.callControlsButton,
    enabledColor: Color = VideoTheme.colors.callActionIconEnabledBackground,
    disabledColor: Color = VideoTheme.colors.callActionIconDisabledBackground,
    enabledIconTint: Color = VideoTheme.colors.callActionIconEnabled,
    disabledIconTint: Color = VideoTheme.colors.callActionIconDisabled,
    onCallAction: (ToggleCamera) -> Unit,
) {
    val cameraIcon = painterResource(
        id = if (isCameraEnabled) {
            R.drawable.stream_video_ic_videocam_on
        } else {
            R.drawable.stream_video_ic_videocam_off
        },
    )

    CallControlActionBackground(
        modifier = modifier,
        isEnabled = isCameraEnabled,
        shape = shape,
        enabledColor = enabledColor,
        disabledColor = disabledColor,
    ) {
        Icon(
            modifier = Modifier
                .padding(13.dp)
                .clickable(enabled = enabled) {
                    onCallAction(
                        ToggleCamera(isCameraEnabled.not()),
                    )
                },
            tint = if (isCameraEnabled) {
                enabledIconTint
            } else {
                disabledIconTint
            },
            painter = cameraIcon,
            contentDescription = stringResource(R.string.stream_video_call_controls_toggle_camera),
        )
    }
}