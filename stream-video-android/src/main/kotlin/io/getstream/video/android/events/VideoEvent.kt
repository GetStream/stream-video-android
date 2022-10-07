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

package io.getstream.video.android.events

import io.getstream.video.android.events.model.CallDetails
import io.getstream.video.android.events.model.CallInfo
import io.getstream.video.android.events.model.CallUser
import stream.video.coordinator.participant_v1.Participant
import stream.video.sfu.Call

/**
 * Represents the events coming in from the socket.
 */
public sealed class VideoEvent

/**
 * Triggered when a user gets connected to the WS.
 */
public data class ConnectedEvent(
    val clientId: String,
) : VideoEvent()

/**
 * Sent periodically by the server to keep the connection alive.
 */
public data class HealthCheckEvent(
    val clientId: String,
    val userId: String
) : VideoEvent()

/**
 * Sent when someone creates a call and invites another person to participate.
 */
public data class CallCreatedEvent(
    val callId: String,
    val users: Map<String, CallUser>,
    val info: CallInfo?,
    val details: CallDetails?
) : VideoEvent()

/**
 * Sent when a call gets started.
 */
public data class CallStartedEvent(
    val callId: String,
    val users: Map<String, CallUser>,
    val info: CallInfo?,
    val details: CallDetails?
) : VideoEvent()

/**
 * Sent when a call gets updated.
 */
public data class CallUpdatedEvent(
    val callId: String,
    val users: Map<String, CallUser>,
    val info: CallInfo?,
    val details: CallDetails?
) : VideoEvent()

/**
 * Sent when a calls gets ended.
 */
public data class CallEndedEvent(
    val callId: String,
    val users: Map<String, CallUser>,
    val info: CallInfo?,
    val details: CallDetails?
) : VideoEvent()

/**
 * Sent when call members get updated.
 */
public data class CallMembersUpdatedEvent(
    val callId: String,
    val users: Map<String, CallUser>,
    val info: CallInfo?,
    val details: CallDetails?
) : VideoEvent()

/**
 * Sent when call members get updated.
 */
public data class CallMembersDeletedEvent(
    val callId: String,
    val users: Map<String, CallUser>,
    val info: CallInfo?,
    val details: CallDetails?
) : VideoEvent()

/**
 * Triggered whenever a user's audio is unmuted or started.
 */
public data class AudioUnmutedEvent(
    val userId: String,
    val call: Call
) : VideoEvent()

/**
 * Triggered whenever a user's audio is muted. Either responds to everyone being muted in a call, or just one
 * user.
 */
public data class AudioMutedEvent(
    val userId: String,
    val call: Call,
    val areAllUsersMuted: Boolean
) : VideoEvent()

/**
 * Triggered whenever a user's video starts.
 */
public data class VideoStartedEvent(
    val userId: String,
    val call: Call
) : VideoEvent()

/**
 * Triggered whenever a user's video is stopped.
 */
public data class VideoStoppedEvent(
    val userId: String,
    val call: Call
) : VideoEvent()

/**
 * Triggered when someone joins a call.
 */
public data class ParticipantJoinedEvent(
    val participant: Participant
) : VideoEvent()

/**
 * Triggered when someone leaves a call.
 */
public data class ParticipantLeftEvent(
    val participant: Participant
) : VideoEvent()

public object UnknownEvent : VideoEvent()

// TODO - rest of the events
