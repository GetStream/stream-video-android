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

package io.getstream.video.android.core.analytics.observer

import io.getstream.video.android.core.analytics.observer.model.Stage
import io.getstream.video.android.core.analytics.observer.model.TelemetryModel
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import java.util.UUID

internal class JoinObserver(
    val callId: String,
    val callType: String,
    val eventReporter: ClientEventReporter,
    val onJoinSuccess: () -> Unit,
) {

    var stageId = ""
    var joinStage = Stage.NOT_STARTED
    var joinStageAttemptId = ""

    fun onJoinFunctionStart() {
        joinStageAttemptId = UUID.randomUUID().toString()
        eventReporter.reportSdkMethodJoinInitiated(
            callType = callType,
            callId = callId,
            joinStageAttemptId = joinStageAttemptId,
        )
    }

    fun onJoinRequestStart() {
        if (joinStage == Stage.NOT_STARTED) {
            stageId = eventReporter.reportCoordinatorJoinInitiated(
                callType = callType,
                callId = callId,
                joinStageAttemptId = joinStageAttemptId,
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
