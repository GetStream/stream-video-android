/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.compose.ui.components.call.outgoingcall.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.extensions.toggleAlpha
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallMediaState
import io.getstream.video.android.core.call.state.CancelCall
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.ui.common.R

@Composable
internal fun OutgoingGroupCallOptions(
    callMediaState: CallMediaState,
    modifier: Modifier = Modifier,
    onCallAction: (CallAction) -> Unit,
) {
    val isMicEnabled = callMediaState.isMicrophoneEnabled
    val isVideoEnabled = callMediaState.isCameraEnabled

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                modifier = Modifier
                    .toggleAlpha(isMicEnabled)
                    .background(
                        color = VideoTheme.colors.appBackground,
                        shape = VideoTheme.shapes.callButton
                    )
                    .size(VideoTheme.dimens.mediumButtonSize),
                onClick = { onCallAction(ToggleMicrophone(!callMediaState.isMicrophoneEnabled)) },
                content = {
                    val micIcon = painterResource(
                        id = if (isMicEnabled) {
                            R.drawable.stream_video_ic_mic_on
                        } else {
                            R.drawable.stream_video_ic_mic_off
                        }
                    )

                    Icon(
                        painter = micIcon,
                        contentDescription = "Toggle Mic",
                        tint = VideoTheme.colors.textHighEmphasis
                    )
                }
            )

            IconButton(
                modifier = Modifier
                    .toggleAlpha(isVideoEnabled)
                    .background(
                        color = VideoTheme.colors.appBackground,
                        shape = VideoTheme.shapes.callButton
                    )
                    .size(VideoTheme.dimens.mediumButtonSize),
                onClick = { onCallAction(ToggleCamera(!callMediaState.isCameraEnabled)) },
                content = {
                    val cameraIcon =
                        painterResource(
                            id = if (isVideoEnabled) {
                                R.drawable.stream_video_ic_videocam_on
                            } else {
                                R.drawable.stream_video_ic_videocam_off
                            }
                        )

                    Icon(
                        painter = cameraIcon,
                        contentDescription = "Toggle Video",
                        tint = VideoTheme.colors.textHighEmphasis
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        IconButton(
            modifier = Modifier
                .background(
                    color = VideoTheme.colors.errorAccent,
                    shape = VideoTheme.shapes.callButton
                )
                .size(VideoTheme.dimens.largeButtonSize),
            onClick = { onCallAction(CancelCall) },
            content = {
                Icon(
                    painter = painterResource(id = R.drawable.stream_video_ic_call_end),
                    tint = Color.White,
                    contentDescription = "End call"
                )
            }
        )
    }
}

@Preview
@Composable
private fun OutgoingCallGroupOptions() {
    VideoTheme {
        Column {
            OutgoingGroupCallOptions(
                callMediaState = CallMediaState(
                    isMicrophoneEnabled = true,
                    isSpeakerphoneEnabled = true,
                    isCameraEnabled = true
                ),
                onCallAction = { }
            )

            OutgoingGroupCallOptions(
                callMediaState = CallMediaState(),
                onCallAction = { }
            )
        }
    }
}
