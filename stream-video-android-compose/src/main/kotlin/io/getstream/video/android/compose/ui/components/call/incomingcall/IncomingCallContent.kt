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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
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
import io.getstream.video.android.ui.common.R

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
    modifier: Modifier = Modifier,
    callHeader: (@Composable () -> Unit)? = null,
    callDetails: (@Composable () -> Unit)? = null,
    callControls: (@Composable () -> Unit)? = null,
    @DrawableRes previewPlaceholder: Int = R.drawable.stream_video_ic_preview_avatar,
    onBackPressed: () -> Unit,
    onCallAction: (CallAction) -> Unit = callViewModel::onCallAction,
) {
    val callDeviceState: CallDeviceState by callViewModel.callDeviceState.collectAsState()

    IncomingCallContent(
        call = callViewModel.call,
        callDeviceState = callDeviceState,
        modifier = modifier,
        previewPlaceholder = previewPlaceholder,
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
    callDeviceState: CallDeviceState,
    modifier: Modifier = Modifier,
    callHeader: (@Composable () -> Unit)? = null,
    callDetails: (@Composable () -> Unit)? = null,
    callControls: (@Composable () -> Unit)? = null,
    @DrawableRes previewPlaceholder: Int = R.drawable.stream_video_ic_preview_avatar,
    onBackPressed: () -> Unit,
    onCallAction: (CallAction) -> Unit = {},
) {
    val participants: List<ParticipantState> by call.state.participants.collectAsState()

    IncomingCallContent(
        callType = CallType.VIDEO,
        participants = participants,
        isVideoEnabled = callDeviceState.isCameraEnabled,
        modifier = modifier,
        previewPlaceholder = previewPlaceholder,
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
 * @param isVideoEnabled Whether the video should be enabled when entering the call or not.
 * @param modifier Modifier for styling.
 * @param showHeader If the app bar header is shown or not.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler used when the user interacts with Call UI.
 */
@Composable
internal fun IncomingCallContent(
    callType: CallType,
    participants: List<ParticipantState>,
    isVideoEnabled: Boolean,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    callHeader: (@Composable () -> Unit)? = null,
    callDetails: (@Composable () -> Unit)? = null,
    callControls: (@Composable () -> Unit)? = null,
    @DrawableRes previewPlaceholder: Int = R.drawable.stream_video_ic_preview_avatar,
    onBackPressed: () -> Unit,
    onCallAction: (CallAction) -> Unit,
) {
    CallBackground(
        modifier = modifier,
        participants = participants,
        callType = callType,
        isIncoming = true
    ) {
        Column {
            if (showHeader) {
                callHeader?.invoke() ?: CallAppBar(
                    onBackPressed = onBackPressed,
                    onCallAction = onCallAction
                )
            }

            val topPadding = if (participants.size == 1) {
                VideoTheme.dimens.singleAvatarAppbarPadding
            } else {
                VideoTheme.dimens.avatarAppbarPadding
            }

            callDetails?.invoke() ?: IncomingCallDetails(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = topPadding),
                callType = callType,
                participants = participants,
                previewPlaceholder = previewPlaceholder
            )
        }

        callControls?.invoke() ?: IncomingCallControls(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = VideoTheme.dimens.incomingCallOptionsBottomPadding),
            isVideoCall = callType == CallType.VIDEO,
            isVideoEnabled = isVideoEnabled,
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
            isVideoEnabled = false,
            previewPlaceholder = R.drawable.stream_video_call_sample,
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
            isVideoEnabled = false,
            previewPlaceholder = R.drawable.stream_video_call_sample,
            onBackPressed = {}
        ) {}
    }
}
