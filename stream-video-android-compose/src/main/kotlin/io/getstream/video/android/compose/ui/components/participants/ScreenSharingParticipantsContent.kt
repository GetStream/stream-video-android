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

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.view.View
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import io.getstream.video.android.call.state.CallAction
import io.getstream.video.android.call.state.CallMediaState
import io.getstream.video.android.compose.ui.components.call.controls.internal.DefaultCallControlsContent
import io.getstream.video.android.compose.ui.components.participants.internal.LandscapeScreenSharingContent
import io.getstream.video.android.compose.ui.components.participants.internal.PortraitScreenSharingContent
import io.getstream.video.android.model.Call
import io.getstream.video.android.model.CallParticipantState
import io.getstream.video.android.model.ScreenSharingSession

/**
 * Renders all the CallParticipants, based on the number of people in a call and the call state.
 * Also takes into account if there are any screen sharing sessions active and adjusts the UI
 * accordingly.
 *
 * @param call The call that contains all the participants state and tracks.
 * @param session The screen sharing session which is active.
 * @param participants List of participants currently in the call.
 * @param callMediaState The state of the call media, such as audio, video.
 * @param onCallAction Handler when the user triggers a Call Control Action.
 * @param modifier Modifier for styling.
 * @param paddingValues Padding within the parent.
 * @param isFullscreen If we're rendering a full screen activity.
 * @param onRender Handler when each of the Video views render their first frame.
 * @param callControlsContent Content shown that allows users to trigger different actions.
 */
@Composable
public fun ScreenSharingCallParticipantsContent(
    call: Call,
    session: ScreenSharingSession,
    participants: List<CallParticipantState>,
    callMediaState: CallMediaState,
    onCallAction: (CallAction) -> Unit,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    isFullscreen: Boolean = false,
    onRender: (View) -> Unit = {},
    onBackPressed: () -> Unit = {},
    callControlsContent: @Composable () -> Unit = {
        DefaultCallControlsContent(
            call = call,
            callMediaState = callMediaState,
            onCallAction = onCallAction
        )
    }
) {
    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation

    if (orientation == ORIENTATION_PORTRAIT) {
        PortraitScreenSharingContent(
            call = call,
            session = session,
            participants = participants,
            paddingValues = paddingValues,
            modifier = modifier,
            onRender = onRender,
            onCallAction = onCallAction
        )
    } else {
        LandscapeScreenSharingContent(
            call = call,
            session = session,
            participants = participants,
            paddingValues = paddingValues,
            modifier = modifier,
            onRender = onRender,
            isFullscreen = isFullscreen,
            onCallAction = onCallAction,
            onBackPressed = onBackPressed,
            callControlsContent = callControlsContent
        )
    }
}
