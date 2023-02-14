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
import io.getstream.video.android.core.events.CallMembersDeletedEvent
import io.getstream.video.android.core.events.CallMembersUpdatedEvent
import io.getstream.video.android.core.events.CallRejectedEvent
import io.getstream.video.android.core.events.CallUpdatedEvent
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.openapitools.client.infrastructure.Serializer
import org.openapitools.client.models.CallCreated
import stream.video.coordinator.client_v1_rpc.WebsocketEvent

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
            val event = Serializer.moshi.adapter(CallCreated::class.java).fromJson(text)!!

            CallCreatedEvent(
                callCid = event.call.cid,
                ringing = event.ringing,
                users = event.members.toCallUsers(),
                info = event.call.toCallInfo(),
            )
        }
        CALL_ACCEPTED -> TODO()
        CALL_REJECTED -> TODO()
        CALL_CANCELLED -> TODO()
        CALL_UPDATED -> TODO()
        CALL_ENDED -> TODO()
        PERMISSION_REQUEST -> TODO()
        UPDATED_CALL_PERMISSIONS -> TODO()
        CUSTOM -> TODO()

        else -> UnknownEvent
    }

    /**
     * Maps [WebsocketEvent]s to our [VideoEvent] that corresponds to the data.
     *
     * @param socketEvent The event we received through the WebSocket.
     * @return [VideoEvent] representation of the data.
     */
    internal fun mapEvent(socketEvent: WebsocketEvent): VideoEvent = when {
        socketEvent.healthcheck != null -> with(socketEvent.healthcheck) {
            HealthCheckEvent(clientId = client_id)
        }

        socketEvent.call_created != null -> with(socketEvent.call_created) {
            CallCreatedEvent(
                callCid = call!!.call_cid,
                ringing = ringing,
                users = socketEvent.users.toCallUsers(),
                info = call.toCallInfo(),
            )
        }

        socketEvent.call_updated != null -> with(socketEvent.call_updated) {
            CallUpdatedEvent(
                callCid = call!!.call_cid,
                users = socketEvent.users.toCallUsers(),
                info = call.toCallInfo(),
                details = call_details.toCallDetails(),
            )
        }

        socketEvent.call_ended != null -> with(socketEvent.call_ended) {
            CallEndedEvent(
                callCid = call!!.call_cid,
                users = socketEvent.users.toCallUsers(),
                info = call.toCallInfo(),
                details = call_details.toCallDetails(),
            )
        }

        socketEvent.call_members_updated != null -> with(socketEvent.call_members_updated) {
            CallMembersUpdatedEvent(
                callCid = call!!.call_cid,
                users = socketEvent.users.toCallUsers(),
                info = call.toCallInfo(),
                details = call_details.toCallDetails(),
            )
        }

        socketEvent.call_members_deleted != null -> with(socketEvent.call_members_deleted) {
            CallMembersDeletedEvent(
                callCid = call!!.call_cid,
                users = socketEvent.users.toCallUsers(),
                info = call.toCallInfo(),
                details = call_details.toCallDetails(),
            )
        }

        socketEvent.call_accepted != null -> with(socketEvent.call_accepted) {
            CallAcceptedEvent(
                callCid = call!!.call_cid,
                sentByUserId = sender_user_id,
                users = socketEvent.users.toCallUsers(),
                info = call.toCallInfo(),
                details = call_details.toCallDetails(),
            )
        }

        socketEvent.call_rejected != null -> with(socketEvent.call_rejected) {
            CallRejectedEvent(
                callCid = call!!.call_cid,
                sentByUserId = sender_user_id,
                users = socketEvent.users.toCallUsers(),
                info = call.toCallInfo(),
                details = call_details.toCallDetails(),
            )
        }

        socketEvent.call_cancelled != null -> with(socketEvent.call_cancelled) {
            CallCanceledEvent(
                callCid = call!!.call_cid,
                sentByUserId = sender_user_id,
                users = socketEvent.users.toCallUsers(),
                info = call.toCallInfo(),
                details = call_details.toCallDetails(),
            )
        }

        else -> UnknownEvent
    }
}
