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

package io.getstream.video.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.base.styling.StreamFixedSizeButtonStyle
import io.getstream.video.android.compose.ui.components.call.controls.actions.CancelCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.LeaveCallAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.audio.StreamAudioDevice
import io.getstream.video.android.core.call.state.CallAction

@Composable
public fun AudioOnlyCallControlsV2(
    modifier: Modifier,
    isMicrophoneEnabled: Boolean,
    call: Call,
    audioDeviceUiStateList: List<AudioDeviceUiState>,
    onCallAction: (CallAction) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ToggleMicrophoneAction(
            modifier = Modifier.testTag("Stream_MicrophoneToggle_Enabled_$isMicrophoneEnabled"),
            isMicrophoneEnabled = isMicrophoneEnabled,
            onCallAction = onCallAction,
            offStyle = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle().fillCircle(1.5f),
            onStyle = VideoTheme.styles.buttonStyles.tertiaryIconButtonStyle().fillCircle(1.5f),
        )

        if (audioDeviceUiStateList.isNotEmpty()) {
            MicSelectorDropDown(call, audioDeviceUiStateList)
        }

        LeaveCallAction(
            modifier = Modifier.testTag("Stream_HangUpButton"),
            onCallAction = onCallAction,
            style = VideoTheme.styles.buttonStyles.primaryIconButtonStyle().fillCircle(1.5f),
        )
    }
}

/**
 * 1. @param call as part of its argument
 * 2. Or should we add a click callback
 */
@Composable
public fun OutgoingCallControlsV2(
    modifier: Modifier = Modifier,
    isVideoCall: Boolean = true,
    isCameraEnabled: Boolean,
    isMicrophoneEnabled: Boolean,
    call: Call,
    audioDeviceUiStateList: List<AudioDeviceUiState>,
    onCallAction: (CallAction) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ToggleMicrophoneAction(
            modifier = Modifier
                .testTag("Stream_MicrophoneToggle_Enabled_$isMicrophoneEnabled"),
            isMicrophoneEnabled = isMicrophoneEnabled,
            onCallAction = onCallAction,
            offStyle = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle().fillCircle(1.5f),
            onStyle = VideoTheme.styles.buttonStyles.tertiaryIconButtonStyle().fillCircle(1.5f),
        )

        if (audioDeviceUiStateList.isNotEmpty()) {
            MicSelectorDropDown(
                call,
                audioDeviceUiStateList,
            )
        }

        if (isVideoCall) {
            ToggleCameraAction(
                modifier = Modifier
                    .testTag("Stream_CameraToggle_Enabled_$isCameraEnabled"),
                offStyle = VideoTheme.styles.buttonStyles.secondaryIconButtonStyle().fillCircle(
                    1.5f,
                ),
                onStyle = VideoTheme.styles.buttonStyles.tertiaryIconButtonStyle().fillCircle(1.5f),
                isCameraEnabled = isCameraEnabled,
                onCallAction = onCallAction,
            )
        }

        CancelCallAction(
            modifier = Modifier.testTag("Stream_DeclineCallButton"),
            onCallAction = onCallAction,
            style = VideoTheme.styles.buttonStyles.primaryIconButtonStyle().fillCircle(1.5f),
        )
    }
}

@Composable
private fun MicSelectorDropDown(call: Call, audioDeviceUiStateList: List<AudioDeviceUiState>) {
    val audioInput = audioDeviceUiStateList.firstOrNull { it.highlight }
    val filteredList = audioDeviceUiStateList.filter { !it.highlight }
    var buttonPosition by remember { mutableStateOf(IntOffset.Zero) }
    if (audioInput != null) {
        var collapsed by remember { mutableStateOf(true) }
        if (collapsed) {
            Button(onClick = {
                collapsed = false
            }) {
                Text(audioInput.text)
            }
        } else {
            val density = LocalDensity.current

            val offsetY = with(density) {
                100.dp.roundToPx()
            }
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(buttonPosition.x, buttonPosition.y - offsetY),
            ) {
                Box() {
                    LazyColumn {
                        items(filteredList.size) { index ->
                            Button(onClick = {
                                val device = filteredList[index].streamAudioDevice
                                if (device is StreamAudioDevice.Speakerphone) {
                                    call.speaker.setEnabled(true, true)
                                } else {
                                    call.microphone.select(filteredList[index].streamAudioDevice)
                                }
                                collapsed = true
                            }) {
                                Text(filteredList[index].text)
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun StreamFixedSizeButtonStyle.fillCircle(fraction: Float): StreamFixedSizeButtonStyle {
    return this.copyFixed(
        width * fraction,
        height * fraction,
        shape = CircleShape,
    )
}

@Composable
fun BoxScope.OutgoingCallControlsRoot(
    call: Call,
    isMicrophoneEnabled: Boolean,
    onCallAction: (CallAction) -> Unit,
) {
    val availableDevices by call.microphone.devices.collectAsStateWithLifecycle()
    val selectedMicroPhoneDevice by call.microphone.selectedDevice.collectAsStateWithLifecycle()
    val audioDeviceUiStateList: List<AudioDeviceUiState> = availableDevices.map {
        val icon = when (it) {
            is StreamAudioDevice.BluetoothHeadset -> Icons.Default.BluetoothAudio
            is StreamAudioDevice.Earpiece -> Icons.Default.Headphones
            is StreamAudioDevice.Speakerphone -> Icons.Default.SpeakerPhone
            is StreamAudioDevice.WiredHeadset -> Icons.Default.HeadsetMic
        }
        AudioDeviceUiState(
            it,
            it.name,
            icon,
            it.name == selectedMicroPhoneDevice?.name,
        )
    }

    if (selectedMicroPhoneDevice != null) {
        OutgoingCallControlsV2(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(VideoTheme.dimens.spacingM),
            isVideoCall = false,
            isCameraEnabled = false,
            isMicrophoneEnabled = isMicrophoneEnabled,
            audioDeviceUiStateList = audioDeviceUiStateList,
            call = call,
            onCallAction = onCallAction,
        )
    }
}

data class AudioDeviceUiState(
    val streamAudioDevice: StreamAudioDevice,
    val text: String,
    val icon: ImageVector, // Assuming it's a drawable resource ID
    val highlight: Boolean,
)
