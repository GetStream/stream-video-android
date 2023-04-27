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

package io.getstream.video.android.compose.ui.components.call.controls

import android.content.res.Configuration
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import io.getstream.video.android.compose.state.ui.call.CallControlAction
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallDeviceState
import io.getstream.video.android.core.viewmodel.CallViewModel

/**
 * Builds the default set of Call Control actions based on the [callDeviceState].
 *
 * @param callDeviceState Information of whether microphone, speaker and camera are on or off.
 * @return [List] of [CallControlAction]s that the user can trigger.
 */
@Composable
public fun buildDefaultCallControlActions(
    callViewModel: CallViewModel,
    onCallAction: (CallAction) -> Unit
): List<@Composable () -> Unit> {

    val callDeviceState by callViewModel.callDeviceState.collectAsState()

    return buildDefaultCallControlActions(
        callDeviceState = callDeviceState,
        onCallAction = onCallAction
    )
}

/**
 * Builds the default set of Call Control actions based on the [callDeviceState].
 *
 * @param callDeviceState Information of whether microphone, speaker and camera are on or off.
 * @return [List] of [CallControlAction]s that the user can trigger.
 */
@Composable
public fun buildDefaultCallControlActions(
    callDeviceState: CallDeviceState,
    onCallAction: (CallAction) -> Unit
): List<@Composable () -> Unit> {

    val orientation = LocalConfiguration.current.orientation

    val modifier = if (orientation == Configuration.ORIENTATION_PORTRAIT) {
        Modifier.size(VideoTheme.dimens.callControlButtonSize)
    } else {
        Modifier.size(VideoTheme.dimens.landscapeCallControlButtonSize)
    }

    return listOf(
        {
            ChatDialogAction(
                modifier = modifier,
                onCallAction = onCallAction
            )
        },
        {
            ToggleCameraAction(
                modifier = modifier,
                isCameraEnabled = callDeviceState.isCameraEnabled,
                onCallAction = onCallAction
            )
        },
        {
            ToggleMicrophoneAction(
                modifier = modifier,
                isMicrophoneEnabled = callDeviceState.isMicrophoneEnabled,
                onCallAction = onCallAction
            )
        },
        {
            FlipCameraAction(
                modifier = modifier,
                onCallAction = onCallAction
            )
        },
        {
            LeaveCallAction(
                modifier = modifier,
                onCallAction = onCallAction
            )
        }
    )
}
