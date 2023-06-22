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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.actions.DefaultOnCallActionHandler
import io.getstream.video.android.compose.ui.components.call.controls.actions.LandscapeControlActions
import io.getstream.video.android.compose.ui.components.call.controls.actions.RegularControlActions
import io.getstream.video.android.compose.ui.components.call.controls.actions.buildDefaultCallControlActions
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockCall

/**
 * Represents the set of controls the user can use to change their audio and video device state, or
 * browse other types of settings, leave the call, or implement something custom.
 * You can simply custom the controls button by giving a list of custom call actions to [actions].
 *
 * @param call The call that contains all the participants state and tracks.
 * @param modifier The modifier to be applied to the call controls.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param actions A list of composable call actions that will be arranged in the layout.
 */
@Composable
public fun ControlActions(
    call: Call,
    modifier: Modifier = Modifier,
    onCallAction: (CallAction) -> Unit = { DefaultOnCallActionHandler.onCallAction(call, it) },
    backgroundColor: Color = VideoTheme.colors.barsBackground,
    elevation: Dp = VideoTheme.dimens.controlActionsElevation,
    shape: Shape = VideoTheme.shapes.callControls,
    spaceBy: Dp? = null,
    actions: List<(@Composable () -> Unit)> = buildDefaultCallControlActions(
        call = call, onCallAction
    ),
) {
    val orientation = LocalConfiguration.current.orientation

    val controlsModifier = if (orientation == ORIENTATION_LANDSCAPE) {
        modifier
            .fillMaxHeight()
            .width(VideoTheme.dimens.landscapeControlActionsWidth)
    } else {
        modifier
            .fillMaxWidth()
            .height(VideoTheme.dimens.controlActionsHeight)
    }

    if (orientation == ORIENTATION_PORTRAIT) {
        RegularControlActions(
            modifier = controlsModifier,
            call = call,
            backgroundColor = backgroundColor,
            shape = shape,
            elevation = elevation,
            spaceBy = spaceBy,
            onCallAction = onCallAction,
            actions = actions
        )
    } else if (orientation == ORIENTATION_LANDSCAPE) {
        LandscapeControlActions(
            modifier = controlsModifier,
            call = call,
            backgroundColor = backgroundColor,
            shape = shape,
            elevation = elevation,
            spaceBy = spaceBy,
            onCallAction = onCallAction,
            actions = actions
        )
    }
}

@Preview
@Composable
private fun CallControlsPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    Column {
        VideoTheme {
            ControlActions(call = mockCall, onCallAction = {})
        }
        VideoTheme(isInDarkMode = true) {
            ControlActions(call = mockCall, onCallAction = {})
        }
    }
}
