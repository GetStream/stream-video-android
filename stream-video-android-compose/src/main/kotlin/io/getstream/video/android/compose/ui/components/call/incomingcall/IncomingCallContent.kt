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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.getstream.video.android.call.state.CallMediaState
import io.getstream.video.android.call.state.ToggleMicrophone
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.background.CallBackground
import io.getstream.video.android.compose.ui.components.call.CallAppBar
import io.getstream.video.android.compose.ui.components.call.incomingcall.internal.IncomingCallDetails
import io.getstream.video.android.compose.ui.components.call.incomingcall.internal.IncomingCallOptions
import io.getstream.video.android.compose.ui.components.mock.mockParticipantList
import io.getstream.video.android.model.CallType
import io.getstream.video.android.model.CallUser
import io.getstream.video.android.model.CallUserState
import io.getstream.video.android.viewmodel.CallViewModel

/**
 * Represents the Incoming Call state and UI, when the user receives a call from other people.
 *
 * @param viewModel The [CallViewModel] used to provide state and various handlers in the call.
 * @param modifier Modifier for styling.
 * @param onRejectCall Handler when the user decides to cancel or drop out of a call.
 * @param onAcceptCall Handler when the user accepts a call in Incoming Call state.
 * @param onVideoToggleChanged Handler when the user toggles their video on or off.
 */
@Composable
public fun IncomingCallContent(
    viewModel: CallViewModel,
    modifier: Modifier = Modifier,
    onRejectCall: () -> Unit = viewModel::hangUpCall,
    onAcceptCall: () -> Unit = viewModel::acceptCall,
    onVideoToggleChanged: (Boolean) -> Unit = { isEnabled ->
        viewModel.onCallAction(
            ToggleMicrophone(isEnabled)
        )
    },
) {
    val callType: CallType by viewModel.callType.collectAsState()
    val participants: List<CallUser> by viewModel.participants.collectAsState()
    val callMediaState: CallMediaState by viewModel.callMediaState.collectAsState()
    IncomingCall(
        modifier = modifier,
        participants = participants,
        isVideoEnabled = callMediaState.isCameraEnabled,
        callType = callType,
        onRejectCall = onRejectCall,
        onAcceptCall = onAcceptCall,
        onVideoToggleChanged = onVideoToggleChanged
    )
}

/**
 * Stateless variant of the Incoming call UI, which you can use to build your own custom logic that
 * powers the state and handlers.
 *
 * @param participants People participating in the call.
 * @param callType The type of call, Audio or Video.
 * @param isVideoEnabled Whether the video should be enabled when entering the call or not.
 * @param onRejectCall Handler when the user decides to cancel or drop out of a call.
 * @param onAcceptCall Handler when the user accepts a call in Incoming Call state.
 * @param onVideoToggleChanged Handler when the user toggles their video on or off.
 * @param modifier Modifier for styling.
 */
@Composable
public fun IncomingCall(
    participants: List<CallUser>,
    callType: CallType,
    isVideoEnabled: Boolean,
    onRejectCall: () -> Unit,
    onAcceptCall: () -> Unit,
    onVideoToggleChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    CallBackground(
        modifier = modifier,
        participants = participants,
        callType = callType,
        isIncoming = true
    ) {
        Column {

            CallAppBar()

            val topPadding = if (participants.size == 1) {
                VideoTheme.dimens.singleAvatarAppbarPadding
            } else {
                VideoTheme.dimens.avatarAppbarPadding
            }

            IncomingCallDetails(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = topPadding),
                participants = participants
            )
        }

        IncomingCallOptions(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = VideoTheme.dimens.incomingCallOptionsBottomPadding),
            isVideoCall = callType == CallType.VIDEO,
            isVideoEnabled = isVideoEnabled,
            onDeclineCall = onRejectCall,
            onAcceptCall = onAcceptCall,
            onVideoToggleChanged = onVideoToggleChanged
        )
    }
}

@Preview
@Composable
private fun IncomingCallPreview() {
    VideoTheme {
        IncomingCall(
            participants = mockParticipantList.map {
                CallUser(
                    id = it.id,
                    name = it.name,
                    role = it.role,
                    state = CallUserState.Idle,
                    createdAt = null,
                    updatedAt = null,
                    imageUrl = it.profileImageURL ?: "",
                    teams = emptyList()
                )
            },
            isVideoEnabled = false,
            callType = CallType.VIDEO,
            onRejectCall = { },
            onAcceptCall = { },
            onVideoToggleChanged = { }
        )
    }
}
