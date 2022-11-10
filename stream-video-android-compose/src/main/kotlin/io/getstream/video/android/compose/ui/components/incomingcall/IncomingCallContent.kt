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

package io.getstream.video.android.compose.ui.components.incomingcall

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.CallTopAppbar
import io.getstream.video.android.compose.ui.components.background.CallBackground
import io.getstream.video.android.compose.ui.components.mock.mockParticipantList
import io.getstream.video.android.model.CallType
import io.getstream.video.android.model.CallUser
import io.getstream.video.android.viewmodel.CallViewModel
import io.getstream.video.android.viewmodel.IncomingCallViewModel

@Composable
public fun IncomingCallScreen(
    viewModel: CallViewModel,
    onDeclineCall: () -> Unit,
    onAcceptCall: () -> Unit,
    onVideoToggleChanged: (Boolean) -> Unit,
) {
    val callType: CallType by viewModel.callType.collectAsState()
    val participants: List<CallUser> by viewModel.participants.collectAsState()
    IncomingCall(
        participants = participants,
        callType = callType,
        onDeclineCall = onDeclineCall,
        onAcceptCall = onAcceptCall,
        onVideoToggleChanged = onVideoToggleChanged
    )
}

@Composable
public fun IncomingCallScreen(
    viewModel: IncomingCallViewModel,
    onDeclineCall: () -> Unit,
    onAcceptCall: () -> Unit,
    onVideoToggleChanged: (Boolean) -> Unit,
) {
    val callType: CallType by viewModel.callType.collectAsState()
    val participants: List<CallUser> by viewModel.participants.collectAsState()
    IncomingCall(
        participants = participants,
        callType = callType,
        onDeclineCall = onDeclineCall,
        onAcceptCall = onAcceptCall,
        onVideoToggleChanged = onVideoToggleChanged
    )
}

@Composable
public fun IncomingCall(
    participants: List<CallUser>,
    callType: CallType,
    onDeclineCall: () -> Unit,
    onAcceptCall: () -> Unit,
    onVideoToggleChanged: (Boolean) -> Unit,
) {
    CallBackground(
        participants = participants,
        callType = callType,
        isIncoming = true
    ) {

        Column {

            CallTopAppbar()

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
                .padding(bottom = 44.dp),
            isVideoCall = callType == CallType.VIDEO,
            onDeclineCall = onDeclineCall,
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
                    createdAt = null,
                    updatedAt = null,
                    imageUrl = it.profileImageURL ?: ""
                )
            },
            callType = CallType.VIDEO,
            onDeclineCall = { },
            onAcceptCall = { },
            onVideoToggleChanged = { }
        )
    }
}
