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

package io.getstream.chat.android.dogfooding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.audio.AudioDevice
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.CallOptions
import io.getstream.video.android.compose.ui.components.MainStage
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.viewmodel.CallViewModel

@Composable
internal fun VideoCallContent(
    callViewModel: CallViewModel,
    onLeaveCall: () -> Unit
) {
    val room by callViewModel.callState.collectAsState(initial = null)
    val isShowingParticipantsInfo by callViewModel.isShowingParticipantsInfo.collectAsState(
        false
    )

    val isShowingSettings by callViewModel.isShowingSettings.collectAsState(
        false
    )

    val participantsState by callViewModel.participantsState.collectAsState(initial = emptyList())

    BackHandler {
        if (isShowingParticipantsInfo || isShowingSettings) {
            callViewModel.dismissOptions()
        } else {
            onLeaveCall()
        }
    }

    VideoTheme {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val roomState = room

            Column(modifier = Modifier.fillMaxSize()) {

                CallActionBar(callViewModel, "TODO - Call ID") // TODO -

                if (roomState == null) {
                    Box(
                        modifier = Modifier
                            .height(250.dp)
                            .fillMaxWidth()
                    ) {
                        Image(
                            modifier = Modifier.align(Alignment.Center),
                            imageVector = Icons.Default.Call,
                            contentDescription = null
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MainStage(
                            modifier = Modifier
                                .fillMaxSize(),
                            call = roomState
                        )

                        CallOptions(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(56.dp),
                            call = roomState,
                            onEndCall = {
                                onLeaveCall()
                            },
                            onCameraToggled = { isEnabled ->
                                callViewModel.toggleCamera(
                                    isEnabled
                                )
                            },
                            onMicrophoneToggled = { isEnabled ->
                                callViewModel.toggleMicrophone(
                                    isEnabled
                                )
                            },
                            onCameraFlipped = callViewModel::flipCamera,
                            onShowSettings = callViewModel::showSettings
                        )
                    }
                }
            }

            if (isShowingParticipantsInfo) {
                ParticipantsInfo(callViewModel, participantsState)
            }

            if (isShowingSettings) {
                CallSettingsMenu(callViewModel)
            }
        }
    }
}

@Composable
private fun ParticipantsInfo(
    callViewModel: CallViewModel,
    participantsState: List<CallParticipantState>
) {
    Box(
        modifier = Modifier
            .background(color = Color.LightGray.copy(alpha = 0.7f))
            .fillMaxSize()
            .clickable { callViewModel.dismissOptions() }
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(16.dp)
                .heightIn(max = 200.dp)
                .widthIn(max = 200.dp)
                .align(Alignment.TopEnd)
                .background(color = Color.White, shape = RoundedCornerShape(16.dp)),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            items(participantsState) {
                ParticipantInfoItem(it)
            }
        }
    }
}

@Composable
private fun CallSettingsMenu(callViewModel: CallViewModel) {
    val devices = callViewModel.getAudioDevices()

    Box(
        modifier = Modifier
            .background(color = Color.LightGray.copy(alpha = 0.7f))
            .fillMaxSize()
            .clickable { callViewModel.dismissOptions() }
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(200.dp)
                .align(Alignment.Center),
            color = Color.White
        ) {

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(text = "Choose audio device")

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    items(devices) {
                        AudioDeviceItem(it) { device ->
                            callViewModel.selectAudioDevice(device)
                            callViewModel.dismissOptions()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioDeviceItem(
    device: AudioDevice,
    onDeviceSelected: (AudioDevice) -> Unit
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        content = { Text(text = device.name) },
        onClick = { onDeviceSelected(device) }
    )
}

@Composable
private fun ParticipantInfoItem(participant: CallParticipantState) {
    Row(
        modifier = Modifier.wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        val isAudioEnabled = participant.isLocalAudioEnabled
        Icon(
            imageVector = if (isAudioEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
            contentDescription = "User Audio"
        )

        val isVideoEnabled = participant.isLocalVideoEnabled
        Icon(
            imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
            contentDescription = "User Video"
        )

        val userName = when {
            participant.userName.isNotBlank() -> participant.userName
            participant.userId.isNotBlank() -> participant.userId
            else -> "Unknown"
        }

        Text(
            text = userName,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun CallActionBar(callViewModel: CallViewModel, callId: String) {
    val title = if (callId.isBlank()) "Joining call..." else "Call ID: $callId"

    Box(
        modifier = Modifier
            .height(56.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colors.primary)
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Icon(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable {
                    callViewModel.showParticipants()
                }
                .padding(8.dp),
            imageVector = Icons.Default.Menu,
            contentDescription = "Participants info",
            tint = Color.White
        )
    }
}
