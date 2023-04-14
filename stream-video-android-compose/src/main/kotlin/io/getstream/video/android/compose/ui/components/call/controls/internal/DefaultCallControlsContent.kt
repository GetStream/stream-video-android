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

package io.getstream.video.android.compose.ui.components.call.controls.internal

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.CallControls
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallDeviceState
import io.getstream.video.android.core.viewmodel.CallViewModel

/**
 * Shows the default Call controls content that allow the user to trigger various actions.
 *
 * @param viewModel Used to fetch the state of the call and its media.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 */
@Composable
internal fun DefaultCallControlsContent(
    viewModel: CallViewModel,
    onCallAction: (CallAction) -> Unit = {}
) {
    val callDeviceState by viewModel.callDeviceState.collectAsState()

    DefaultCallControlsContent(
        call = viewModel.call,
        callDeviceState = callDeviceState,
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
internal fun DefaultCallControlsContent(
    call: Call,
    callDeviceState: CallDeviceState,
    onCallAction: (CallAction) -> Unit = {}
) {
    val screenSharingSession = call.state.screenSharingSession.collectAsState()
    val screenSharing = screenSharingSession.value
    val isScreenSharing by remember(screenSharing) { derivedStateOf { screenSharing != null } }

    val orientation = LocalConfiguration.current.orientation

    val modifier = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
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
        isScreenSharing = isScreenSharing,
        onCallAction = onCallAction
    )
}
