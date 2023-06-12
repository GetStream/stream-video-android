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

package io.getstream.video.android.compose.ui.components.call.lobby

import android.content.res.Configuration
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.state.CallAction

/**
 * Builds the default set of Lobby Control actions based on the call device states.
 *
 * @param call The call that contains all the participants state and tracks.
 * @return [List] of call control actions that the user can trigger.
 */
@Composable
public fun buildDefaultLobbyControlActions(
    call: Call,
    onCallAction: (CallAction) -> Unit
): List<@Composable () -> Unit> {

    val orientation = LocalConfiguration.current.orientation

    val modifier = if (orientation == Configuration.ORIENTATION_PORTRAIT) {
        Modifier.size(VideoTheme.dimens.callControlButtonSize)
    } else {
        Modifier.size(VideoTheme.dimens.landscapeCallControlButtonSize)
    }

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
            ToggleMicrophoneAction(
                modifier = modifier,
                isMicrophoneEnabled = isMicrophoneEnabled,
                onCallAction = onCallAction
            )
        },
        {
            ToggleCameraAction(
                modifier = modifier,
                isCameraEnabled = isCameraEnabled,
                onCallAction = onCallAction
            )
        },
    )
}
