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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.common.viewmodel.CallViewModel
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.actions.LandscapeControlActions
import io.getstream.video.android.compose.ui.components.call.controls.actions.RegularControlActions
import io.getstream.video.android.compose.ui.components.call.controls.actions.buildDefaultCallControlActions
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallDeviceState

/**
 * Represents the set of controls the user can use to change their audio and video device state, or
 * browse other types of settings, leave the call, or implement something custom.
 * You can simply custom the controls button by giving a list of custom call actions to [actions].
 *
 * @param modifier The modifier to be applied to the call controls.
 * @param callViewModel Used to fetch the state of the call and its media.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param actions A list of composable call actions that will be arranged in the layout.
 */
@Composable
public fun ControlActions(
    modifier: Modifier = Modifier,
    callViewModel: CallViewModel,
    backgroundColor: Color = VideoTheme.colors.barsBackground,
    elevation: Dp = VideoTheme.dimens.callControlsElevation,
    shape: Shape = VideoTheme.shapes.callControls,
    onCallAction: (CallAction) -> Unit = {},
    actions: List<(@Composable () -> Unit)> = buildDefaultCallControlActions(
        callViewModel = callViewModel,
        onCallAction
    ),
) {
    val callDeviceState by callViewModel.callDeviceState.collectAsStateWithLifecycle()

    ControlActions(
        modifier = modifier,
        callDeviceState = callDeviceState,
        backgroundColor = backgroundColor,
        elevation = elevation,
        shape = shape,
        actions = actions,
        onCallAction = onCallAction
    )
}

/**
 * Represents the set of controls the user can use to change their audio and video device state, or
 * browse other types of settings, leave the call, or implement something custom.
 * You can simply custom the controls button by giving a list of custom call actions to [actions].
 *
 * @param callDeviceState A call device states that contains states for video, audio, and speaker.
 * @param modifier The modifier to be applied to the call controls.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param actions A list of composable call actions that will be arranged in the layout.
 */
@Composable
public fun ControlActions(
    callDeviceState: CallDeviceState,
    modifier: Modifier = Modifier,
    onCallAction: (CallAction) -> Unit = {},
    backgroundColor: Color = VideoTheme.colors.barsBackground,
    elevation: Dp = VideoTheme.dimens.callControlsElevation,
    shape: Shape = VideoTheme.shapes.callControls,
    actions: List<(@Composable () -> Unit)> = buildDefaultCallControlActions(
        callDeviceState,
        onCallAction
    ),
) {
    val orientation = LocalConfiguration.current.orientation

    val controlsModifier = if (orientation == ORIENTATION_LANDSCAPE) {
        modifier
            .fillMaxHeight()
            .width(VideoTheme.dimens.landscapeCallControlsSheetWidth)
    } else {
        modifier
            .fillMaxWidth()
            .height(VideoTheme.dimens.callControlsSheetHeight)
    }

    if (orientation == ORIENTATION_PORTRAIT) {
        RegularControlActions(
            modifier = controlsModifier,
            callDeviceState = callDeviceState,
            backgroundColor = backgroundColor,
            shape = shape,
            elevation = elevation,
            onCallAction = onCallAction,
            actions = actions
        )
    } else if (orientation == ORIENTATION_LANDSCAPE) {
        LandscapeControlActions(
            modifier = controlsModifier,
            callDeviceState = callDeviceState,
            backgroundColor = backgroundColor,
            shape = shape,
            elevation = elevation,
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
            ControlActions(
                callDeviceState = CallDeviceState(),
                onCallAction = {}
            )
        }
        VideoTheme(isInDarkMode = true) {
            ControlActions(
                callDeviceState = CallDeviceState(),
                onCallAction = {}
            )
        }
    }
}
