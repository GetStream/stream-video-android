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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.FlipCamera
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleHifiAudio
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.call.state.ToggleSpeakerphone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import stream.video.sfu.models.AudioBitrateProfile

/**
 * Builds the default set of Call Control actions based on the call devices.
 *
 * @param call The call that contains all the participants state and tracks.
 * @return [List] of call control actions that the user can trigger.
 */
@Composable
public fun buildDefaultAudioControlActions(
    call: Call,
    onCallAction: (CallAction) -> Unit,
): List<@Composable () -> Unit> {
    val isMicrophoneEnabled by if (LocalInspectionMode.current) {
        remember { mutableStateOf(true) }
    } else {
        call.microphone.isEnabled.collectAsStateWithLifecycle()
    }

    return listOf(
        {
            ToggleMicrophoneAction(
                isMicrophoneEnabled = isMicrophoneEnabled,
                onCallAction = onCallAction,
            )
        },
        {
            LeaveCallAction(onCallAction = onCallAction)
        },
    )
}

/**
 * Builds the default set of Call Control actions based on the call devices.
 *
 * @param call The call that contains all the participants state and tracks.
 * @return [List] of call control actions that the user can trigger.
 */
@Composable
public fun buildDefaultCallControlActions(
    call: Call,
    onCallAction: (CallAction) -> Unit,
): List<@Composable () -> Unit> {
    val isCameraEnabled by if (LocalInspectionMode.current) {
        remember { mutableStateOf(true) }
    } else {
        call.camera.isEnabled.collectAsStateWithLifecycle()
    }
    val isMicrophoneEnabled by if (LocalInspectionMode.current) {
        remember { mutableStateOf(true) }
    } else {
        call.microphone.isEnabled.collectAsStateWithLifecycle()
    }

    return listOf(
        {
            ToggleCameraAction(
                isCameraEnabled = isCameraEnabled,
                onCallAction = onCallAction,
            )
        },
        {
            ToggleMicrophoneAction(
                isMicrophoneEnabled = isMicrophoneEnabled,
                onCallAction = onCallAction,
            )
        },
        {
            FlipCameraAction(
                onCallAction = onCallAction,
            )
        },
    )
}

public object DefaultOnCallActionHandler {

    public fun onCallAction(call: Call, callAction: CallAction) {
        when (callAction) {
            is ToggleCamera -> call.camera.setEnabled(callAction.isEnabled)
            is ToggleMicrophone -> call.microphone.setEnabled(callAction.isEnabled)
            is ToggleSpeakerphone -> call.speaker.setEnabled(callAction.isEnabled)
            is FlipCamera -> call.camera.flip()
            is ToggleHifiAudio -> {
                val newProfile = if (callAction.isHifiAudioEnabled) {
                    AudioBitrateProfile.AUDIO_BITRATE_PROFILE_MUSIC_HIGH_QUALITY
                } else {
                    AudioBitrateProfile.AUDIO_BITRATE_PROFILE_VOICE_STANDARD_UNSPECIFIED
                }
                // Launch in a coroutine scope since setAudioBitrateProfile is suspend
                // Using SupervisorJob to prevent cancellation of other coroutines on failure
                CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
                    call.microphone.setAudioBitrateProfile(newProfile)
                }
            }
            else -> Unit
        }
    }
}
