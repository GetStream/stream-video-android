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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.getstream.video.android.compose.state.ui.participants.ChangeMuteState
import io.getstream.video.android.compose.state.ui.participants.InviteUsers
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.activecall.DefaultPictureInPictureContent
import io.getstream.video.android.compose.ui.components.call.activecall.internal.InviteUsersDialog
import io.getstream.video.android.compose.ui.components.call.controls.CallControls
import io.getstream.video.android.compose.ui.components.call.incomingcall.IncomingCallContent
import io.getstream.video.android.compose.ui.components.call.outgoingcall.OutgoingCallContent
import io.getstream.video.android.compose.ui.components.participants.CallParticipantsInfoMenu
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.InviteUsersToCall
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.model.Call
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.viewmodel.CallViewModel
import io.getstream.video.android.core.model.state.StreamCallState as State

/**
 * Represents different call content based on the call state provided from the [viewModel].
 *
 * The user can be in an Active Call state, if they've full joined the call, an Incoming Call state,
 * if they're being invited to join a call, or Outgoing Call state, if they're inviting other people
 * to join. Based on that, we show [CallContent], [IncomingCallContent] or [OutgoingCallContent],
 * respectively.
 *
 * @param viewModel The [CallViewModel] used to provide state and various handlers in the call.
 * @param modifier Modifier for styling.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler when the user clicks on some of the call controls.
 * @param callControlsContent Content shown for the
 * [CallControls] part of the UI.
 * @param pictureInPictureContent Content shown when the user enters Picture in Picture mode, if
 * it's been enabled in the app.
 * @param incomingCallContent Content shown when we're receiving a [Call].
 * @param outgoingCallContent Content shown when we're ringing other people.
 * @param callContent Content shown when we're connected to a [Call] successfully.
 */
@Composable
public fun CallContainer(
    viewModel: CallViewModel,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    //onCallAction: (CallAction) -> Unit = viewModel::onCallAction,
//    callControlsContent: @Composable () -> Unit = {
//        DefaultCallControlsContent(
//            viewModel
//            //onCallAction
//        )
//    },
    pictureInPictureContent: @Composable (Call) -> Unit = { DefaultPictureInPictureContent(it) },
    incomingCallContent: @Composable () -> Unit = {
        IncomingCallContent(
            modifier = modifier.testTag("incoming_call_content"),
            viewModel = viewModel,
            onBackPressed = onBackPressed,
            //onCallAction = onCallAction
        )
    },
    outgoingCallContent: @Composable () -> Unit = {
        OutgoingCallContent(
            modifier = modifier.testTag("outgoing_call_content"),
            viewModel = viewModel,
            onBackPressed = onBackPressed,
            //onCallAction = onCallAction
        )
    },
    callContent: @Composable () -> Unit = {
        DefaultCallContent(
            viewModel = viewModel,
            modifier = modifier,
            onBackPressed = onBackPressed,
            //onCallAction = onCallAction,
            //callControlsContent = callControlsContent,
            pictureInPictureContent = pictureInPictureContent
        )
    }
) {
    val stateHolder = viewModel.streamCallState.collectAsState(initial = State.Idle)
    val state = stateHolder.value

    if (state is State.Incoming && !state.acceptedByMe) {
        incomingCallContent()
    } else if (state is State.Outgoing && !state.acceptedByCallee) {
        outgoingCallContent()
    } else {
        callContent()
    }
}

@Composable
internal fun DefaultCallContent(
    viewModel: CallViewModel,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    //onCallAction: (CallAction) -> Unit = viewModel::onCallAction,
    callControlsContent: @Composable () -> Unit,
    pictureInPictureContent: @Composable (Call) -> Unit = { DefaultPictureInPictureContent(it) }
) {
    CallContent(
        modifier = modifier.testTag("call_content"),
        callViewModel = viewModel,
        onBackPressed = onBackPressed,
        //onCallAction = onCallAction,
        callControlsContent = callControlsContent,
        pictureInPictureContent = pictureInPictureContent
    )

    val isShowingParticipantsInfo by viewModel.isShowingCallInfo.collectAsState()
    val participantsState by viewModel.call.state.participants.collectAsState(initial = emptyList())
    var usersToInvite by remember { mutableStateOf(emptyList<User>()) }

    if (isShowingParticipantsInfo && participantsState.isNotEmpty()) {
        val users by viewModel.client.state.user.collectAsState()

        CallParticipantsInfoMenu(
            modifier = Modifier
                .fillMaxSize()
                .background(VideoTheme.colors.appBackground),
            participantsState = participantsState,
            //users = users,
            //onDismiss = { viewModel.dismissOptions() },
            onInfoMenuAction = { action ->
                when (action) {
                    is InviteUsers -> {
                        //viewModel.dismissOptions()
                        usersToInvite = action.users
                    }

                    //is ChangeMuteState -> onCallAction(ToggleMicrophone(action.isEnabled))
                    is ChangeMuteState -> TODO()
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
                //viewModel.onCallAction(InviteUsersToCall(it))
            }
        )
    }
}
