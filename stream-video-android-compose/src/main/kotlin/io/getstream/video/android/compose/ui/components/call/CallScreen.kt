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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import io.getstream.video.android.compose.ui.components.incomingcall.IncomingCallScreen
import io.getstream.video.android.compose.ui.components.outgoingcall.OutgoingCallScreen
import io.getstream.video.android.viewmodel.CallViewModel
import io.getstream.video.android.model.state.StreamCallState as State

@Composable
public fun CallScreen(
    viewModel: CallViewModel,
    onRejectCall: () -> Unit,
    onAcceptCall: () -> Unit,
    onCancelCall: () -> Unit = {},
    onMicToggleChanged: (Boolean) -> Unit = {},
    onVideoToggleChanged: (Boolean) -> Unit,
) {
    val stateHolder = viewModel.streamCallState.collectAsState(initial = State.Idle)
    val state = stateHolder.value
    if (state is State.Incoming && !state.acceptedByMe) {
        IncomingCallScreen(
            viewModel = viewModel,
            onDeclineCall = onRejectCall,
            onAcceptCall = onAcceptCall,
            onVideoToggleChanged = onVideoToggleChanged
        )
    } else if (state is State.Outgoing && !state.acceptedByCallee) {
        OutgoingCallScreen(
            viewModel = viewModel,
            onCancelCall = { onCancelCall() },
            onMicToggleChanged = onMicToggleChanged,
            onVideoToggleChanged = onVideoToggleChanged
        )
    } else {
        CallContent(
            callViewModel = viewModel,
            onLeaveCall = onCancelCall
        )
    }
}
