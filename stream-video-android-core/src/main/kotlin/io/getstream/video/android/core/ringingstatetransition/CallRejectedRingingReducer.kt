/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.ringingstatetransition

import io.getstream.android.video.generated.models.CallRejectedEvent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.REJECT_REASON_TIMEOUT
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.model.RejectReason
import kotlinx.coroutines.flow.StateFlow

/**
 * TODO Rahul is there any-case where I should ignore the CallRejectedEvent? Maybe in group-calls
 */
internal class CallRejectedRingingReducer(
    private val call: Call,
    private val rejectedBy: StateFlow<Set<String>>,
    private val members: StateFlow<List<MemberState>>,
    private val streamVideo: StreamVideo
) : RingingStateReducer2<CallRejectedEvent, RingingState.RejectedByAll, RingingState.TimeoutNoAnswer> {
    override fun reduce(
        originalState: RingingState,
        event: CallRejectedEvent,
    ): ReducerOutput2<RingingState, CallRejectedEvent, RingingState.RejectedByAll, RingingState.TimeoutNoAnswer> {
        val outgoingMembersCount = members.value.filter {
            it.user.id != streamVideo.userId
        }.size

        val rejectReason = event.reason?.let {
            when (it) {
                RejectReason.Busy.alias -> RejectReason.Busy
                RejectReason.Cancel.alias -> RejectReason.Cancel
                RejectReason.Decline.alias -> RejectReason.Decline
                else -> RejectReason.Custom(alias = it)
            }
        }

        if (rejectReason?.alias == REJECT_REASON_TIMEOUT) {
            return ReducerOutput2.SecondOption(RingingState.TimeoutNoAnswer)
        } else {
            return ReducerOutput2.FirstOption(RingingState.RejectedByAll)
        }
    }
}
