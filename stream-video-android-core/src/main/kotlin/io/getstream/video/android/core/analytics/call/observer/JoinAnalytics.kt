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

import io.getstream.video.android.core.analytics.call.observer.model.JoinAnalyticsModel
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.call.observer.model.Stage
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import java.util.UUID

internal class JoinAnalytics(
    val callId: String,
    val callType: String,
    val eventReporter: ClientEventReporter,
    val joinAnalyticsStateHolder: JoinAnalyticsStateHolder,
    val onJoinSuccess: () -> Unit,
) {

    /**
     * Records the invocation of the public `Call.join()` API.
     *
     * Each invocation mints a new [JoinTelemetryState.joinStageAttemptId], including
     * coalesced and already-joined calls.
     */
    fun onJoinFunctionStart() {
        val stageAttemptId = UUID.randomUUID().toString()
        joinAnalyticsStateHolder.updateJoinStageAttemptId(stageAttemptId)
        eventReporter.reportSdkMethodJoinInitiated(
            callType = callType,
            callId = callId,
            joinStageAttemptId = stageAttemptId,
        )
    }

    /**
     * Records the start of a Coordinator join API request.
     *
     * @param joinReason The reason for starting the join request, such as an initial join or rejoin.
     */
    fun onJoinRequestStart(joinReason: JoinReason?) {
        if (joinReason != JoinReason.FirstAttempt) {
            val stageAttemptId = UUID.randomUUID().toString()
            joinAnalyticsStateHolder.updateJoinStageAttemptId(stageAttemptId)
        }
        when (joinAnalyticsStateHolder.state.value.joinStage) {
            Stage.NOT_STARTED, Stage.COMPLETED -> {
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

            else -> {}
        }
    }

    /**
     * Records the successful completion of a Coordinator join API request.
     *
     * @param joinAnalyticsModel Analytics metadata for the completed join attempt.
     * @param currentSessionId The call session ID returned by the Coordinator.
     */
    fun onJoinRequestSuccess(joinAnalyticsModel: JoinAnalyticsModel, currentSessionId: String) {
        when (joinAnalyticsStateHolder.state.value.joinStage) {
            Stage.IN_PROGRESS -> {
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
                joinAnalyticsStateHolder.updateStage(Stage.COMPLETED)
            }
            else -> {}
        }
    }

    /**
     * Records a terminal join failure when the error is permanent and no retry can be performed.
     * The public `Call.join()` API exits with a failure after this event is reported.
     *
     * @param retryCount The number of retries performed before the permanent failure.
     * @param failureCode The analytics code identifying the failure.
     * @param message A description of the failure.
     */
    fun onJoinRequestPermanentError(retryCount: Int, failureCode: String, message: String) {
        when (joinAnalyticsStateHolder.state.value.joinStage) {
            Stage.IN_PROGRESS -> {
                if (joinAnalyticsStateHolder.state.value.stageId.isNotEmpty()) {
                    eventReporter.reportCoordinatorJoinCompleted(
                        stageId = joinAnalyticsStateHolder.state.value.stageId,
                        success = false,
                        retryCount = retryCount,
                        failureReason = message,
                        failureCode = failureCode,
                    )
                }
                joinAnalyticsStateHolder.updateStage(Stage.COMPLETED)
            }
            else -> {}
        }
    }

    /**
     * Records a terminal join failure after all retry attempts to connect to the Coordinator or
     * SFU have been exhausted.
     *
     * @param retryCount The total number of retries performed.
     * @param failureCode The analytics code identifying the failure.
     * @param message A description of the failure.
     */
    fun onJoinRequestRetryExhausted(retryCount: Int, failureCode: String, message: String) {
        onJoinRequestPermanentError(retryCount, failureCode, message)
    }

    fun resetStage() {
        joinAnalyticsStateHolder.updateStage(Stage.NOT_STARTED)
    }
}
