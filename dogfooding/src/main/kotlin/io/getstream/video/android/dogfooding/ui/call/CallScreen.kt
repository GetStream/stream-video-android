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

package io.getstream.video.android.dogfooding.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.controls.ControlActions
import io.getstream.video.android.compose.ui.components.call.controls.actions.CancelCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.FlipCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.SettingsAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.core.Call
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockCall

@Composable
fun CallScreen(
    call: Call,
    onLeaveCall: () -> Unit = {}
) {
    val isCameraEnabled by call.camera.isEnabled.collectAsState()
    val isMicrophoneEnabled by call.microphone.isEnabled.collectAsState()
    val speakingWhileMuted by call.state.speakingWhileMuted.collectAsState()

    VideoTheme {
        CallContent(
            modifier = Modifier.background(color = VideoTheme.colors.appBackground),
            call = call,
            enableInPictureInPicture = true,
            onBackPressed = { onLeaveCall.invoke() },
            controlsContent = {
                ControlActions(
                    call = call,
                    actions = listOf(
                        {
                            SettingsAction(
                                modifier = Modifier.size(VideoTheme.dimens.controlActionsButtonSize),
                                onCallAction = { }
                            )
                        },
                        {
                            ToggleCameraAction(
                                modifier = Modifier.size(VideoTheme.dimens.controlActionsButtonSize),
                                isCameraEnabled = isCameraEnabled,
                                onCallAction = { call.camera.setEnabled(it.isEnabled) }
                            )
                        },
                        {
                            ToggleMicrophoneAction(
                                modifier = Modifier.size(VideoTheme.dimens.controlActionsButtonSize),
                                isMicrophoneEnabled = isMicrophoneEnabled,
                                onCallAction = { call.microphone.setEnabled(it.isEnabled) }
                            )
                        },
                        {
                            FlipCameraAction(
                                modifier = Modifier.size(VideoTheme.dimens.controlActionsButtonSize),
                                onCallAction = { call.camera.flip() }
                            )
                        },
                        {
                            CancelCallAction(
                                modifier = Modifier.size(VideoTheme.dimens.controlActionsButtonSize),
                                onCallAction = { onLeaveCall.invoke() }
                            )
                        },
                    )
                )
            },
        )

        if (speakingWhileMuted) {
            Snackbar {
                Text(text = "You're talking while muting the microphone!")
            }
        }
    }
}

@Preview
@Composable
private fun CallScreenPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallScreen(call = mockCall)
    }
}
