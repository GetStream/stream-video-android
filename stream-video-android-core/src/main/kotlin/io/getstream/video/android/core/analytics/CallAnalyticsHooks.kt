/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.analytics

import io.getstream.video.android.core.CallLeaveReason
import io.getstream.video.android.core.events.reporting.ClientEventReporter

internal class CallAnalyticsHooks(val callId: String, val callType: String, val eventReporter: ClientEventReporter) {
    val joinRequestHooks = JoinRequestHooks(callId, callType, eventReporter)
    val wsHook = WsHook(callId, callType, eventReporter) {
        joinRequestHooks.joinStageAttemptId
    }

    val peerConnectionHook = PeerConnectionHook(callId, callType, eventReporter) {
        joinRequestHooks.joinStageAttemptId
    }

    fun onCallLeave(callLeaveReason: CallLeaveReason) {
        val abortReason = when (callLeaveReason) {
            is CallLeaveReason.Backend -> ClientEventReporter.AbortReason.BACKEND_LEAVE
            else -> ClientEventReporter.AbortReason.CLIENT_ABORTED
        }
        eventReporter.abortAllInFlight(abortReason)
    }
}
