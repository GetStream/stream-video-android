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

package io.getstream.video.android.compose.ui.components.call.controls.actions

import android.content.res.Configuration
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.common.viewmodel.CallViewModel
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallDeviceState

/**
 * Builds the default set of Call Control actions based on the [CallDeviceState].
 *
 * @return [List] of call control actions that the user can trigger.
 */
@Composable
public fun buildDefaultCallControlActions(
    callViewModel: CallViewModel,
    onCallAction: (CallAction) -> Unit
): List<@Composable () -> Unit> {

    val callDeviceState by callViewModel.callDeviceState.collectAsStateWithLifecycle()

    return buildDefaultCallControlActions(
        callDeviceState = callDeviceState,
        onCallAction = onCallAction
    )
}

/**
 * Builds the default set of Call Control actions based on the [callDeviceState].
 *
 * @param callDeviceState Information of whether microphone, speaker and camera are on or off.
 * @return [List] of call control actions that the user can trigger.
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
