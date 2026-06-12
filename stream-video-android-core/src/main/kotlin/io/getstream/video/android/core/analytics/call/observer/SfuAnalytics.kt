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

import io.getstream.android.video.generated.models.ClientEvent
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.call.observer.model.Stage
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import io.getstream.video.android.core.analytics.reporting.dispatcher.EventDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class SfuAnalytics(
    val isEnabled: Boolean,
    val callId: String,
    val callType: String,
    val connectionFlow: StateFlow<RealtimeConnection>,
    val observerScope: CoroutineScope,
    val reporter: ClientEventReporter,
    val joinAnalyticsStateHolder: JoinAnalyticsStateHolder,
    val sfuAnalyticsStateHolder: SfuAnalyticsStateHolder,
) {

    companion object {
        internal fun getFakeSfuAnalytics(): SfuAnalytics {
            return SfuAnalytics(
                false,
                "",
                "",
                MutableStateFlow<RealtimeConnection>(
                    RealtimeConnection.PreJoin,
                ),
                CoroutineScope(Dispatchers.Default),
                ClientEventReporter(
                    sender = object :
                        EventDispatcher {
                        override fun send(event: ClientEvent) {}

                        override fun sendAll(events: List<ClientEvent>) {}
                    },
                    userAgent = { "" },
                    sdkVersion = "",
                ),
                JoinAnalyticsStateHolder(),
                SfuAnalyticsStateHolder(),
            )
        }
    }

    fun onSfuWsInitiated(wasPreviouslyConnected: Boolean) {
        if (!isEnabled) return
        when (sfuAnalyticsStateHolder.stage.value) {
            Stage.NOT_STARTED, Stage.COMPLETED -> {
                val wsEventStageId = reporter.reportSfuWsJoinInitiated(
                    callId = callId,
                    callType = callType,
                    sfuId = sfuAnalyticsStateHolder.sfuId.value,
                    wasPreviouslyConnected = wasPreviouslyConnected,
                    joinStageAttemptId = joinAnalyticsStateHolder.state.value.joinStageAttemptId
                        ?: "unknown",
                    joinReason = joinAnalyticsStateHolder.state.value.joinReason
                        ?: JoinReason.Unknown,
                    callSessionId = joinAnalyticsStateHolder.state.value.callSessionId,
                )
                sfuAnalyticsStateHolder.updateStage(Stage.IN_PROGRESS)
                sfuAnalyticsStateHolder.updateStageId(wsEventStageId)
            } else -> {}
        }
    }

    fun onSfuWsCompleted(
        success: Boolean,
        retryCount: Int,
        failureReason: String? = null,
        failureCode: String? = null,
    ) {
        if (!isEnabled) return
        if (sfuAnalyticsStateHolder.stage.value == Stage.IN_PROGRESS) {
            if (sfuAnalyticsStateHolder.stageId.value.isNotEmpty()) {
                reporter.reportSfuWsJoinCompleted(
                    stageId = sfuAnalyticsStateHolder.stageId.value,
                    success = success,
                    retryCount = retryCount,
                    failureReason = failureReason,
                    failureCode = failureCode,
                    joinStageAttemptId = joinAnalyticsStateHolder.state.value.joinStageAttemptId ?: "unknown",
                )
            }
            sfuAnalyticsStateHolder.updateStage(Stage.COMPLETED)
        }
    }

    fun resetStage() {
        sfuAnalyticsStateHolder.updateStage(Stage.NOT_STARTED)
    }
}
