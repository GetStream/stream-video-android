/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.openapitools.client.infrastructure.Serializer

/**
 * The discriminator object for all websocket events, you should use this to map event payloads to their own type
 *
 */

public abstract class VideoEvent {
    abstract fun getEventType(): String
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
                Serializer.moshi.adapter(getSubclass(eventType)).fromJson(peekedReader)
            }
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: VideoEvent?) {
        throw UnsupportedOperationException("toJson not implemented")
    }

    private fun getSubclass(type: String): Class<out VideoEvent> {
        return when (type) {
            "call.accepted" -> CallAcceptedEvent::class.java
            "call.blocked_user" -> BlockedUserEvent::class.java
            "call.created" -> CallCreatedEvent::class.java
            "call.ended" -> CallEndedEvent::class.java
            "call.member_added" -> CallMemberAddedEvent::class.java
            "call.member_removed" -> CallMemberRemovedEvent::class.java
            "call.member_updated" -> CallMemberUpdatedEvent::class.java
            "call.permission_request" -> PermissionRequestEvent::class.java
            "call.permissions_updated" -> UpdatedCallPermissionsEvent::class.java
            "call.reaction_new" -> CallReactionEvent::class.java
            "call.recording_started" -> CallRecordingStartedEvent::class.java
            "call.recording_stopped" -> CallRecordingStoppedEvent::class.java
            "call.rejected" -> CallRejectedEvent::class.java
            "call.unblocked_user" -> UnblockedUserEvent::class.java
            "call.updated" -> CallUpdatedEvent::class.java
            "call.updated_permission" -> CallMemberUpdatedPermissionEvent::class.java
            "connection.ok" -> ConnectedEvent::class.java
            "custom" -> CustomVideoEvent::class.java
            "health.check" -> HealthCheckEvent::class.java
            else -> throw IllegalArgumentException("Unknown type: $type")
        }
    }
}
