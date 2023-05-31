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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.video.android.common.viewmodel.CallViewModel
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.activecall.DefaultPictureInPictureContent
import io.getstream.video.android.compose.ui.components.call.activecall.internal.InviteUsersDialog
import io.getstream.video.android.compose.ui.components.call.controls.ControlActions
import io.getstream.video.android.compose.ui.components.call.ringing.RingingCallContent
import io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallContent
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContent
import io.getstream.video.android.compose.ui.components.participants.CallParticipantsInfoMenu
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.CallDeviceState
import io.getstream.video.android.core.call.state.InviteUsersToCall
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.mock.StreamMockUtils
import io.getstream.video.android.mock.mockCall
import io.getstream.video.android.model.User

/**
 * Represents different call content based on the call state provided from the [callViewModel].
 *
 * The user can be in an Active Call state, if they've full joined the call, an Incoming Call state,
 * if they're being invited to join a call, or Outgoing Call state, if they're inviting other people
 * to join. Based on that, we show [CallContent], [IncomingCallContent] or [OutgoingCallContent],
 * respectively.
 *
 * @param callViewModel The [CallViewModel] used to provide state and various handlers in the call.
 * @param modifier Modifier for styling.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler when the user clicks on some of the call controls.
 * @param callAppBarContent Content shown that a call information or an additional actions.
 * @param callControlsContent Content is shown that allows users to trigger different actions to control a joined call.
 * @param pictureInPictureContent Content shown when the user enters Picture in Picture mode, if
 * it's been enabled in the app.
 * @param ringingCallContent Content shown when we're receiving or sending a [Call].
 * @param callContent Content is shown by rendering video/audio when we're connected to a call successfully.
 */
@Composable
public fun CallContainer(
    callViewModel: CallViewModel,
    modifier: Modifier = Modifier,
    isVideoType: Boolean = true,
    onBackPressed: () -> Unit = {},
    onCallAction: (CallAction) -> Unit = callViewModel::onCallAction,
    callAppBarContent: @Composable (call: Call) -> Unit = {
        CallAppBar(
            modifier = Modifier.testTag("call_appbar"),
            call = callViewModel.call,
            leadingContent = null,
            onBackPressed = onBackPressed,
            onCallAction = onCallAction
        )
    },
    callControlsContent: @Composable (call: Call) -> Unit = {
        ControlActions(
            modifier = Modifier.testTag("call_controls"),
            callViewModel = callViewModel,
            onCallAction = onCallAction,
        )
    },
    pictureInPictureContent: @Composable (call: Call) -> Unit = { DefaultPictureInPictureContent(it) },
    callContent: @Composable (call: Call) -> Unit = {
        DefaultCallContent(
            modifier = modifier.testTag("call_content"),
            callViewModel = callViewModel,
            onBackPressed = onBackPressed,
            onCallAction = onCallAction,
            callAppBarContent = callAppBarContent,
            callControlsContent = callControlsContent,
            pictureInPictureContent = pictureInPictureContent
        )
    },
    ringingCallContent: @Composable (call: Call) -> Unit = {
        RingingCallContent(
            modifier = modifier.testTag("ringing_call_content"),
            isVideoType = isVideoType,
            callViewModel = callViewModel,
            onBackPressed = onBackPressed,
            onCallAction = onCallAction,
            onAcceptedContent = { callContent.invoke(it) },
            onRejectedContent = {}
        )
    },
) {
    val callDeviceState by callViewModel.callDeviceState.collectAsStateWithLifecycle()

    CallContainer(
        call = callViewModel.call,
        callDeviceState = callDeviceState,
        isVideoType = isVideoType,
        modifier = modifier,
        onBackPressed = onBackPressed,
        onCallAction = onCallAction,
        callAppBarContent = callAppBarContent,
        callControlsContent = callControlsContent,
        pictureInPictureContent = pictureInPictureContent,
        ringingCallContent = ringingCallContent,
        callContent = callContent,
    )
}

/**
 * Represents different call content based on the call state provided from the [call].
 *
 * The user can be in an Active Call state, if they've full joined the call, an Incoming Call state,
 * if they're being invited to join a call, or Outgoing Call state, if they're inviting other people
 * to join. Based on that, we show [CallContent], [IncomingCallContent] or [OutgoingCallContent],
 * respectively.
 *
 * @param call The call that contains all the participants state and tracks.
 * @param modifier Modifier for styling.
 * @param onBackPressed Handler when the user taps on the back button.
 * @param onCallAction Handler when the user clicks on some of the call controls.
 * @param callAppBarContent Content is shown that calls information or additional actions.
 * @param callControlsContent Content is shown that allows users to trigger different actions to control a joined call.
 * @param pictureInPictureContent Content shown when the user enters Picture in Picture mode, if
 * it's been enabled in the app.
 * @param ringingCallContent Content shown when we're receiving or sending a [Call].
 * @param callContent Content is shown by rendering video/audio when we're connected to a call successfully.
 */
@Composable
public fun CallContainer(
    call: Call,
    modifier: Modifier = Modifier,
    isVideoType: Boolean = true,
    callDeviceState: CallDeviceState,
    onBackPressed: () -> Unit = {},
    onCallAction: (CallAction) -> Unit = {},
    callAppBarContent: @Composable (call: Call) -> Unit = {
        CallAppBar(
            modifier = Modifier.testTag("call_appbar"),
            call = call,
            onBackPressed = onBackPressed,
            onCallAction = onCallAction
        )
    },
    callControlsContent: @Composable (call: Call) -> Unit = {
        ControlActions(
            modifier = Modifier.testTag("call_controls"),
            callDeviceState = callDeviceState,
            onCallAction = onCallAction
        )
    },
    pictureInPictureContent: @Composable (call: Call) -> Unit = { DefaultPictureInPictureContent(it) },
    callContent: @Composable (call: Call) -> Unit = {
        CallContent(
            call = call,
            modifier = modifier.testTag("call_content"),
            callDeviceState = callDeviceState,
            onCallAction = onCallAction,
            callAppBarContent = callAppBarContent,
            callControlsContent = callControlsContent,
            pictureInPictureContent = pictureInPictureContent
        )
    },
    ringingCallContent: @Composable (call: Call) -> Unit = {
        RingingCallContent(
            call = call,
            modifier = modifier.testTag("ringing_call_content"),
            isVideoType = isVideoType,
            callDeviceState = callDeviceState,
            onBackPressed = onBackPressed,
            onCallAction = onCallAction,
            onAcceptedContent = { callContent.invoke(it) },
            onRejectedContent = {}
        )
    },
) {
    ringingCallContent.invoke(call)
}

@Composable
internal fun DefaultCallContent(
    callViewModel: CallViewModel,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    onCallAction: (CallAction) -> Unit = callViewModel::onCallAction,
    callAppBarContent: @Composable (call: Call) -> Unit,
    callControlsContent: @Composable (call: Call) -> Unit,
    pictureInPictureContent: @Composable (call: Call) -> Unit = { DefaultPictureInPictureContent(it) }
) {
    CallContent(
        modifier = modifier,
        callViewModel = callViewModel,
        onBackPressed = onBackPressed,
        onCallAction = onCallAction,
        callAppBarContent = callAppBarContent,
        callControlsContent = callControlsContent,
        pictureInPictureContent = pictureInPictureContent
    )

    val isShowingParticipantsInfo by callViewModel.isShowingCallInfoMenu.collectAsStateWithLifecycle()
    val participantsState by callViewModel.call.state.participants.collectAsStateWithLifecycle()
    var usersToInvite by remember { mutableStateOf(emptyList<User>()) }

    if (isShowingParticipantsInfo && participantsState.isNotEmpty()) {
        CallParticipantsInfoMenu(
            modifier = Modifier
                .fillMaxSize()
                .background(VideoTheme.colors.appBackground),
            participants = participantsState,
            onDismiss = { callViewModel.dismissCallInfoMenu() },
        ) { action ->
            when (action) {
                is InviteUsersToCall -> {
                    usersToInvite = action.users
                    callViewModel.dismissCallInfoMenu()
                }

                is ToggleMicrophone -> onCallAction(action)

                else -> Unit
            }
        }
    }

    if (usersToInvite.isNotEmpty()) {
        InviteUsersDialog(
            users = usersToInvite,
            onDismiss = { usersToInvite = emptyList() },
            onInviteUsers = {
                usersToInvite = emptyList()
                callViewModel.onCallAction(InviteUsersToCall(it))
            }
        )
    }
}

@Preview
@Composable
private fun CallContainerPreview() {
    StreamMockUtils.initializeStreamVideo(LocalContext.current)
    VideoTheme {
        CallContainer(
            call = mockCall,
            isVideoType = true,
            callDeviceState = CallDeviceState()
        )
    }
}
