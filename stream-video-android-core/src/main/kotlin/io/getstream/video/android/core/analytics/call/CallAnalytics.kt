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
import io.getstream.video.android.core.analytics.call.observer.AudioAnalytics
import io.getstream.video.android.core.analytics.call.observer.JoinAnalytics
import io.getstream.video.android.core.analytics.call.observer.JoinAnalyticsStateHolder
import io.getstream.video.android.core.analytics.call.observer.MediaPermissionObserver
import io.getstream.video.android.core.analytics.call.observer.PeerConnectionAnalytics
import io.getstream.video.android.core.analytics.call.observer.SfuAnalytics
import io.getstream.video.android.core.analytics.call.observer.SfuAnalyticsStateHolder
import io.getstream.video.android.core.analytics.call.observer.VideoAnalytics
import io.getstream.video.android.core.analytics.call.observer.VideoAnalyticsIceState
import io.getstream.video.android.core.analytics.call.observer.model.Stage
import io.getstream.video.android.core.analytics.call.observer.toVideoAnalyticsIceState
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import io.getstream.video.android.core.analytics.reporting.model.AnalyticsCallAbortReason
import io.getstream.video.android.core.call.RtcSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

internal class CallAnalytics(
    val context: Context,
    val callId: String,
    val callType: String,
    val myParticipantState: StateFlow<ParticipantState?>,
    val connectionFlow: StateFlow<RealtimeConnection>,
    val participants: StateFlow<List<ParticipantState>>,
    val eventReporter: ClientEventReporter,
    val observerScope: CoroutineScope,
) {
    val logger by taggedLogger("CallAnalyticsHooks")

    val joinAnalyticsStateHolder = JoinAnalyticsStateHolder()
    val sfuAnalyticsStateHolder = SfuAnalyticsStateHolder()

    val joinAnalytics = JoinAnalytics(callId, callType, eventReporter, joinAnalyticsStateHolder) {
        resetAfterJoinSuccess()
    }
    val sfuAnalytics =
        SfuAnalytics(
            true,
            callId,
            callType,
            connectionFlow,
            observerScope,
            eventReporter,
            joinAnalyticsStateHolder,
            sfuAnalyticsStateHolder,
        )
    val peerConnectionAnalytics =
        PeerConnectionAnalytics(
            callId,
            callType,
            observerScope,
            eventReporter,
            joinAnalyticsStateHolder,
            sfuAnalyticsStateHolder,
        )
    val mediaPermissionObserver =
        MediaPermissionObserver(context, callId, callType, eventReporter, joinAnalyticsStateHolder)

    val audioAnalytics =
        AudioAnalytics(
            callId,
            callType,
            eventReporter,
            joinAnalyticsStateHolder,
            sfuAnalyticsStateHolder,
            observerScope,
        )
    val videoAnalytics =
        VideoAnalytics(
            callId,
            callType,
            myParticipantState,
            eventReporter,
            joinAnalyticsStateHolder,
            sfuAnalyticsStateHolder,
        )

    fun resetAfterJoinSuccess() {
        videoAnalytics.reset()
        audioAnalytics.reset()
    }

    fun onCallLeave(session: StateFlow<RtcSession?>, callLeaveReason: CallLeaveReason) {
        val isAnyStageInProgress =
            joinAnalytics.joinAnalyticsStateHolder.state.value.joinStage == Stage.IN_PROGRESS ||
                sfuAnalytics.sfuAnalyticsStateHolder.stage.value == Stage.IN_PROGRESS ||
                peerConnectionAnalytics.stateHolder.state.value.publisherStage == Stage.IN_PROGRESS ||
                peerConnectionAnalytics.stateHolder.state.value.subscriberStage == Stage.IN_PROGRESS

        if (isAnyStageInProgress) {
            val pairCodeWithMessage = when (callLeaveReason) {
                is CallLeaveReason.Backend -> {
                    if (callLeaveReason.cause == io.getstream.video.android.core.BackendCause.LEAVE_TIMEOUT_AFTER_DISCONNECT) {
                        Pair(AnalyticsCallAbortReason.NETWORK_OFFLINE, callLeaveReason.message)
                    } else {
                        Pair(AnalyticsCallAbortReason.BACKEND_LEAVE, callLeaveReason.message)
                    }
                }
                is CallLeaveReason.UserAction -> {
                    Pair(AnalyticsCallAbortReason.CLIENT_ABORTED, callLeaveReason.message)
                }
                is CallLeaveReason.RetryExhausted -> {
                    Pair(AnalyticsCallAbortReason.RETRY_EXHAUSTED, callLeaveReason.message)
                }
                is CallLeaveReason.SdkDriven -> {
                    Pair(AnalyticsCallAbortReason.SDK_DRIVEN_ANDROID, callLeaveReason.message)
                }
                is CallLeaveReason.Custom -> {
                    Pair(AnalyticsCallAbortReason.CUSTOM, callLeaveReason.message)
                }
            }
            val publisherIceState =
                session.value?.publisher?.value?.iceState?.value?.toVideoAnalyticsIceState()
                    ?: VideoAnalyticsIceState.NOT_CONNECTED
            val subscriberIceState =
                session.value?.subscriber?.value?.iceState?.value?.toVideoAnalyticsIceState()
                    ?: VideoAnalyticsIceState.NOT_CONNECTED
            eventReporter.abortAllPostCallInFlight(
                publisherIceState,
                subscriberIceState,
                pairCodeWithMessage.first.name,
                pairCodeWithMessage.second,
            )
        }
    }

    fun stopObservers() {
        peerConnectionAnalytics.stop()
    }
}
