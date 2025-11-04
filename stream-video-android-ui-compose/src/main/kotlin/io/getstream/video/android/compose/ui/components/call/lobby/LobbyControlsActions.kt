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

package io.getstream.video.android.compose.ui.components.call.lobby

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleHifiAudioAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.state.CallAction
import stream.video.sfu.models.AudioBitrateProfile

/**
 * Builds the default set of Lobby Control actions based on the call device states.
 *
 * @param call The call that contains all the participants state and tracks.
 * @return [List] of call control actions that the user can trigger.
 */
@Composable
public fun buildDefaultLobbyControlActions(
    call: Call,
    onCallAction: (CallAction) -> Unit,
    isCameraEnabled: Boolean = if (LocalInspectionMode.current) {
        true
    } else {
        call.camera.isEnabled.value
    },
    isMicrophoneEnabled: Boolean = if (LocalInspectionMode.current) {
        true
    } else {
        call.microphone.isEnabled.value
    },
): List<@Composable () -> Unit> {
    val audioBitrateProfile by if (LocalInspectionMode.current) {
        remember {
            mutableStateOf(
                AudioBitrateProfile.AUDIO_BITRATE_PROFILE_VOICE_STANDARD_UNSPECIFIED,
            )
        }
    } else {
        call.microphone.audioBitrateProfile.collectAsStateWithLifecycle()
    }
    val isMusicHighQuality = audioBitrateProfile == AudioBitrateProfile.AUDIO_BITRATE_PROFILE_MUSIC_HIGH_QUALITY

    return listOf(
        {
            ToggleMicrophoneAction(
                modifier = Modifier
                    .testTag("Stream_MicrophoneToggle_Enabled_$isMicrophoneEnabled"),
                isMicrophoneEnabled = isMicrophoneEnabled,
                onCallAction = onCallAction,
            )
        },
        {
            ToggleCameraAction(
                modifier = Modifier
                    .testTag("Stream_CameraToggle_Enabled_$isCameraEnabled"),
                isCameraEnabled = isCameraEnabled,
                onCallAction = onCallAction,
            )
        },
        {
            ToggleHifiAudioAction(
                modifier = Modifier
                    .testTag("Stream_HifiAudioToggle_IsMusicHighQuality_$isMusicHighQuality"),
                isMusicHighQuality = isMusicHighQuality,
                onCallAction = onCallAction,
            )
        },
    )
}
