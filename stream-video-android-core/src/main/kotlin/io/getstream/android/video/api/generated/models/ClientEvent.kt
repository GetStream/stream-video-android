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

package io.getstream.android.video.api.generated.models

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * A single client-side telemetry event. JoinInitiated is the top-level marker emitted when a user begins a join attempt and carries only join_attempt_id (no stage_id or coordinator_connect_id). When stage is CoordinatorJoin, CoordinatorWS, WSJoin, or PeerConnectionConnect the event reports a join-lifecycle attempt; initiation and completion of a stage attempt share the same stage_id. FirstAudioFrame and FirstVideoFrame report media readiness and only ever carry an initiated event. MediaDevicePermission reports the result of requesting screen-share, microphone, and camera permissions. Other stage values denote generic client events.
 */
public data class ClientEvent (
    @Json(name = "call_session_id")
    public val callSessionId: String? = null,

    @Json(name = "camera_permission_status")
    public val cameraPermissionStatus: String? = null,

    @Json(name = "coordinator_connect_id")
    public val coordinatorConnectId: String? = null,

    @Json(name = "elapsed_time")
    public val elapsedTime: Int? = null,

    @Json(name = "event_type")
    public val eventType: String? = null,

    @Json(name = "ice_state")
    public val iceState: String? = null,

    @Json(name = "id")
    public val id: String? = null,

    @Json(name = "join_attempt_id")
    public val joinAttemptId: String? = null,

    @Json(name = "join_reason")
    public val joinReason: String? = null,

    @Json(name = "leave_initiator")
    public val leaveInitiator: String? = null,

    @Json(name = "leave_reason")
    public val leaveReason: String? = null,

    @Json(name = "microphone_permission_status")
    public val microphonePermissionStatus: String? = null,

    @Json(name = "outcome")
    public val outcome: String? = null,

    @Json(name = "peer_connection")
    public val peerConnection: String? = null,

    @Json(name = "previously_connected_timestamp")
    public val previouslyConnectedTimestamp: kotlin.time.Instant? = null,

    @Json(name = "retry_count_attempt")
    public val retryCountAttempt: Int? = null,

    @Json(name = "retry_failure_code")
    public val retryFailureCode: String? = null,

    @Json(name = "retry_failure_reason")
    public val retryFailureReason: String? = null,

    @Json(name = "screen_share_status")
    public val screenShareStatus: String? = null,

    @Json(name = "sdk_version")
    public val sdkVersion: String? = null,

    @Json(name = "sfu_id")
    public val sfuId: String? = null,

    @Json(name = "source")
    public val source: String? = null,

    @Json(name = "stage")
    public val stage: String? = null,

    @Json(name = "stage_id")
    public val stageId: String? = null,

    @Json(name = "timestamp")
    public val timestamp: kotlin.time.Instant? = null,

    @Json(name = "track_id")
    public val trackId: String? = null,

    @Json(name = "type")
    public val type: String? = null,

    @Json(name = "user_agent")
    public val userAgent: String? = null,

    @Json(name = "user_id")
    public val userId: String? = null,

    @Json(name = "was_previously_connected")
    public val wasPreviouslyConnected: Boolean? = null
)
