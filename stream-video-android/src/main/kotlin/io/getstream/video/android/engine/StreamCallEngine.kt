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

package io.getstream.video.android.engine

import io.getstream.video.android.errors.VideoError
import io.getstream.video.android.model.CallMetadata
import io.getstream.video.android.model.JoinedCall
import io.getstream.video.android.model.state.StreamCallState
import kotlinx.coroutines.flow.StateFlow
import stream.video.coordinator.client_v1_rpc.UserEventType

internal interface StreamCallEngine {

    val callState: StateFlow<StreamCallState>

    fun onCallJoined(joinedCall: JoinedCall)

    fun onCallStarting(
        type: String,
        id: String,
        participantIds: List<String>,
        ringing: Boolean,
        forcedNewCall: Boolean
    )

    fun onCallStarted(call: CallMetadata)

    fun onCallJoining(call: CallMetadata)

    fun onCallFailed(error: VideoError)

    fun onCallEventSending(cid: String, eventType: UserEventType)

    fun onCallEventSent(cid: String, eventType: UserEventType)
}
