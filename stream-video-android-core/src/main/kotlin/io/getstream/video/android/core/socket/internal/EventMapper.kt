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

import io.getstream.video.android.core.events.CallAcceptedEvent
import io.getstream.video.android.core.events.CallCanceledEvent
import io.getstream.video.android.core.events.CallCreatedEvent
import io.getstream.video.android.core.events.CallEndedEvent
import io.getstream.video.android.core.events.CallRejectedEvent
import io.getstream.video.android.core.events.CallUpdatedEvent
import io.getstream.video.android.core.events.CustomEvent
import io.getstream.video.android.core.events.HealthCheckEvent
import io.getstream.video.android.core.events.UnknownEvent
import io.getstream.video.android.core.events.VideoEvent
import io.getstream.video.android.core.model.toCallDetails
import io.getstream.video.android.core.model.toCallInfo
import io.getstream.video.android.core.model.toCallUsers
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
import org.openapitools.client.models.Callaccepted
import org.openapitools.client.models.Callcancelled
import org.openapitools.client.models.Callcreated
import org.openapitools.client.models.Callended
import org.openapitools.client.models.Callrejected
import org.openapitools.client.models.Callupdated
import org.openapitools.client.models.Custom
import stream.video.coordinator.client_v1_rpc.WebsocketEvent
import java.util.*

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
            val data = Json.decodeFromString<JsonObject>(text)
            val connectionId = data["connection_id"]?.jsonPrimitive?.content ?: ""

            HealthCheckEvent(clientId = connectionId)
        }

        CALL_CREATED -> {
            val event = Serializer.moshi.adapter(Callcreated::class.java).fromJson(text)!!

            CallCreatedEvent(
                callCid = event.call.cid,
                ringing = event.ringing,
                users = event.members.toCallUsers(),
                callInfo = event.call.toCallInfo(),
                callDetails = event.toCallDetails()
            )
        }
        CALL_ACCEPTED -> {
            val event = Serializer.moshi.adapter(Callaccepted::class.java).fromJson(text)!!

            CallAcceptedEvent(
                callCid = event.callCid,
                sentByUserId = event.user.id,
            )
        }
        CALL_REJECTED -> {
            val event = Serializer.moshi.adapter(Callrejected::class.java).fromJson(text)!!

            CallRejectedEvent(
                callCid = event.callCid,
                user = event.user.toUser(),
                updatedAt = Date(event.createdAt.toEpochSecond() * 1000)
            )
        }
        CALL_CANCELLED -> {
            val event = Serializer.moshi.adapter(Callcancelled::class.java).fromJson(text)!!

            CallCanceledEvent(
                callCid = event.callCid,
                sentByUserId = event.user.id,
            )
        }
        CALL_UPDATED -> {
            val event = Serializer.moshi.adapter(Callupdated::class.java).fromJson(text)!!

            CallUpdatedEvent(
                callCid = event.call.cid,
                capabilitiesByRole = event.capabilitiesByRole,
                info = event.call.toCallInfo(),
                ownCapabilities = event.call.ownCapabilities
            )
        }
        CALL_ENDED -> {
            val event = Serializer.moshi.adapter(Callended::class.java).fromJson(text)!!

            CallEndedEvent(
                callCid = event.callCid,
                endedByUser = event.user.toUser()
            )
        }
        PERMISSION_REQUEST -> {
            // TODO - implement permission request

            UnknownEvent
        }
        UPDATED_CALL_PERMISSIONS -> {
            // TODO - implement permission request

            UnknownEvent
        }
        CUSTOM -> {
            val event = Serializer.moshi.adapter(Custom::class.java).fromJson(text)!!

            CustomEvent(
                cid = event.callCid ?: "",
                sentByUser = event.user?.toUser(),
                custom = event.custom ?: emptyMap()
            )
        }

        else -> UnknownEvent
    }
}
