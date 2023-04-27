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

package io.getstream.video.android.compose.ui.components.call.controls

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.internal.LandscapeCallControls
import io.getstream.video.android.compose.ui.components.call.controls.internal.RegularCallControls
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallDeviceState
import io.getstream.video.android.core.viewmodel.CallViewModel

/**
 * Shows the default Call controls content that allow the user to trigger various actions.
 *
 * @param callViewModel Used to fetch the state of the call and its media.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 */
@Composable
public fun CallControls(
    callViewModel: CallViewModel,
    onCallAction: (CallAction) -> Unit,
    actions: List<(@Composable () -> Unit)> = buildDefaultCallControlActions(
        callViewModel = callViewModel,
        onCallAction
    ),
) {
    val callDeviceState by callViewModel.callDeviceState.collectAsState()

    CallControls(
        callDeviceState = callDeviceState,
        actions = actions,
        onCallAction = onCallAction
    )
}

/**
 * Stateless version of [DefaultCallControlsContent].
 * Shows the default Call controls content that allow the user to trigger various actions.
 *
 * @param call State of the Call.
 * @param callDeviceState Media state of the call.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 */
@Composable
public fun CallControls(
    callDeviceState: CallDeviceState,
    onCallAction: (CallAction) -> Unit = {},
    actions: List<(@Composable () -> Unit)> = buildDefaultCallControlActions(
        callDeviceState,
        onCallAction
    ),
) {
    val orientation = LocalConfiguration.current.orientation

    val modifier = if (orientation == ORIENTATION_LANDSCAPE) {
        Modifier
            .fillMaxHeight()
            .width(VideoTheme.dimens.landscapeCallControlsSheetWidth)
    } else {
        Modifier
            .fillMaxWidth()
            .height(VideoTheme.dimens.callControlsSheetHeight)
    }

    CallControls(
        modifier = modifier,
        callDeviceState = callDeviceState,
        actions = actions,
        onCallAction = onCallAction
    )
}

/**
 * Represents the set of controls the user can use to change their audio and video device state, or
 * browse other types of settings, leave the call, or implement something custom.
 *
 * @param callDeviceState The state of the media devices for the current user.
 * @param isScreenSharing If there is a screen sharing session active.
 * @param modifier Modifier for styling.
 * @param actions Actions to show to the user with different controls.
 * @param onCallAction Handler when the user triggers an action.
 */
@Composable
public fun CallControls(
    callDeviceState: CallDeviceState,
    modifier: Modifier = Modifier,
    onCallAction: (CallAction) -> Unit,
    actions: List<(@Composable () -> Unit)> = buildDefaultCallControlActions(
        callDeviceState,
        onCallAction
    ),
) {
    val orientation = LocalConfiguration.current.orientation

    if (orientation == ORIENTATION_PORTRAIT) {
        RegularCallControls(
            modifier = modifier,
            callDeviceState = callDeviceState,
            onCallAction = onCallAction,
            actions = actions
        )
    } else if (orientation == ORIENTATION_LANDSCAPE) {
        LandscapeCallControls(
            modifier = modifier,
            callDeviceState = callDeviceState,
            onCallAction = onCallAction,
            actions = actions
        )
    }
}

@Preview
@Composable
private fun CallControlsPreview() {
    Column {
        VideoTheme {
            CallControls(
                callDeviceState = CallDeviceState(),
                onCallAction = {}
            )
        }
        VideoTheme(isInDarkMode = true) {
            CallControls(
                callDeviceState = CallDeviceState(),
                onCallAction = {}
            )
        }
    }
}
