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

import io.getstream.video.android.core.events.reporting.ClientEventReporter
import io.getstream.video.android.core.events.reporting.TelemetryModel
import java.util.UUID

internal class JoinRequestHooks(val callId: String, val callType: String, val eventReporter: ClientEventReporter) {

    var eventSessionId = ""
    var joinStage = Stage.NOT_STARTED
    var joinStageAttemptId = ""
    fun onJoinRequestStart() {
        if (joinStage == Stage.NOT_STARTED) {
            joinStageAttemptId = UUID.randomUUID().toString()
            eventSessionId = eventReporter.reportCoordinatorJoinInitiated(
                callType = callType,
                callId = callId,
                joinStageAttemptId = joinStageAttemptId,
            )
            joinStage = Stage.IN_PROGRESS
        }
    }
    fun onJoinRequestSuccess(telemetryModel: TelemetryModel, currentSessionId: String) {
        if (joinStage == Stage.IN_PROGRESS) {
            if (eventSessionId.isNotEmpty()) {
                eventReporter.reportCoordinatorJoinCompleted(
                    eventSessionId = eventSessionId,
                    success = true,
                    retryCount = telemetryModel.retryAttempt,
                    callSessionId = currentSessionId,
                )
            }
            resetStage()
        }
    }

    fun onJoinRequestPermanentError(retryCount: Int, message: String) {
        if (joinStage == Stage.IN_PROGRESS) {
            if (eventSessionId.isNotEmpty()) {
                eventReporter.reportCoordinatorJoinCompleted(
                    eventSessionId = eventSessionId,
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
