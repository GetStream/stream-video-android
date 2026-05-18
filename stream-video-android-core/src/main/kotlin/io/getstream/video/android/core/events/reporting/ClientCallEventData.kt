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

package io.getstream.video.android.core.events.reporting

import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter

internal enum class CallEventStage(val value: String) {
    COORDINATOR_JOIN("coordinator_join"),
    WS_JOIN("ws_join"),
    PEER_CONNECTION_CONNECT("peer_connection_connect"),
}

internal enum class CallEventType(val value: String) {
    INITIATED("initiated"),
    COMPLETED("completed"),
}

internal enum class CallEventOutcome(val value: String) {
    SUCCESS("success"),
    FAILURE("failure"),
}

internal enum class PeerConnectionRole(val value: String) {
    PUBLISH("publish"),
    SUBSCRIBE("subscribe"),
}

internal fun buildEventMap(
    userId: String,
    callType: String,
    callId: String,
    callCid: String,
    stage: CallEventStage,
    eventType: CallEventType,
    eventSessionId: String,
    joinSuccessId: String,
    userAgent: String,
    sdkVersion: String,
    elapsedTime: Long? = null,
    outcome: CallEventOutcome? = null,
    retryCountAttempt: Int? = null,
    retryFailureReason: String? = null,
    retryFailureCode: String? = null,
    callSessionId: String? = null,
    sfuId: String? = null,
    peerConnection: PeerConnectionRole? = null,
    wasPreviouslyConnected: Boolean? = null,
    iceState: String? = null,
    dtlsState: String? = null,
    userSessionId: String? = null,
): Map<String, Any> = buildMap {
    put("user_id", userId)
    put("type", callType)
    put("id", callId) // review with Gulzar: already in URL
    put("call_cid", callCid) // review: already in URL
    put("stage", stage.value)
    put("event_type", eventType.value)
    put("event_session_id", eventSessionId)
    put("join_success_id", joinSuccessId)
    put("timestamp", currentRfc3339Timestamp())
    put("user_agent", userAgent.take(512))
    put("sdk_version", sdkVersion) // review: already comes in header
    elapsedTime?.let { put("elapsed_time", it) }
    outcome?.let { put("outcome", it.value) }
    retryCountAttempt?.let { put("retry_count_attempt", it) }
    retryFailureReason?.let { put("retry_failure_reason", it) }
    retryFailureCode?.let { put("retry_failure_code", it) }
    callSessionId?.let { put("call_session_id", it) }
    sfuId?.let { put("sfu_id", it) }
    peerConnection?.let { put("peer_connection", it.value) }
    wasPreviouslyConnected?.let { put("was_previously_connected", it) }
    iceState?.let { put("ice_state", it) }
    dtlsState?.let { put("dtls_state", it) }
    userSessionId?.let { put("user_session_id", it) }
}

internal fun currentRfc3339Timestamp(): String =
    OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
