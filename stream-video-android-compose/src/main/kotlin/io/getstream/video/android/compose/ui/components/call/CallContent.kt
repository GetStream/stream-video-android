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

package io.getstream.video.android.compose.ui.components.call

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.getstream.video.android.call.state.ToggleCamera
import io.getstream.video.android.call.state.ToggleMicrophone
import io.getstream.video.android.compose.state.ui.participants.ChangeMuteState
import io.getstream.video.android.compose.state.ui.participants.InviteUsers
import io.getstream.video.android.compose.ui.components.call.activecall.ActiveCallContent
import io.getstream.video.android.compose.ui.components.call.activecall.internal.InviteUsersDialog
import io.getstream.video.android.compose.ui.components.call.incomingcall.IncomingCallContent
import io.getstream.video.android.compose.ui.components.call.outgoingcall.OutgoingCallContent
import io.getstream.video.android.compose.ui.components.participants.CallParticipantsInfoMenu
import io.getstream.video.android.model.User
import io.getstream.video.android.viewmodel.CallViewModel
import io.getstream.video.android.model.state.StreamCallState as State

/**
 * Represents different call content based on the call state provided from the [viewModel].
 *
 * The user can be in an Active Call state, if they've full joined the call, an Incoming Call state,
 * if they're being invited to join a call, or Outgoing Call state, if they're inviting other people
 * to join. Based on that, we show [ActiveCallContent], [IncomingCallContent] or [OutgoingCallContent],
 * respectively.
 *
 * @param viewModel The [CallViewModel] used to provide state and various handlers in the call.
 * @param modifier Modifier for styling.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallInfoSelected Handler when the call participants info is selected.
 * @param onRejectCall Handler when the user taps on the Reject Call button in Incoming Call state.
 * @param onAcceptCall Handler when the user accepts a call in Incoming Call state.
 * @param onCancelCall Handler when the user decides to cancel or drop out of a call.
 * @param onMicToggleChanged Handler when the user toggles their microphone on or off.
 * @param onVideoToggleChanged Handler when the user toggles their video on or off.
 */
@Composable
public fun CallContent(
    viewModel: CallViewModel,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    onCallInfoSelected: () -> Unit = viewModel::showCallInfo,
    onRejectCall: () -> Unit = viewModel::rejectCall,
    onAcceptCall: () -> Unit = viewModel::acceptCall,
    onCancelCall: () -> Unit = viewModel::cancelCall,
    onMicToggleChanged: (Boolean) -> Unit = { isEnabled ->
        viewModel.onCallAction(
            ToggleCamera(isEnabled)
        )
    },
    onVideoToggleChanged: (Boolean) -> Unit = { isEnabled ->
        viewModel.onCallAction(
            ToggleMicrophone(isEnabled)
        )
    },
) {
    val stateHolder = viewModel.streamCallState.collectAsState(initial = State.Idle)
    val state = stateHolder.value
    var usersToInvite by remember { mutableStateOf(emptyList<User>()) }

    if (state is State.Incoming && !state.acceptedByMe) {
        IncomingCallContent(
            modifier = modifier,
            viewModel = viewModel,
            onBackPressed = onBackPressed,
            onCallInfoSelected = onCallInfoSelected,
            onRejectCall = onRejectCall,
            onAcceptCall = onAcceptCall,
            onVideoToggleChanged = onVideoToggleChanged
        )
    } else if (state is State.Outgoing && !state.acceptedByCallee) {
        OutgoingCallContent(
            modifier = modifier,
            viewModel = viewModel,
            onBackPressed = onBackPressed,
            onCallInfoSelected = onCallInfoSelected,
            onCancelCall = onCancelCall,
            onMicToggleChanged = onMicToggleChanged,
            onVideoToggleChanged = onVideoToggleChanged
        )
    } else {
        ActiveCallContent(
            modifier = modifier,
            callViewModel = viewModel,
            onBackPressed = onBackPressed,
            onCallInfoSelected = onCallInfoSelected,
        )
        val isShowingParticipantsInfo by viewModel.isShowingCallInfo.collectAsState()

        if (isShowingParticipantsInfo) {
            val participantsState by viewModel.participantList.collectAsState(initial = emptyList())
            val users by viewModel.getUsersState().collectAsState()

            CallParticipantsInfoMenu(
                modifier = Modifier.fillMaxSize(),
                participantsState = participantsState,
                users = users,
                onDismiss = { viewModel.dismissOptions() },
                onInfoMenuAction = { action ->
                    when (action) {
                        is InviteUsers -> {
                            viewModel.dismissOptions()
                            usersToInvite = action.users
                        }
                        is ChangeMuteState -> viewModel.toggleMicrophone(action.isEnabled)
                    }
                }
            )
        }

        if (usersToInvite.isNotEmpty()) {
            InviteUsersDialog(
                users = usersToInvite,
                onDismiss = { usersToInvite = emptyList() },
                onInviteUsers = {
                    usersToInvite = emptyList()
                    viewModel.inviteUsersToCall(it)
                }
            )
        }
    }
}
