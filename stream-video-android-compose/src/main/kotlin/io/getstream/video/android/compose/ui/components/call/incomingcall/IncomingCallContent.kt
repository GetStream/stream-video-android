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

package io.getstream.video.android.compose.ui.components.call.incomingcall

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import io.getstream.video.android.common.util.MockUtils
import io.getstream.video.android.common.util.mockParticipants
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.background.CallBackground
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.compose.ui.components.call.incomingcall.internal.IncomingCallControls
import io.getstream.video.android.compose.ui.components.call.incomingcall.internal.IncomingCallDetails
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallDeviceState
import io.getstream.video.android.core.model.CallType
import io.getstream.video.android.core.viewmodel.CallViewModel

/**
 * Represents the Incoming Call state and UI, when the user receives a call from other people.
 *
 * @param callViewModel The [CallViewModel] used to provide state and various handlers in the call.
 * @param modifier Modifier for styling.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler used when the user interacts with Call UI.
 */
@Composable
public fun IncomingCallContent(
    callViewModel: CallViewModel,
    callType: CallType,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    callHeader: (@Composable ColumnScope.() -> Unit)? = null,
    callDetails: (
        @Composable ColumnScope.(
            participants: List<ParticipantState>, topPadding: Dp
        ) -> Unit
    )? = null,
    callControls: (@Composable BoxScope.() -> Unit)? = null,
    onBackPressed: () -> Unit,
    onCallAction: (CallAction) -> Unit = callViewModel::onCallAction,
) {
    val callDeviceState: CallDeviceState by callViewModel.callDeviceState.collectAsState()

    IncomingCallContent(
        call = callViewModel.call,
        callType = callType,
        callDeviceState = callDeviceState,
        modifier = modifier,
        showHeader = showHeader,
        callHeader = callHeader,
        callDetails = callDetails,
        callControls = callControls,
        onBackPressed = onBackPressed,
        onCallAction = onCallAction
    )
}

@Composable
public fun IncomingCallContent(
    call: Call,
    callType: CallType,
    callDeviceState: CallDeviceState,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    callHeader: (@Composable ColumnScope.() -> Unit)? = null,
    callDetails: (
        @Composable ColumnScope.(
            participants: List<ParticipantState>, topPadding: Dp
        ) -> Unit
    )? = null,
    callControls: (@Composable BoxScope.() -> Unit)? = null,
    onBackPressed: () -> Unit,
    onCallAction: (CallAction) -> Unit = {},
) {
    val participants: List<ParticipantState> by call.state.participants.collectAsState()

    IncomingCallContent(
        callType = callType,
        participants = participants,
        isCameraEnabled = callDeviceState.isCameraEnabled,
        showHeader = showHeader,
        modifier = modifier,
        callHeader = callHeader,
        callDetails = callDetails,
        callControls = callControls,
        onBackPressed = onBackPressed,
        onCallAction = onCallAction
    )
}

/**
 * Stateless variant of the Incoming call UI, which you can use to build your own custom logic that
 * powers the state and handlers.
 *
 * @param callType The type of call, Audio or Video.
 * @param participants People participating in the call.
 * @param isCameraEnabled Whether the video should be enabled when entering the call or not.
 * @param modifier Modifier for styling.
 * @param showHeader If the app bar header is shown or not.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler used when the user interacts with Call UI.
 */
@Composable
internal fun IncomingCallContent(
    callType: CallType,
    participants: List<ParticipantState>,
    isCameraEnabled: Boolean,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    callHeader: (@Composable ColumnScope.() -> Unit)? = null,
    callDetails: (
        @Composable ColumnScope.(
            participants: List<ParticipantState>, topPadding: Dp
        ) -> Unit
    )? = null,
    callControls: (@Composable BoxScope.() -> Unit)? = null,
    onBackPressed: () -> Unit,
    onCallAction: (CallAction) -> Unit,
) {
    CallBackground(
        modifier = modifier, participants = participants, callType = callType, isIncoming = true
    ) {
        Column {
            if (showHeader) {
                callHeader?.invoke(this) ?: CallAppBar(
                    onBackPressed = onBackPressed, onCallAction = onCallAction
                )
            }

            val topPadding = if (participants.size == 1) {
                VideoTheme.dimens.singleAvatarAppbarPadding
            } else {
                VideoTheme.dimens.avatarAppbarPadding
            }

            callDetails?.invoke(this, participants, topPadding) ?: IncomingCallDetails(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = topPadding),
                callType = callType,
                participants = participants,
            )
        }

        callControls?.invoke(this) ?: IncomingCallControls(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = VideoTheme.dimens.incomingCallOptionsBottomPadding),
            isVideoCall = callType == CallType.VIDEO,
            isCameraEnabled = isCameraEnabled,
            onCallAction = onCallAction
        )
    }
}

@Preview
@Composable
private fun IncomingCallPreview1() {
    MockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        IncomingCallContent(
            participants = mockParticipants.takeLast(1),
            callType = CallType.VIDEO,
            isCameraEnabled = false,
            onBackPressed = {}
        ) {}
    }
}

@Preview
@Composable
private fun IncomingCallPreview2() {
    MockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        IncomingCallContent(
            participants = mockParticipants,
            callType = CallType.VIDEO,
            isCameraEnabled = false,
            onBackPressed = {}
        ) {}
    }
}
