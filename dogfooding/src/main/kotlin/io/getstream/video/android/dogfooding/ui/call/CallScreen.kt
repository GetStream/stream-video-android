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

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
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
import kotlinx.coroutines.launch

@Composable
fun CallScreen(
    call: Call,
    onLeaveCall: () -> Unit = {}
) {
    val isCameraEnabled by call.camera.isEnabled.collectAsState()
    val isMicrophoneEnabled by call.microphone.isEnabled.collectAsState()
    val speakingWhileMuted by call.state.speakingWhileMuted.collectAsState()
    var isShowingSettingMenu by remember { mutableStateOf(false) }

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
                                onCallAction = { isShowingSettingMenu = true }
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
            SpeakingWhileMuted()
        }

        if (isShowingSettingMenu) {
            SettingMenu(call = call) {
                isShowingSettingMenu = false
            }
        }
    }
}

@Composable
private fun SpeakingWhileMuted() {
    Snackbar {
        Text(text = "You're talking while muting the microphone!")
    }
}

@Composable
private fun SettingMenu(
    call: Call,
    onDismissed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Popup(
        alignment = Alignment.BottomStart,
        offset = IntOffset(30, -200),
        onDismissRequest = { onDismissed.invoke() }
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .background(VideoTheme.colors.appBackground)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.clickable {
                        scope.launch {
                            call.sendReaction(type = "default", emoji = ":raise-hand:")
                            onDismissed.invoke()
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = io.getstream.video.android.ui.common.R.drawable.stream_video_ic_reaction),
                        tint = VideoTheme.colors.textHighEmphasis,
                        contentDescription = null
                    )

                    Text(
                        modifier = Modifier.padding(start = 20.dp),
                        text = "Reactions",
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.clickable {
                        call.debug.restartSubscriberIce()
                        call.debug.restartPublisherIce()
                        onDismissed.invoke()
                        Toast.makeText(context, "Restart Ice", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = io.getstream.video.android.ui.common.R.drawable.stream_video_ic_fullscreen_exit),
                        tint = VideoTheme.colors.textHighEmphasis,
                        contentDescription = null
                    )

                    Text(
                        modifier = Modifier.padding(start = 20.dp),
                        text = "Restart Ice",
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.clickable {
                        call.debug.switchSfu()
                        onDismissed.invoke()
                        Toast.makeText(context, "Switch sfu", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = io.getstream.video.android.ui.common.R.drawable.stream_video_ic_fullscreen),
                        tint = VideoTheme.colors.textHighEmphasis,
                        contentDescription = null
                    )

                    Text(
                        modifier = Modifier.padding(start = 20.dp),
                        text = "Switch sfu",
                        color = Color.White
                    )
                }
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
