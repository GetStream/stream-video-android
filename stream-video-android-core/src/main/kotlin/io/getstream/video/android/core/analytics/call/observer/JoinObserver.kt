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
import io.getstream.video.android.core.analytics.call.observer.model.TelemetryModel
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import java.util.UUID

internal class JoinObserver(
    val callId: String,
    val callType: String,
    val eventReporter: ClientEventReporter,
    val joinAnalyticsRepository: JoinAnalyticsRepository,
    val onJoinSuccess: () -> Unit,
) {

    var stageId = ""
    var joinStage = Stage.NOT_STARTED

    fun onJoinFunctionStart() {
        val stageAttemptId = UUID.randomUUID().toString()
        joinAnalyticsRepository.updateJoinStageAttemptId(stageAttemptId)
        eventReporter.reportSdkMethodJoinInitiated(
            callType = callType,
            callId = callId,
            joinStageAttemptId = stageAttemptId,
        )
    }

    fun onJoinRequestStart(joinReason: JoinReason?) {
        if (joinStage == Stage.NOT_STARTED) {
            joinAnalyticsRepository.updateJoinReason(joinReason)
            stageId = eventReporter.reportCoordinatorJoinInitiated(
                callType = callType,
                callId = callId,
                joinStageAttemptId = joinAnalyticsRepository.state.value.joinStageAttemptId
                    ?: "unknown",
                joinReason = joinReason ?: JoinReason.Unknown,
            )
            joinStage = Stage.IN_PROGRESS
        }
    }

    fun onJoinRequestSuccess(telemetryModel: TelemetryModel, currentSessionId: String) {
        if (joinStage == Stage.IN_PROGRESS) {
            if (stageId.isNotEmpty()) {
                eventReporter.reportCoordinatorJoinCompleted(
                    stageId = stageId,
                    success = true,
                    retryCount = telemetryModel.retryAttempt,
                    callSessionId = currentSessionId,
                )
                onJoinSuccess()
            }
            resetStage()
        }
    }

    fun onJoinRequestPermanentError(retryCount: Int, message: String) {
        if (joinStage == Stage.IN_PROGRESS) {
            if (stageId.isNotEmpty()) {
                eventReporter.reportCoordinatorJoinCompleted(
                    stageId = stageId,
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
        joinStage = Stage.NOT_STARTED
    }
}
