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

package io.getstream.video.android.core.analytics.call

import android.content.Context
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.CallLeaveReason
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.analytics.call.observer.AudioObserver
import io.getstream.video.android.core.analytics.call.observer.JoinAnalyticsRepository
import io.getstream.video.android.core.analytics.call.observer.JoinObserver
import io.getstream.video.android.core.analytics.call.observer.MediaPermissionObserver
import io.getstream.video.android.core.analytics.call.observer.PeerConnectionObserver
import io.getstream.video.android.core.analytics.call.observer.SfuSocketObserver
import io.getstream.video.android.core.analytics.call.observer.VideoObserver
import io.getstream.video.android.core.analytics.call.observer.model.Stage
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import io.getstream.video.android.core.analytics.reporting.model.AnalyticsCallAbortReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal class CallAnalytics(
    val context: Context,
    val callId: String,
    val callType: String,
    val connectionFlow: StateFlow<RealtimeConnection>,
    val participants: StateFlow<List<ParticipantState>>,
    val eventReporter: ClientEventReporter,
    val scope: CoroutineScope,
) {
    val logger by taggedLogger("CallAnalyticsHooks")
    val joinAnalyticsRepository = JoinAnalyticsRepository()

    val joinObserver = JoinObserver(callId, callType, eventReporter, joinAnalyticsRepository) {
        resetAfterJoinSuccess()
    }
    val sfuSocketObserver =
        SfuSocketObserver(
            callId,
            callType,
            connectionFlow,
            scope,
            eventReporter,
            joinAnalyticsRepository,
        )
    val peerConnectionObserver =
        PeerConnectionObserver(callId, callType, scope, eventReporter, joinAnalyticsRepository)
    val mediaPermissionObserver =
        MediaPermissionObserver(context, callId, callType, eventReporter, joinAnalyticsRepository)
    val audioObserver =
        AudioObserver(callId, callType, eventReporter, joinAnalyticsRepository, {
            sfuSocketObserver.sfuName
        })
    val videoObserver =
        VideoObserver(callId, callType, eventReporter, joinAnalyticsRepository, {
            sfuSocketObserver.sfuName
        })

    fun resetAfterJoinSuccess() {
        audioObserver.reset()
        videoObserver.reset()
        audioObserver.observeParticipantsForFirstRemoteAudioFrame(participants, scope)
    }

    fun onCallLeave(callLeaveReason: CallLeaveReason) {
        val isAnyStageInProgress =
            joinObserver.joinStage == Stage.IN_PROGRESS ||
                sfuSocketObserver.wsStage == Stage.IN_PROGRESS ||
                peerConnectionObserver.publisherStage == Stage.IN_PROGRESS ||
                peerConnectionObserver.subscriberStage == Stage.IN_PROGRESS
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
        peerConnectionObserver.stop()
    }
}
