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

package io.getstream.video.android.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.StreamButton
import io.getstream.video.android.compose.ui.components.call.controls.actions.ChatDialogAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.FlipCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.mapper.ReactionMapper
import io.getstream.video.android.mock.StreamPreviewDataUtils
import io.getstream.video.android.mock.previewCall
import io.getstream.video.android.tooling.extensions.toPx

@Composable
fun LandscapeControls(
    call: Call,
    onChat: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isCameraEnabled by call.camera.isEnabled.collectAsStateWithLifecycle()
    val isMicrophoneEnabled by call.microphone.isEnabled.collectAsStateWithLifecycle()
    val toggleCamera = {
        call.camera.setEnabled(!isCameraEnabled, true)
    }
    val toggleMicrophone = {
        call.microphone.setEnabled(!isMicrophoneEnabled, true)
    }
    val onClick = {
        call.leave()
    }

    Popup(
        onDismissRequest = onDismiss,
        alignment = Alignment.TopEnd,
        offset = IntOffset(
            0,
            (VideoTheme.dimens.componentHeightL + VideoTheme.dimens.spacingM).toPx().toInt(),
        ),
    ) {
        LandscapeControlsContent(
            isCameraEnabled = isCameraEnabled,
            isMicEnabled = isMicrophoneEnabled,
            call = call,
            camera = toggleCamera,
            mic = toggleMicrophone,
            onClick = onClick,
            onChat = onChat,
            onSettings = onSettings,
        ) {
            onDismiss()
        }
    }
}

@Composable
fun LandscapeControlsContent(
    isCameraEnabled: Boolean,
    isMicEnabled: Boolean,
    call: Call,
    onChat: () -> Unit,
    onSettings: () -> Unit,
    camera: () -> Unit,
    mic: () -> Unit,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(
                color = VideoTheme.colors.baseSheetPrimary,
                shape = VideoTheme.shapes.dialog,
            )
            .width(400.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp),
        ) {
            Icon(
                tint = Color.White,
                imageVector = Icons.Default.Close,
                contentDescription = Icons.Default.Close.name,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp).clickable {
                    onDismiss()
                },
            )
            ReactionsMenu(call = call, reactionMapper = ReactionMapper.defaultReactionMapper()) {
                onDismiss()
            }
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ToggleCameraAction(isCameraEnabled = isCameraEnabled) {
                        camera()
                    }
                    ToggleMicrophoneAction(isMicrophoneEnabled = isMicEnabled) {
                        mic()
                    }
                    FlipCameraAction {
                        call.camera.flip()
                    }

                    ChatDialogAction(
                        messageCount = 0,
                        onCallAction = { onChat() },
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                StreamButton(
                    modifier = Modifier.fillMaxWidth(),
                    style = VideoTheme.styles.buttonStyles.primaryIconButtonStyle(),
                    icon = Icons.Default.Settings,
                    text = "Settings",
                    onClick = onSettings,
                )
                StreamButton(
                    modifier = Modifier.fillMaxWidth(),
                    style = VideoTheme.styles.buttonStyles.alertButtonStyle(),
                    icon = Icons.Default.CallEnd,
                    text = "Leave call",
                    onClick = onClick,
                )
            }
        }
    }
}

@Preview
@Composable
fun LandscapeControlsPreview() {
    StreamPreviewDataUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        LandscapeControlsContent(
            true,
            false,
            previewCall,
            {},
            {},
            {},
            {},
            {},
        ) {
        }
    }
}
