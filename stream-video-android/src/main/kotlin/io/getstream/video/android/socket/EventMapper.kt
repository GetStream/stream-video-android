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

package io.getstream.video.android.socket

import io.getstream.video.android.events.CallAcceptedEvent
import io.getstream.video.android.events.CallCanceledEvent
import io.getstream.video.android.events.CallCreatedEvent
import io.getstream.video.android.events.CallEndedEvent
import io.getstream.video.android.events.CallMembersDeletedEvent
import io.getstream.video.android.events.CallMembersUpdatedEvent
import io.getstream.video.android.events.CallRejectedEvent
import io.getstream.video.android.events.CallUpdatedEvent
import io.getstream.video.android.events.HealthCheckEvent
import io.getstream.video.android.events.UnknownEvent
import io.getstream.video.android.events.VideoEvent
import io.getstream.video.android.model.toCallDetails
import io.getstream.video.android.model.toCallInfo
import io.getstream.video.android.model.toCallUsers
import stream.video.coordinator.client_v1_rpc.WebsocketEvent

internal object EventMapper {

    /**
     * Maps [WebsocketEvent]s to our [VideoEvent] that corresponds to the data.
     *
     * @param socketEvent The event we received through the WebSocket.
     * @return [VideoEvent] representation of the data.
     */
    internal fun mapEvent(socketEvent: WebsocketEvent): VideoEvent = when {
        socketEvent.healthcheck != null -> with(socketEvent.healthcheck) {
            HealthCheckEvent(userId = user_id, clientId = client_id)
        }

        socketEvent.call_created != null -> with(socketEvent.call_created) {
            CallCreatedEvent(
                callId = call_cid,
                users = socketEvent.users.toCallUsers(),
                info = socketEvent.calls[call_cid]?.toCallInfo(),
                details = socketEvent.call_details[call_cid]?.toCallDetails(),
            )
        }

        socketEvent.call_updated != null -> with(socketEvent.call_updated) {
            CallUpdatedEvent(
                callId = call_cid,
                users = socketEvent.users.toCallUsers(),
                info = socketEvent.calls[call_cid]?.toCallInfo(),
                details = socketEvent.call_details[call_cid]?.toCallDetails(),
            )
        }

        socketEvent.call_ended != null -> with(socketEvent.call_ended) {
            CallEndedEvent(
                callId = call_cid,
                users = socketEvent.users.toCallUsers(),
                info = socketEvent.calls[call_cid]?.toCallInfo(),
                details = socketEvent.call_details[call_cid]?.toCallDetails(),
            )
        }

        socketEvent.call_members_updated != null -> with(socketEvent.call_members_updated) {
            CallMembersUpdatedEvent(
                callId = call_cid,
                users = socketEvent.users.toCallUsers(),
                info = socketEvent.calls[call_cid]?.toCallInfo(),
                details = socketEvent.call_details[call_cid]?.toCallDetails(),
            )
        }

        socketEvent.call_members_deleted != null -> with(socketEvent.call_members_deleted) {
            CallMembersDeletedEvent(
                callId = call_cid,
                users = socketEvent.users.toCallUsers(),
                info = socketEvent.calls[call_cid]?.toCallInfo(),
                details = socketEvent.call_details[call_cid]?.toCallDetails(),
            )
        }

        socketEvent.call_accepted != null -> CallAcceptedEvent

        socketEvent.call_rejected != null -> CallRejectedEvent

        socketEvent.call_cancelled != null -> CallCanceledEvent

//        TODO - do we remove these?
//        socketEvent.audio_muted != null -> with(socketEvent.audio_muted) {
//            AudioMutedEvent(user_id ?: "", call!!, all_users ?: false)
//        }
//
//        socketEvent.audio_unmuted != null -> with(socketEvent.audio_unmuted) {
//            AudioUnmutedEvent(user_id, call!!)
//        }
//
//        socketEvent.video_started != null -> with(socketEvent.video_started) {
//            VideoStartedEvent(user_id, call!!)
//        }
//
//        socketEvent.video_stopped != null -> with(socketEvent.video_stopped) {
//            VideoStoppedEvent(user_id, call!!)
//        }
//
//        socketEvent.participant_joined != null -> with(socketEvent.participant_joined) {
//            ParticipantJoinedEvent(participant!!)
//        }
//
//        socketEvent.participant_left != null -> with(socketEvent.participant_left) {
//            ParticipantLeftEvent(participant!!)
//        }

        else -> UnknownEvent
    }
}
