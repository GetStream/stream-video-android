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

package io.getstream.video.android.core.analytics.reporting

import io.getstream.android.video.generated.models.ClientEvent
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.coordinator.CoordinatorAnalyticsStateHolder
import io.getstream.video.android.core.analytics.reporting.model.EventOutcome
import io.getstream.video.android.core.analytics.reporting.model.EventStage
import io.getstream.video.android.core.analytics.reporting.model.EventType
import io.getstream.video.android.core.analytics.reporting.model.PeerConnectionRole
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import org.webrtc.PeerConnection

internal class ClientEventFactory(
    val sdkVersion: String,
    val userAgent: () -> String,
    val coordinatorAnalyticsStateHolder: CoordinatorAnalyticsStateHolder,
) {

    fun buildRequest(
        callId: String? = null,
        callType: String? = null,
        stage: EventStage,
        eventType: EventType,
        stageId: String? = null,
        joinReason: JoinReason? = null,
        joinStageAttemptId: String? = null,
        elapsedTime: Long? = null,
        outcome: EventOutcome? = null,
        retryCountAttempt: Int? = null,
        retryFailureReason: String? = null,
        retryFailureCode: String? = null,
        callSessionId: String? = null,
        sfuId: String? = null,
        peerConnection: PeerConnectionRole? = null,
        wasPreviouslyConnected: Boolean? = null,
        iceState: PeerConnection.IceConnectionState? = null,
        peerConnectionState: PeerConnection.PeerConnectionState? = null,
        userSessionId: String? = null,
        screenShareAllowed: Boolean? = null,
        microphoneAllowed: Boolean? = null,
        cameraAllowed: Boolean? = null,
        trackId: String? = null,
    ): ClientEvent = ClientEvent(
        stageId = stageId,
        joinAttemptId = joinStageAttemptId,
        eventType = eventType.value,
        id = callId,
        sdkVersion = sdkVersion,
        stage = stage.value,
        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
        type = callType,
        userAgent = userAgent.invoke().take(512),
        userId = StreamVideo.Companion.instanceOrNull()?.userId,
        callSessionId = callSessionId,
        elapsedTime = elapsedTime?.toInt(),
        iceState = iceState?.name,
        outcome = outcome?.value,
        peerConnection = peerConnection?.value,
        previouslyConnectedTimestamp = null,
        retryCountAttempt = retryCountAttempt,
        retryFailureCode = retryFailureCode,
        retryFailureReason = retryFailureReason,
        sfuId = sfuId,
        userSessionId = userSessionId,
        wasPreviouslyConnected = wasPreviouslyConnected,
        screenShareStatus = getPermissionStatusText(screenShareAllowed),
        microphonePermissionStatus = getPermissionStatusText(microphoneAllowed),
        cameraPermissionStatus = getPermissionStatusText(cameraAllowed),
        trackId = trackId,
        coordinatorConnectId = coordinatorAnalyticsStateHolder.coordinatorConnectId.value,
        joinReason = joinReason?.message,
    )

    fun getPermissionStatusText(allowed: Boolean?): String? {
        return if (allowed == true) "GRANTED" else if (allowed == false) "FAILED" else null
    }
}
