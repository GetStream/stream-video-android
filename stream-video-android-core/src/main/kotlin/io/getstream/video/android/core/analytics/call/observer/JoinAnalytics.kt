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

package io.getstream.video.android.core.analytics.call.observer

import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.call.observer.model.Stage
import io.getstream.video.android.core.analytics.call.observer.model.JoinAnalyticsModel
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import java.util.UUID

internal class JoinAnalytics(
    val callId: String,
    val callType: String,
    val eventReporter: ClientEventReporter,
    val joinAnalyticsStateHolder: JoinAnalyticsStateHolder,
    val onJoinSuccess: () -> Unit,
) {

    fun onJoinFunctionStart() {
        val stageAttemptId = UUID.randomUUID().toString()
        joinAnalyticsStateHolder.updateJoinStageAttemptId(stageAttemptId)
        eventReporter.reportSdkMethodJoinInitiated(
            callType = callType,
            callId = callId,
            joinStageAttemptId = stageAttemptId,
        )
    }

    fun onJoinRequestStart(joinReason: JoinReason?) {
        if (joinReason != JoinReason.FirstAttempt) {
            val stageAttemptId = UUID.randomUUID().toString()
            joinAnalyticsStateHolder.updateJoinStageAttemptId(stageAttemptId)
        }
        if (joinAnalyticsStateHolder.state.value.joinStage == Stage.NOT_STARTED) {
            joinAnalyticsStateHolder.updateJoinReason(joinReason)
            val stageId = eventReporter.reportCoordinatorJoinInitiated(
                callType = callType,
                callId = callId,
                joinStageAttemptId = joinAnalyticsStateHolder.state.value.joinStageAttemptId
                    ?: "unknown",
                joinReason = joinReason ?: JoinReason.Unknown,
            )
            joinAnalyticsStateHolder.updateStageId(stageId)
            joinAnalyticsStateHolder.updateStage(Stage.IN_PROGRESS)
        }
    }

    fun onJoinRequestSuccess(joinAnalyticsModel: JoinAnalyticsModel, currentSessionId: String) {
        if (joinAnalyticsStateHolder.state.value.joinStage == Stage.IN_PROGRESS) {
            joinAnalyticsStateHolder.updateCallSessionId(currentSessionId)
            if (joinAnalyticsStateHolder.state.value.stageId.isNotEmpty()) {
                eventReporter.reportCoordinatorJoinCompleted(
                    stageId = joinAnalyticsStateHolder.state.value.stageId,
                    success = true,
                    retryCount = joinAnalyticsModel.retryAttempt,
                    callSessionId = currentSessionId,
                )
                onJoinSuccess()
            }
            resetStage()
        }
    }

    fun onJoinRequestPermanentError(retryCount: Int, message: String) {
        if (joinAnalyticsStateHolder.state.value.joinStage == Stage.IN_PROGRESS) {
            if (joinAnalyticsStateHolder.state.value.stageId.isNotEmpty()) {
                eventReporter.reportCoordinatorJoinCompleted(
                    stageId = joinAnalyticsStateHolder.state.value.stageId,
                    success = false,
                    retryCount = retryCount,
                    failureReason = message,
                )
            }
            resetStage()
        }
    }

    fun onJoinRequestRetryExhausted(retryCount: Int, message: String) {
        onJoinRequestPermanentError(retryCount, message)
    }

    fun resetStage() {
        joinAnalyticsStateHolder.updateStage(Stage.NOT_STARTED)
    }
}
