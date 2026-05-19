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
 * Reports a single client-side join-lifecycle event (initiated or completed) for one of CoordinatorJoin, WSJoin, or PeerConnectionConnect. Initiation and completion of a stage attempt share the same event_session_id.
 */

data class ReportClientCallEventRequest (
    @Json(name = "event_session_id")
    val eventSessionId: kotlin.String,

    @Json(name = "event_type")
    val eventType: EventType,

    @Json(name = "id")
    val id: kotlin.String,

    @Json(name = "sdk_version")
    val sdkVersion: kotlin.String,

    @Json(name = "stage")
    val stage: Stage,

    @Json(name = "timestamp")
    val timestamp: org.threeten.bp.OffsetDateTime,

    @Json(name = "type")
    val type: kotlin.String,

    @Json(name = "user_agent")
    val userAgent: kotlin.String,

    @Json(name = "user_id")
    val userId: kotlin.String,

    @Json(name = "call_session_id")
    val callSessionId: kotlin.String? = null,

    @Json(name = "elapsed_time")
    val elapsedTime: kotlin.Int? = null,

    @Json(name = "ice_state")
    val iceState: IceState? = null,

    @Json(name = "outcome")
    val outcome: Outcome? = null,

    @Json(name = "peer_connection")
    val peerConnection: PeerConnection? = null,

    @Json(name = "previously_connected_timestamp")
    val previouslyConnectedTimestamp: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "retry_count_attempt")
    val retryCountAttempt: kotlin.Int? = null,

    @Json(name = "retry_failure_code")
    val retryFailureCode: kotlin.String? = null,

    @Json(name = "retry_failure_reason")
    val retryFailureReason: kotlin.String? = null,

    @Json(name = "sfu_id")
    val sfuId: kotlin.String? = null,

    @Json(name = "user_session_id")
    val userSessionId: kotlin.String? = null,

    @Json(name = "was_previously_connected")
    val wasPreviouslyConnected: kotlin.Boolean? = null
)
{
    
    /**
    * EventType Enum
    */
    sealed class EventType(val value: kotlin.String) {
            override fun toString(): String = value

            companion object {
                fun fromString(s: kotlin.String): EventType = when (s) {
                    "completed" -> Completed
                    "initiated" -> Initiated
                    else -> Unknown(s)
                }
            }
            object Completed : EventType("completed")
            object Initiated : EventType("initiated")
            data class Unknown(val unknownValue: kotlin.String) : EventType(unknownValue)
        

        class EventTypeAdapter : JsonAdapter<EventType>() {
            @FromJson
            override fun fromJson(reader: JsonReader): EventType? {
                val s = reader.nextString() ?: return null
                return EventType.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: EventType?) {
                writer.value(value?.value)
            }
        }
    }
    /**
    * Stage Enum
    */
    sealed class Stage(val value: kotlin.String) {
            override fun toString(): String = value

            companion object {
                fun fromString(s: kotlin.String): Stage = when (s) {
                    "CoordinatorJoin" -> CoordinatorJoin
                    "PeerConnectionConnect" -> PeerConnectionConnect
                    "WSJoin" -> WSJoin
                    else -> Unknown(s)
                }
            }
            object CoordinatorJoin : Stage("CoordinatorJoin")
            object PeerConnectionConnect : Stage("PeerConnectionConnect")
            object WSJoin : Stage("WSJoin")
            data class Unknown(val unknownValue: kotlin.String) : Stage(unknownValue)
        

        class StageAdapter : JsonAdapter<Stage>() {
            @FromJson
            override fun fromJson(reader: JsonReader): Stage? {
                val s = reader.nextString() ?: return null
                return Stage.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: Stage?) {
                writer.value(value?.value)
            }
        }
    }
    /**
    * IceState Enum
    */
    sealed class IceState(val value: kotlin.String) {
            override fun toString(): String = value

            companion object {
                fun fromString(s: kotlin.String): IceState = when (s) {
                    "CONNECTED" -> CONNECTED
                    "FAILED" -> FAILED
                    "NOT_CONNECTED" -> NOTCONNECTED
                    else -> Unknown(s)
                }
            }
            object CONNECTED : IceState("CONNECTED")
            object FAILED : IceState("FAILED")
            object NOTCONNECTED : IceState("NOT_CONNECTED")
            data class Unknown(val unknownValue: kotlin.String) : IceState(unknownValue)
        

        class IceStateAdapter : JsonAdapter<IceState>() {
            @FromJson
            override fun fromJson(reader: JsonReader): IceState? {
                val s = reader.nextString() ?: return null
                return IceState.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: IceState?) {
                writer.value(value?.value)
            }
        }
    }
    /**
    * Outcome Enum
    */
    sealed class Outcome(val value: kotlin.String) {
            override fun toString(): String = value

            companion object {
                fun fromString(s: kotlin.String): Outcome = when (s) {
                    "failure" -> Failure
                    "success" -> Success
                    else -> Unknown(s)
                }
            }
            object Failure : Outcome("failure")
            object Success : Outcome("success")
            data class Unknown(val unknownValue: kotlin.String) : Outcome(unknownValue)
        

        class OutcomeAdapter : JsonAdapter<Outcome>() {
            @FromJson
            override fun fromJson(reader: JsonReader): Outcome? {
                val s = reader.nextString() ?: return null
                return Outcome.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: Outcome?) {
                writer.value(value?.value)
            }
        }
    }
    /**
    * PeerConnection Enum
    */
    sealed class PeerConnection(val value: kotlin.String) {
            override fun toString(): String = value

            companion object {
                fun fromString(s: kotlin.String): PeerConnection = when (s) {
                    "publish" -> Publish
                    "subscribe" -> Subscribe
                    else -> Unknown(s)
                }
            }
            object Publish : PeerConnection("publish")
            object Subscribe : PeerConnection("subscribe")
            data class Unknown(val unknownValue: kotlin.String) : PeerConnection(unknownValue)
        

        class PeerConnectionAdapter : JsonAdapter<PeerConnection>() {
            @FromJson
            override fun fromJson(reader: JsonReader): PeerConnection? {
                val s = reader.nextString() ?: return null
                return PeerConnection.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: PeerConnection?) {
                writer.value(value?.value)
            }
        }
    }    
}
