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

package io.getstream.video.android.compose.ui.components.call.ringing

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallContent
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallDeviceState
import io.getstream.video.android.core.model.CallType
import io.getstream.video.android.core.viewmodel.CallViewModel
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockCall

/**
 * Represents different outgoing/incoming call content based on the call state provided from the [callViewModel].
 *
 * Depending on the call state, we show [CallContent], [IncomingCallContent] or [OutgoingCallContent] respectively.
 *
 * @param callViewModel The [CallViewModel] used to provide state and various handlers in the call.
 * @param callType Represent the call type is a video or an audio.
 * @param modifier Modifier for styling.
 * @param isShowingHeader Weather or not the app bar will be shown.
 * @param callHeaderContent Content shown for the call header.
 * @param callDetailsContent Content shown for call details, such as call participant information.
 * @param callControlsContent Content shown for controlling call, such as accepting a call or declining a call.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler used when the user interacts with Call UI.
 * @param onAcceptedCallContent A call content is shown when the call state is Accept.ø
 */
@Composable
public fun RingingCallContent(
    callViewModel: CallViewModel,
    callType: CallType,
    modifier: Modifier = Modifier,
    isShowingHeader: Boolean = true,
    callHeaderContent: (@Composable ColumnScope.() -> Unit)? = null,
    callDetailsContent: (
        @Composable ColumnScope.(
            participants: List<ParticipantState>, topPadding: Dp
        ) -> Unit
    )? = null,
    callControlsContent: (@Composable BoxScope.() -> Unit)? = null,
    onBackPressed: () -> Unit,
    onCallAction: (CallAction) -> Unit = callViewModel::onCallAction,
    onAcceptedCallContent: @Composable () -> Unit
) {
    val callDeviceState: CallDeviceState by callViewModel.callDeviceState.collectAsStateWithLifecycle()

    RingingCallContent(
        call = callViewModel.call,
        callType = callType,
        callDeviceState = callDeviceState,
        modifier = modifier,
        isShowingHeader = isShowingHeader,
        callHeaderContent = callHeaderContent,
        callDetailsContent = callDetailsContent,
        callControlsContent = callControlsContent,
        onBackPressed = onBackPressed,
        onCallAction = onCallAction,
        onAcceptedCallContent = onAcceptedCallContent
    )
}

/**
 * Represents different outgoing/incoming call content based on the call state provided from the [call].
 *
 * Depending on the call state, we show [CallContent], [IncomingCallContent] or [OutgoingCallContent] respectively.
 *
 * @param call The call contains states and will be rendered with participants.
 * @param callDeviceState A call device states that contains states for video, audio, and speaker.
 * @param callType Represent the call type is a video or an audio.
 * @param modifier Modifier for styling.
 * @param isShowingHeader Weather or not the app bar will be shown.
 * @param callHeaderContent Content shown for the call header.
 * @param callDetailsContent Content shown for call details, such as call participant information.
 * @param callControlsContent Content shown for controlling call, such as accepting a call or declining a call.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler used when the user interacts with Call UI.
 * @param onAcceptedCallContent A call content is shown when the call state is Accept.ø
 */
@Composable
public fun RingingCallContent(
    call: Call,
    callType: CallType,
    callDeviceState: CallDeviceState,
    modifier: Modifier = Modifier,
    isShowingHeader: Boolean = true,
    callHeaderContent: (@Composable ColumnScope.() -> Unit)? = null,
    callDetailsContent: (
        @Composable ColumnScope.(
            participants: List<ParticipantState>, topPadding: Dp
        ) -> Unit
    )? = null,
    callControlsContent: (@Composable BoxScope.() -> Unit)? = null,
    onBackPressed: () -> Unit = {},
    onCallAction: (CallAction) -> Unit = {},
    onAcceptedCallContent: @Composable () -> Unit
) {
    val ringingStateHolder = call.state.ringingState.collectAsState(initial = RingingState.Idle)
    val ringingState = ringingStateHolder.value

    if (ringingState is RingingState.Incoming && !ringingState.acceptedByMe) {
        IncomingCallContent(
            call = call,
            callType = callType,
            callDeviceState = callDeviceState,
            modifier = modifier,
            isShowingHeader = isShowingHeader,
            callHeaderContent = callHeaderContent,
            callDetailsContent = callDetailsContent,
            callControlsContent = callControlsContent,
            onBackPressed = onBackPressed,
            onCallAction = onCallAction
        )
    } else if (ringingState is RingingState.Outgoing && !ringingState.acceptedByCallee) {
        OutgoingCallContent(
            call = call,
            callType = callType,
            callDeviceState = callDeviceState,
            modifier = modifier,
            isShowingHeader = isShowingHeader,
            callHeaderContent = callHeaderContent,
            callDetailsContent = callDetailsContent,
            callControlsContent = callControlsContent,
            onBackPressed = onBackPressed,
            onCallAction = onCallAction
        )
    } else {
        // TODO: rejected + fallback
        onAcceptedCallContent.invoke()
    }
}

@Preview
@Composable
private fun RingingCallContentPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        RingingCallContent(
            call = mockCall,
            callType = CallType.VIDEO,
            callDeviceState = CallDeviceState(),
            onAcceptedCallContent = {},
        )
    }
}
