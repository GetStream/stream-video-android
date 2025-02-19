/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package org.openapitools.client.models

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

public abstract class VideoEvent {
    abstract fun getEventType(): kotlin.String

}


class VideoEventAdapter : JsonAdapter<VideoEvent>() {

    @FromJson
    override fun fromJson(reader: JsonReader): VideoEvent? {
        val peek = reader.peekJson()
        var eventType: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            if (reader.nextName() == "type") {
                eventType = reader.nextString()
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()

        return eventType?.let {
            peek.use { peekedReader ->
                org.openapitools.client.infrastructure.Serializer.moshi.adapter(getSubclass(eventType)).fromJson(peekedReader)
            }
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: VideoEvent?) {
        throw UnsupportedOperationException("toJson not implemented")
    }

    private fun getSubclass(type: String): Class<out VideoEvent> {
        return when (type) {
            "call.accepted" -> org.openapitools.client.models.CallAcceptedEvent::class.java
            "call.blocked_user" -> org.openapitools.client.models.BlockedUserEvent::class.java
            "call.closed_caption" -> org.openapitools.client.models.ClosedCaptionEvent::class.java
            "call.closed_captions_failed" -> org.openapitools.client.models.CallClosedCaptionsFailedEvent::class.java
            "call.closed_captions_started" -> org.openapitools.client.models.CallClosedCaptionsStartedEvent::class.java
            "call.closed_captions_stopped" -> org.openapitools.client.models.CallClosedCaptionsStoppedEvent::class.java
            "call.created" -> org.openapitools.client.models.CallCreatedEvent::class.java
            "call.deleted" -> org.openapitools.client.models.CallDeletedEvent::class.java
            "call.ended" -> org.openapitools.client.models.CallEndedEvent::class.java
            "call.hls_broadcasting_failed" -> org.openapitools.client.models.CallHLSBroadcastingFailedEvent::class.java
            "call.hls_broadcasting_started" -> org.openapitools.client.models.CallHLSBroadcastingStartedEvent::class.java
            "call.hls_broadcasting_stopped" -> org.openapitools.client.models.CallHLSBroadcastingStoppedEvent::class.java
            "call.live_started" -> org.openapitools.client.models.CallLiveStartedEvent::class.java
            "call.member_added" -> org.openapitools.client.models.CallMemberAddedEvent::class.java
            "call.member_removed" -> org.openapitools.client.models.CallMemberRemovedEvent::class.java
            "call.member_updated" -> org.openapitools.client.models.CallMemberUpdatedEvent::class.java
            "call.member_updated_permission" -> org.openapitools.client.models.CallMemberUpdatedPermissionEvent::class.java
            "call.missed" -> org.openapitools.client.models.CallMissedEvent::class.java
            "call.notification" -> org.openapitools.client.models.CallNotificationEvent::class.java
            "call.permission_request" -> org.openapitools.client.models.PermissionRequestEvent::class.java
            "call.permissions_updated" -> org.openapitools.client.models.UpdatedCallPermissionsEvent::class.java
            "call.reaction_new" -> org.openapitools.client.models.CallReactionEvent::class.java
            "call.recording_failed" -> org.openapitools.client.models.CallRecordingFailedEvent::class.java
            "call.recording_ready" -> org.openapitools.client.models.CallRecordingReadyEvent::class.java
            "call.recording_started" -> org.openapitools.client.models.CallRecordingStartedEvent::class.java
            "call.recording_stopped" -> org.openapitools.client.models.CallRecordingStoppedEvent::class.java
            "call.rejected" -> org.openapitools.client.models.CallRejectedEvent::class.java
            "call.ring" -> org.openapitools.client.models.CallRingEvent::class.java
            "call.rtmp_broadcast_failed" -> org.openapitools.client.models.CallRtmpBroadcastFailedEvent::class.java
            "call.rtmp_broadcast_started" -> org.openapitools.client.models.CallRtmpBroadcastStartedEvent::class.java
            "call.rtmp_broadcast_stopped" -> org.openapitools.client.models.CallRtmpBroadcastStoppedEvent::class.java
            "call.session_ended" -> org.openapitools.client.models.CallSessionEndedEvent::class.java
            "call.session_participant_count_updated" -> org.openapitools.client.models.CallSessionParticipantCountsUpdatedEvent::class.java
            "call.session_participant_joined" -> org.openapitools.client.models.CallSessionParticipantJoinedEvent::class.java
            "call.session_participant_left" -> org.openapitools.client.models.CallSessionParticipantLeftEvent::class.java
            "call.session_started" -> org.openapitools.client.models.CallSessionStartedEvent::class.java
            "call.transcription_failed" -> org.openapitools.client.models.CallTranscriptionFailedEvent::class.java
            "call.transcription_ready" -> org.openapitools.client.models.CallTranscriptionReadyEvent::class.java
            "call.transcription_started" -> org.openapitools.client.models.CallTranscriptionStartedEvent::class.java
            "call.transcription_stopped" -> org.openapitools.client.models.CallTranscriptionStoppedEvent::class.java
            "call.unblocked_user" -> org.openapitools.client.models.UnblockedUserEvent::class.java
            "call.updated" -> org.openapitools.client.models.CallUpdatedEvent::class.java
            "call.user_muted" -> org.openapitools.client.models.CallUserMutedEvent::class.java
            "connection.error" -> org.openapitools.client.models.ConnectionErrorEvent::class.java
            "connection.ok" -> org.openapitools.client.models.ConnectedEvent::class.java
            "custom" -> org.openapitools.client.models.CustomVideoEvent::class.java
            "health.check" -> org.openapitools.client.models.HealthCheckEvent::class.java
            "user.updated" -> org.openapitools.client.models.UserUpdatedEvent::class.java
            else -> UnsupportedVideoEvent::class.java
        }
    }
}

class UnsupportedVideoEvent(val type: String) : VideoEvent() {
    override fun getEventType(): String {
        return type
    }
}

class UnsupportedVideoEventException(val type: String) : Exception()
