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
    "UnusedImport",
)

package io.getstream.android.video.generated.models

import com.squareup.moshi.FromJson
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
                io.getstream.android.video.generated.infrastructure.Serializer.moshi.adapter(
                    getSubclass(eventType),
                ).fromJson(peekedReader)
            }
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: VideoEvent?) {
        throw UnsupportedOperationException("toJson not implemented")
    }

    private fun getSubclass(type: String): Class<out VideoEvent> {
        return when (type) {
            "call.accepted" -> io.getstream.android.video.generated.models.CallAcceptedEvent::class.java
            "call.blocked_user" -> io.getstream.android.video.generated.models.BlockedUserEvent::class.java
            "call.closed_caption" -> io.getstream.android.video.generated.models.ClosedCaptionEvent::class.java
            "call.closed_captions_failed" -> io.getstream.android.video.generated.models.CallClosedCaptionsFailedEvent::class.java
            "call.closed_captions_started" -> io.getstream.android.video.generated.models.CallClosedCaptionsStartedEvent::class.java
            "call.closed_captions_stopped" -> io.getstream.android.video.generated.models.CallClosedCaptionsStoppedEvent::class.java
            "call.created" -> io.getstream.android.video.generated.models.CallCreatedEvent::class.java
            "call.deleted" -> io.getstream.android.video.generated.models.CallDeletedEvent::class.java
            "call.ended" -> io.getstream.android.video.generated.models.CallEndedEvent::class.java
            "call.hls_broadcasting_failed" -> io.getstream.android.video.generated.models.CallHLSBroadcastingFailedEvent::class.java
            "call.hls_broadcasting_started" -> io.getstream.android.video.generated.models.CallHLSBroadcastingStartedEvent::class.java
            "call.hls_broadcasting_stopped" -> io.getstream.android.video.generated.models.CallHLSBroadcastingStoppedEvent::class.java
            "call.live_started" -> io.getstream.android.video.generated.models.CallLiveStartedEvent::class.java
            "call.member_added" -> io.getstream.android.video.generated.models.CallMemberAddedEvent::class.java
            "call.member_removed" -> io.getstream.android.video.generated.models.CallMemberRemovedEvent::class.java
            "call.member_updated" -> io.getstream.android.video.generated.models.CallMemberUpdatedEvent::class.java
            "call.member_updated_permission" -> io.getstream.android.video.generated.models.CallMemberUpdatedPermissionEvent::class.java
            "call.missed" -> io.getstream.android.video.generated.models.CallMissedEvent::class.java
            "call.notification" -> io.getstream.android.video.generated.models.CallNotificationEvent::class.java
            "call.permission_request" -> io.getstream.android.video.generated.models.PermissionRequestEvent::class.java
            "call.permissions_updated" -> io.getstream.android.video.generated.models.UpdatedCallPermissionsEvent::class.java
            "call.reaction_new" -> io.getstream.android.video.generated.models.CallReactionEvent::class.java
            "call.recording_failed" -> io.getstream.android.video.generated.models.CallRecordingFailedEvent::class.java
            "call.recording_ready" -> io.getstream.android.video.generated.models.CallRecordingReadyEvent::class.java
            "call.recording_started" -> io.getstream.android.video.generated.models.CallRecordingStartedEvent::class.java
            "call.recording_stopped" -> io.getstream.android.video.generated.models.CallRecordingStoppedEvent::class.java
            "call.rejected" -> io.getstream.android.video.generated.models.CallRejectedEvent::class.java
            "call.ring" -> io.getstream.android.video.generated.models.CallRingEvent::class.java
            "call.rtmp_broadcast_failed" -> io.getstream.android.video.generated.models.CallRtmpBroadcastFailedEvent::class.java
            "call.rtmp_broadcast_started" -> io.getstream.android.video.generated.models.CallRtmpBroadcastStartedEvent::class.java
            "call.rtmp_broadcast_stopped" -> io.getstream.android.video.generated.models.CallRtmpBroadcastStoppedEvent::class.java
            "call.session_ended" -> io.getstream.android.video.generated.models.CallSessionEndedEvent::class.java
            "call.session_participant_count_updated" -> io.getstream.android.video.generated.models.CallSessionParticipantCountsUpdatedEvent::class.java
            "call.session_participant_joined" -> io.getstream.android.video.generated.models.CallSessionParticipantJoinedEvent::class.java
            "call.session_participant_left" -> io.getstream.android.video.generated.models.CallSessionParticipantLeftEvent::class.java
            "call.session_started" -> io.getstream.android.video.generated.models.CallSessionStartedEvent::class.java
            "call.transcription_failed" -> io.getstream.android.video.generated.models.CallTranscriptionFailedEvent::class.java
            "call.transcription_ready" -> io.getstream.android.video.generated.models.CallTranscriptionReadyEvent::class.java
            "call.transcription_started" -> io.getstream.android.video.generated.models.CallTranscriptionStartedEvent::class.java
            "call.transcription_stopped" -> io.getstream.android.video.generated.models.CallTranscriptionStoppedEvent::class.java
            "call.unblocked_user" -> io.getstream.android.video.generated.models.UnblockedUserEvent::class.java
            "call.updated" -> io.getstream.android.video.generated.models.CallUpdatedEvent::class.java
            "call.user_muted" -> io.getstream.android.video.generated.models.CallUserMutedEvent::class.java
            "connection.error" -> io.getstream.android.video.generated.models.ConnectionErrorEvent::class.java
            "connection.ok" -> io.getstream.android.video.generated.models.ConnectedEvent::class.java
            "custom" -> io.getstream.android.video.generated.models.CustomVideoEvent::class.java
            "health.check" -> io.getstream.android.video.generated.models.HealthCheckEvent::class.java
            "user.updated" -> io.getstream.android.video.generated.models.UserUpdatedEvent::class.java
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
