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

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package io.getstream.android.video.generated.models

import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.*
import kotlin.io.*
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * A single client-side telemetry event. When stage is CoordinatorJoin, WSJoin, or PeerConnectionConnect the event reports a join-lifecycle attempt; initiation and completion of a stage attempt share the same event_session_id. Other stage values denote generic client events.
 */

data class ClientEvent (
    @Json(name = "call_session_id")
    val callSessionId: kotlin.String? = null,

    @Json(name = "elapsed_time")
    val elapsedTime: kotlin.Int? = null,

    @Json(name = "event_session_id")
    val eventSessionId: kotlin.String? = null,

    @Json(name = "event_type")
    val eventType: kotlin.String? = null,

    @Json(name = "ice_state")
    val iceState: kotlin.String? = null,

    @Json(name = "id")
    val id: kotlin.String? = null,

    @Json(name = "outcome")
    val outcome: kotlin.String? = null,

    @Json(name = "peer_connection")
    val peerConnection: kotlin.String? = null,

    @Json(name = "previously_connected_timestamp")
    val previouslyConnectedTimestamp: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "retry_count_attempt")
    val retryCountAttempt: kotlin.Int? = null,

    @Json(name = "retry_failure_code")
    val retryFailureCode: kotlin.String? = null,

    @Json(name = "retry_failure_reason")
    val retryFailureReason: kotlin.String? = null,

    @Json(name = "sdk_version")
    val sdkVersion: kotlin.String? = null,

    @Json(name = "sfu_id")
    val sfuId: kotlin.String? = null,

    @Json(name = "stage")
    val stage: kotlin.String? = null,

    @Json(name = "timestamp")
    val timestamp: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "type")
    val type: kotlin.String? = null,

    @Json(name = "user_agent")
    val userAgent: kotlin.String? = null,

    @Json(name = "user_id")
    val userId: kotlin.String? = null,

    @Json(name = "user_session_id")
    val userSessionId: kotlin.String? = null,

    @Json(name = "was_previously_connected")
    val wasPreviouslyConnected: kotlin.Boolean? = null
) {

    internal fun toLog(): String {
        return buildString {
            append("ClientEvent(")

            appendIfNotNull("type", type)
            appendIfNotNull("eventType", eventType)
            appendIfNotNull("stage", stage)
            appendIfNotNull("outcome", outcome)

            appendIfNotNull("callSessionId", callSessionId)
            appendIfNotNull("eventSessionId", eventSessionId)
            appendIfNotNull("userSessionId", userSessionId)

            appendIfNotNull("userId", userId)
            appendIfNotNull("sfuId", sfuId)

            appendIfNotNull("peerConnection", peerConnection)
            appendIfNotNull("iceState", iceState)

            appendIfNotNull("retryCountAttempt", retryCountAttempt)
            appendIfNotNull("retryFailureCode", retryFailureCode)
            appendIfNotNull("retryFailureReason", retryFailureReason)

            appendIfNotNull("elapsedTime", elapsedTime)

            appendIfNotNull(
                "wasPreviouslyConnected",
                wasPreviouslyConnected
            )

            appendIfNotNull(
                "previouslyConnectedTimestamp",
                previouslyConnectedTimestamp
            )

            appendIfNotNull("sdkVersion", sdkVersion)
            appendIfNotNull("timestamp", timestamp)

            append(")")
        }
    }

    private fun StringBuilder.appendIfNotNull(
        key: String,
        value: Any?,
    ) {
        if (value == null) return

        if (!endsWith("(")) {
            append(",\n")
        }

        append(key)
        append("=")
        append(value)
    }
}
