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

package io.getstream.video.android.compose.ui.components.participants

import android.view.View
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.ui.components.call.controls.internal.DefaultCallControlsContent
import io.getstream.video.android.compose.ui.components.participants.internal.RegularCallParticipantsContent
import io.getstream.video.android.compose.ui.components.participants.internal.ScreenSharingCallParticipantsContent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallDeviceState

/**
 * Renders all the CallParticipants, based on the number of people in a call and the call state.
 * Also takes into account if there are any screen sharing sessions active and adjusts the UI
 * accordingly.
 *
 * @param call The call that contains all the participants state and tracks.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param callDeviceState The state of the call media, such as audio, video.
 * @param modifier Modifier for styling.
 * @param paddingValues Padding within the parent.
 * @param onRender Handler when each of the Video views render their first frame.
 * @param onBackPressed Handler when the user taps back.
 * @param callControlsContent Content shown that allows users to trigger different actions.
 */
@Composable
public fun CallParticipants(
    call: Call,
    modifier: Modifier = Modifier,
    onCallAction: (CallAction) -> Unit = {},
    callDeviceState: CallDeviceState,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onRender: (View) -> Unit = {},
    onBackPressed: () -> Unit = {},
    callControlsContent: @Composable () -> Unit = {
        DefaultCallControlsContent(
            call = call,
            callDeviceState = callDeviceState,
            onCallAction = onCallAction
        )
    }
) {
    val screenSharingSession = call.state.screenSharingSession.collectAsState()
    val screenSharing = screenSharingSession.value

    if (screenSharing == null) {
        RegularCallParticipantsContent(
            call = call,
            modifier = modifier,
            paddingValues = paddingValues,
            onRender = onRender,
            onCallAction = onCallAction,
            onBackPressed = onBackPressed,
            callDeviceState = callDeviceState,
            callControlsContent = callControlsContent
        )
    } else {
        val participants by call.state.participants.collectAsState()

        ScreenSharingCallParticipantsContent(
            call = call,
            session = screenSharing,
            participants = participants,
            modifier = modifier,
            paddingValues = paddingValues,
            onRender = onRender,
            onCallAction = onCallAction,
            callDeviceState = callDeviceState,
            onBackPressed = onBackPressed,
            callControlsContent = callControlsContent
        )
    }
}
