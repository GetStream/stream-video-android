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

package io.getstream.video.android.tutorial.livestream.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.core.Call

@Composable
fun LiveButton(
    modifier: Modifier,
    call: Call,
    isBackstage: Boolean,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Button(
            modifier = modifier,
            colors = if (isBackstage) {
                ButtonDefaults.buttonColors(
                    backgroundColor = VideoTheme.colors.brandPrimary,
                    contentColor = VideoTheme.colors.brandPrimary,
                )
            } else {
                ButtonDefaults.buttonColors(
                    backgroundColor = VideoTheme.colors.brandPrimary,
                    contentColor = VideoTheme.colors.brandPrimary,
                )
            },
            onClick = onClick,
        ) {
            Icon(
                modifier = Modifier.padding(vertical = 3.dp, horizontal = 6.dp),
                imageVector = if (isBackstage) {
                    Icons.Default.PlayArrow
                } else {
                    Icons.Default.Close
                },
                tint = Color.White,
                contentDescription = null,
            )

            Text(
                modifier = Modifier.padding(end = 6.dp),
                text = if (isBackstage) "Go Live" else "Stop Broadcast",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White,
            )
        }

        val isCameraEnabled by call.camera.isEnabled.collectAsState()
        val isMicrophoneEnabled by call.microphone.isEnabled.collectAsState()

        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            ToggleCameraAction(
                modifier = Modifier.size(45.dp),
                isCameraEnabled = isCameraEnabled,
                shape = RoundedCornerShape(8.dp),
                onCallAction = { callAction -> call.camera.setEnabled(callAction.isEnabled) },
            )

            ToggleMicrophoneAction(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(45.dp),
                isMicrophoneEnabled = isMicrophoneEnabled,
                shape = RoundedCornerShape(8.dp),
                onCallAction = { callAction -> call.microphone.setEnabled(callAction.isEnabled) },
            )
        }
    }
}
