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

import android.content.Context
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.CallLeaveReason
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.analytics.reporting.AnalyticsCallAbortReason
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal class CallAnalyticsCoordinator(
    val context: Context,
    val callId: String,
    val callType: String,
    val connectionFlow: StateFlow<RealtimeConnection>,
    val participants: StateFlow<List<ParticipantState>>,
    val eventReporter: ClientEventReporter,
    val scope: CoroutineScope,
) {
    val logger by taggedLogger("CallAnalyticsHooks")
    val joinRequestHooks = JoinRequestHooks(callId, callType, eventReporter) {
        resetAfterJoinSuccess()
    }
    val wsHook = WsHook(callId, callType, connectionFlow, scope, eventReporter) {
        joinRequestHooks.joinStageAttemptId
    }

    val peerConnectionAnalyticsObserver =
        PeerConnectionAnalyticsObserver(callId, callType, scope, eventReporter) {
            joinRequestHooks.joinStageAttemptId
        }

    val mediaPermissionHook = MediaPermissionHook(context, callId, callType, eventReporter) {
        joinRequestHooks.joinStageAttemptId
    }
    val audioAnalytics = AudioAnalytics(callId, callType, eventReporter, { wsHook.sfuName }) {
        joinRequestHooks.joinStageAttemptId
    }
    val videoAnalytics = VideoAnalytics(callId, callType, eventReporter, { wsHook.sfuName }) {
        joinRequestHooks.joinStageAttemptId
    }

    fun resetAfterJoinSuccess() {
        audioAnalytics.reset()
        videoAnalytics.reset()
        audioAnalytics.observeParticipantsForFirstRemoteAudioFrame(participants, scope)
    }

    fun onCallLeave(callLeaveReason: CallLeaveReason) {
        val isAnyStageInProgress =
            joinRequestHooks.joinStage == Stage.IN_PROGRESS ||
                wsHook.wsStage == Stage.IN_PROGRESS ||
                peerConnectionAnalyticsObserver.publisherStage == Stage.IN_PROGRESS ||
                peerConnectionAnalyticsObserver.subscriberStage == Stage.IN_PROGRESS
        logger.d { "noob isAnyStageInProgress:$isAnyStageInProgress" }

        if (isAnyStageInProgress) {
            val abortReason = when (callLeaveReason) {
                is CallLeaveReason.Backend -> AnalyticsCallAbortReason.BACKEND_LEAVE
                else -> AnalyticsCallAbortReason.CLIENT_ABORTED
            }
            eventReporter.abortAllPostCallInFlight(abortReason)
        }
    }

    fun stopObservers() {
        peerConnectionAnalyticsObserver.stop()
    }
}
