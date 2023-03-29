/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.socket.internal

import io.getstream.video.android.core.events.BlockedUserEvent
import io.getstream.video.android.core.events.CallAcceptedEvent
import io.getstream.video.android.core.events.CallCancelledEvent
import io.getstream.video.android.core.events.CallCreatedEvent
import io.getstream.video.android.core.events.CallEndedEvent
import io.getstream.video.android.core.events.CallRejectedEvent
import io.getstream.video.android.core.events.CallUpdatedEvent
import io.getstream.video.android.core.events.CustomEvent
import io.getstream.video.android.core.events.HealthCheckEvent
import io.getstream.video.android.core.events.PermissionRequestEvent
import io.getstream.video.android.core.events.RecordingStartedEvent
import io.getstream.video.android.core.events.RecordingStoppedEvent
import io.getstream.video.android.core.events.UnblockedUserEvent
import io.getstream.video.android.core.events.UnknownEvent
import io.getstream.video.android.core.events.UpdatedCallPermissionsEvent
import io.getstream.video.android.core.events.VideoEvent
import io.getstream.video.android.core.model.toCallDetails
import io.getstream.video.android.core.model.toCallInfo
import io.getstream.video.android.core.model.toCallUsers
import io.getstream.video.android.core.socket.internal.EventType.BLOCKED_USER
import io.getstream.video.android.core.socket.internal.EventType.CALL_ACCEPTED
import io.getstream.video.android.core.socket.internal.EventType.CALL_CANCELLED
import io.getstream.video.android.core.socket.internal.EventType.CALL_CREATED
import io.getstream.video.android.core.socket.internal.EventType.CALL_ENDED
import io.getstream.video.android.core.socket.internal.EventType.CALL_REJECTED
import io.getstream.video.android.core.socket.internal.EventType.CALL_UPDATED
import io.getstream.video.android.core.socket.internal.EventType.CUSTOM
import io.getstream.video.android.core.socket.internal.EventType.HEALTH_CHECK
import io.getstream.video.android.core.socket.internal.EventType.PERMISSION_REQUEST
import io.getstream.video.android.core.socket.internal.EventType.UPDATED_CALL_PERMISSIONS
import io.getstream.video.android.core.utils.toUser
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.openapitools.client.infrastructure.Serializer
import org.openapitools.client.models.CallRecordingStartedEvent
import org.openapitools.client.models.CallRecordingStoppedEvent
import org.openapitools.client.models.CustomVideoEvent
import stream.video.coordinator.client_v1_rpc.WebsocketEvent
import java.util.Date

internal object EventMapper {

    /**
     * Maps [WebsocketEvent]s to our [VideoEvent] that corresponds to the data.
     *
     * @param eventType The type of event we received through the WebSocket.
     * @param text JSON representation of the event.
     * @return [VideoEvent] representation of the data.
     */
    internal fun mapEvent(
        eventType: EventType,
        text: String
    ): VideoEvent = when (eventType) {
        HEALTH_CHECK -> {
//            val event =
//                Serializer.moshi.adapter(
//                    org.openapitools.client.models.HealthCheckEvent::class.java
//                ).fromJson(text)!!
//
//            HealthCheckEvent(clientId = "") // TODO - missing from BE, reimpl when available

            val data = Json.decodeFromString<JsonObject>(text)
            val connectionId = data["connection_id"]?.jsonPrimitive?.content ?: ""

            HealthCheckEvent(clientId = connectionId)
        }

        CALL_CREATED -> {
            val event =
                Serializer.moshi.adapter(
                    org.openapitools.client.models.CallCreatedEvent::class.java
                ).fromJson(text)!!

            CallCreatedEvent(
                callCid = event.call.cid,
                ringing = event.ringing,
                users = event.members.toCallUsers(),
                callInfo = event.call.toCallInfo(),
                callDetails = event.toCallDetails()
            )
        }
        CALL_ACCEPTED -> {
            val event = Serializer.moshi.adapter(
                org.openapitools.client.models.CallAcceptedEvent::class.java
            ).fromJson(text)!!

            CallAcceptedEvent(
                callCid = event.callCid,
                sentByUserId = event.user.id,
            )
        }
        CALL_REJECTED -> {
            val event = Serializer.moshi.adapter(
                org.openapitools.client.models.CallRejectedEvent::class.java
            ).fromJson(text)!!

            CallRejectedEvent(
                callCid = event.callCid,
                user = event.user.toUser(),
                updatedAt = Date(event.createdAt.toEpochSecond() * 1000)
            )
        }
        CALL_CANCELLED -> {
            val event = Serializer.moshi.adapter(
                org.openapitools.client.models.CallCancelledEvent::class.java
            ).fromJson(text)!!

            CallCancelledEvent(
                callCid = event.callCid,
                sentByUserId = event.user.id,
            )
        }
        CALL_UPDATED -> {
            val event = Serializer.moshi.adapter(
                org.openapitools.client.models.CallUpdatedEvent::class.java
            ).fromJson(text)!!

            CallUpdatedEvent(
                callCid = event.call.cid,
                capabilitiesByRole = event.capabilitiesByRole,
                info = event.call.toCallInfo(),
                ownCapabilities = event.call.ownCapabilities
            )
        }
        CALL_ENDED -> {
            val event = Serializer.moshi.adapter(
                org.openapitools.client.models.CallEndedEvent::class.java
            ).fromJson(text)!!

            CallEndedEvent(
                callCid = event.callCid,
                endedByUser = event.user?.toUser()
            )
        }
        PERMISSION_REQUEST -> {
            val event =
                Serializer.moshi.adapter(org.openapitools.client.models.PermissionRequestEvent::class.java)
                    .fromJson(text)!!

            PermissionRequestEvent(
                cid = event.callCid,
                type = event.type,
                permissions = event.permissions,
                user = event.user.toUser()
            )
        }
        UPDATED_CALL_PERMISSIONS -> {
            val event =
                Serializer.moshi.adapter(org.openapitools.client.models.UpdatedCallPermissionsEvent::class.java)
                    .fromJson(text)!!

            UpdatedCallPermissionsEvent(
                cid = event.callCid,
                type = event.type,
                ownCapabilities = event.ownCapabilities,
                user = event.user.toUser()
            )
        }
        BLOCKED_USER -> {
            val event = Serializer.moshi.adapter(
                org.openapitools.client.models.BlockedUserEvent::class.java
            ).fromJson(text)!!

            BlockedUserEvent(
                cid = event.callCid,
                type = event.type,
                userId = event.userId
            )
        }
        EventType.UNBLOCKED_USER -> {
            val event = Serializer.moshi.adapter(
                org.openapitools.client.models.UnblockedUserEvent::class.java
            ).fromJson(text)!!

            UnblockedUserEvent(
                cid = event.callCid,
                type = event.type,
                userId = event.userId
            )
        }
        EventType.RECORDING_STARTED -> {
            val event = Serializer.moshi.adapter(
                CallRecordingStartedEvent::class.java
            ).fromJson(text)!!

            RecordingStartedEvent(
                event.callCid,
                event.callCid,
                event.type
            )
        }
        EventType.RECORDING_STOPPED -> {
            val event = Serializer.moshi.adapter(
                CallRecordingStoppedEvent::class.java
            ).fromJson(text)!!

            RecordingStoppedEvent(
                event.callCid,
                event.type
            )
        }
        CUSTOM -> {
            val event = Serializer.moshi.adapter(CustomVideoEvent::class.java).fromJson(text)!!

            CustomEvent(
                cid = event.callCid,
                sentByUser = event.user.toUser(),
                custom = event.custom
            )
        }

        else -> UnknownEvent
    }
}
