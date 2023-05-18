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

package io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.background.CallBackground
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallDeviceState
import io.getstream.video.android.core.model.CallType
import io.getstream.video.android.core.viewmodel.CallViewModel
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockCall
import io.getstream.video.android.mock.mockParticipantList

/**
 * Represents the Outgoing Call state and UI, when the user is calling other people.
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
 */
@Composable
public fun OutgoingCallContent(
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
    onBackPressed: () -> Unit = {},
    onCallAction: (CallAction) -> Unit = callViewModel::onCallAction,
) {
    val callDeviceState: CallDeviceState by callViewModel.callDeviceState.collectAsStateWithLifecycle()

    OutgoingCallContent(
        call = callViewModel.call,
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
}

/**
 * Represents the Outgoing Call state and UI, when the user is calling other people.
 *
 * @param call The call contains states and will be rendered with participants.
 * @param callType Represent the call type is a video or an audio.
 * @param modifier Modifier for styling.
 * @param isShowingHeader Weather or not the app bar will be shown.
 * @param callHeaderContent Content shown for the call header.
 * @param callDetailsContent Content shown for call details, such as call participant information.
 * @param callControlsContent Content shown for controlling call, such as accepting a call or declining a call.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler used when the user interacts with Call UI.
 */
@Composable
public fun OutgoingCallContent(
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
) {
    val participants: List<ParticipantState> by call.state.participants.collectAsStateWithLifecycle()

    OutgoingCallContent(
        call = call,
        callType = callType,
        participants = participants,
        callDeviceState = callDeviceState,
        modifier = modifier,
        isShowingHeader = isShowingHeader,
        callHeaderContent = callHeaderContent,
        callDetailsContent = callDetailsContent,
        callControlsContent = callControlsContent,
        onBackPressed = onBackPressed,
        onCallAction = onCallAction
    )
}

/**
 * Represents the Outgoing Call state and UI, when the user is calling other people.
 *
 * @param call The call contains states and will be rendered with participants.
 * @param callType Represent the call type is a video or an audio.
 * @param modifier Modifier for styling.
 * @param participants A list of participants.
 * @param isShowingHeader Weather or not the app bar will be shown.
 * @param callHeaderContent Content shown for the call header.
 * @param callDetailsContent Content shown for call details, such as call participant information.
 * @param callControlsContent Content shown for controlling call, such as accepting a call or declining a call.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler used when the user interacts with Call UI.
 */
@Composable
public fun OutgoingCallContent(
    call: Call,
    callType: CallType,
    participants: List<ParticipantState>,
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
) {
    CallBackground(
        modifier = modifier,
        participants = participants,
        callType = callType,
        isIncoming = false
    ) {

        Column {
            if (isShowingHeader) {
                callHeaderContent?.invoke(this) ?: CallAppBar(
                    call = call,
                    onBackPressed = onBackPressed,
                    onCallAction = onCallAction
                )
            }

            val topPadding = if (participants.size == 1 || callType == CallType.VIDEO) {
                VideoTheme.dimens.singleAvatarAppbarPadding
            } else {
                VideoTheme.dimens.avatarAppbarPadding
            }

            callDetailsContent?.invoke(this, participants, topPadding) ?: OutgoingCallDetails(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = topPadding),
                participants = participants,
                callType = callType
            )
        }

        callControlsContent?.invoke(this) ?: OutgoingCallControls(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = VideoTheme.dimens.outgoingCallOptionsBottomPadding),
            isCameraEnabled = callDeviceState.isCameraEnabled,
            isMicrophoneEnabled = callDeviceState.isMicrophoneEnabled,
            onCallAction = onCallAction
        )
    }
}

@Preview
@Composable
private fun OutgoingCallVideoPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        OutgoingCallContent(
            call = mockCall,
            callType = CallType.VIDEO,
            participants = mockParticipantList,
            callDeviceState = CallDeviceState(),
            onBackPressed = {}
        ) {}
    }
}

@Preview
@Composable
private fun OutgoingCallAudioPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        OutgoingCallContent(
            call = mockCall,
            callType = CallType.AUDIO,
            participants = mockParticipantList,
            callDeviceState = CallDeviceState(),
            onBackPressed = {}
        ) {}
    }
}
