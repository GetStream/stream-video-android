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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import io.getstream.video.android.compose.state.ui.call.CallControlAction
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallMediaState

/**
 * Represents the set of controls the user can use to change their audio and video device state, or
 * browse other types of settings, leave the call, or implement something custom.
 *
 * @param callMediaState The state of the media devices for the current user.
 * @param isScreenSharing If there is a screen sharing session active.
 * @param modifier Modifier for styling.
 * @param actions Actions to show to the user with different controls.
 * @param onCallAction Handler when the user triggers an action.
 */
@Composable
public fun CallControls(
    callMediaState: CallMediaState,
    isScreenSharing: Boolean,
    modifier: Modifier = Modifier,
    actions: List<CallControlAction> = buildDefaultCallControlActions(callMediaState = callMediaState),
    onCallAction: (CallAction) -> Unit
) {
    val orientation = LocalConfiguration.current.orientation

    if (orientation == ORIENTATION_PORTRAIT) {
        RegularCallControls(
            modifier = modifier,
            callMediaState = callMediaState,
            isScreenSharing = isScreenSharing,
            onCallAction = onCallAction,
            actions = actions
        )
    } else if (orientation == ORIENTATION_LANDSCAPE) {
        LandscapeCallControls(
            modifier = modifier,
            callMediaState = callMediaState,
            onCallAction = onCallAction,
            isScreenSharing = true,
            actions = actions
        )
    }
}
