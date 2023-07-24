package io.getstream.video.android.ui.call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.api.models.querysort.QuerySortByField
import io.getstream.chat.android.client.models.Filters
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.client.utils.onSuccessSuspend
import io.getstream.chat.android.state.extensions.globalState
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.ringing.incomingcall.IncomingCallContent
import io.getstream.video.android.compose.ui.components.call.ringing.outgoingcall.OutgoingCallContent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.call.state.AcceptCall
import io.getstream.video.android.core.call.state.CallAction
import io.getstream.video.android.core.call.state.DeclineCall
import io.getstream.video.android.core.call.state.LeaveCall
import io.getstream.video.android.core.call.state.ToggleCamera
import io.getstream.video.android.core.call.state.ToggleMicrophone
import io.getstream.video.android.core.call.state.ToggleSpeakerphone

/**
 * This screen handles the complete Call lifecycle - from ringing screens, to meeting screen and
 * UI actions.
 */
@Composable
fun CallContentScreen(
    modifier: Modifier = Modifier,
    call: Call,
    onAcceptCall: () -> Unit = {},
    onLeaveCall: () -> Unit = {},
    onDeclineCall: () -> Unit = {},
    onRejecteCall: () -> Unit = {},
) {
    val ringingState by call.state.ringingState.collectAsStateWithLifecycle()

    val onPreCallAction: (CallAction) -> Unit = { callAction ->
        when (callAction) {
            is ToggleCamera -> call.camera.setEnabled(callAction.isEnabled)
            is ToggleMicrophone -> call.microphone.setEnabled(callAction.isEnabled)
            is ToggleSpeakerphone -> call.speaker.setEnabled(callAction.isEnabled)
            is LeaveCall -> {
                onLeaveCall.invoke()
            }
            is DeclineCall -> {
                onDeclineCall.invoke()
            }
            is AcceptCall -> {
                onAcceptCall.invoke()
            }

            else -> Unit
        }
    }

    VideoTheme {
        when (ringingState) {
            RingingState.Incoming -> {
                IncomingCallContent(
                    call = call,
                    modifier = modifier,
                    onBackPressed = onLeaveCall,
                    onCallAction = onPreCallAction
                )
            }

            RingingState.Outgoing -> {
                OutgoingCallContent(
                    call = call,
                    modifier = modifier,
                    onBackPressed = onLeaveCall,
                    onCallAction = onPreCallAction
                )
            }

            RingingState.RejectedByAll -> {
                onRejecteCall.invoke()
            }

            RingingState.TimeoutNoAnswer -> {
                //
            }

            RingingState.Active -> {
                InCallScreenContent(
                    call = call,
                    onLeaveCall = onLeaveCall,
                    showDebugOptions = io.getstream.video.android.BuildConfig.DEBUG
                )
            }

            RingingState.Idle -> {
                // Call state is not ready yet? Show loading?
            }
        }
    }

    // (optional) - chat integration
    val user: User? by ChatClient.instance().globalState.user.collectAsState(null)
    LaunchedEffect(key1 = user) {
        if (user != null) {
            val channel = ChatClient.instance().channel("videocall", call.id)
            channel.queryMembers(
                offset = 0,
                limit = 10,
                filter = Filters.neutral(),
                sort = QuerySortByField()
            ).await().onSuccessSuspend { members ->
                if (members.isNotEmpty()) {
                    channel.addMembers(listOf(user!!.id)).await()
                } else {
                    channel.create(listOf(user!!.id), emptyMap()).await()
                }
            }
        }
    }
}