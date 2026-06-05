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

import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.analytics.observer.model.Stage
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal class SfuSocketObserver(
    val callId: String,
    val callType: String,
    val connectionFlow: StateFlow<RealtimeConnection>,
    val scope: CoroutineScope,
    val reporter: ClientEventReporter,
    val getJoinStageAttemptId: () -> String,
) {
    var telemetryWsEventStageId = ""
    var wsStage = Stage.NOT_STARTED

    var sfuName: String = ""

    fun onWsInitiated(sfuName: String, wasPreviouslyConnected: Boolean) {
        if (wsStage == Stage.NOT_STARTED) {
            this.sfuName = sfuName
            telemetryWsEventStageId = reporter.reportWsJoinInitiated(
                callId = callId,
                callType = callType,
                sfuId = sfuName,
                wasPreviouslyConnected = wasPreviouslyConnected,
                joinStageAttemptId = getJoinStageAttemptId.invoke(),
            )
            wsStage = Stage.IN_PROGRESS
        }
    }

    fun onWsCompleted(
        success: Boolean,
        retryCount: Int,
        failureReason: String? = null,
        failureCode: String? = null,
    ) {
        if (wsStage == Stage.IN_PROGRESS) {
            if (telemetryWsEventStageId.isNotEmpty()) {
                reporter.reportWsJoinCompleted(
                    stageId = telemetryWsEventStageId,
                    success = success,
                    retryCount = retryCount,
                    failureReason = failureReason,
                    failureCode = failureCode,
                    joinStageAttemptId = getJoinStageAttemptId.invoke(),
                )
            }
            resetStage()
        }
    }

    fun resetStage() {
        wsStage = Stage.NOT_STARTED
    }
}
