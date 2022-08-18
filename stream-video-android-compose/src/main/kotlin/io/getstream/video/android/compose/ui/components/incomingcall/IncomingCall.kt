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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.getstream.video.android.model.CallType
import io.getstream.video.android.model.VideoParticipant

@Composable
public fun IncomingCall(
    callId: String,
    callType: CallType,
    participants: List<VideoParticipant>,
    onDeclineCall: () -> Unit,
    onAcceptCall: (String, Boolean) -> Unit
) {

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IncomingCallDetails(participants = participants)

        Spacer(modifier = Modifier.height(150.dp))

        IncomingCallOptions(
            callId = callId,
            callType = callType,
            onDeclineCall = onDeclineCall,
            onAcceptCall = onAcceptCall
        )
    }
}
