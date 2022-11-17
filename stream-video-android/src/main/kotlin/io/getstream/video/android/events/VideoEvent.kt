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

import io.getstream.video.android.model.CallDetails
import io.getstream.video.android.model.CallInfo
import io.getstream.video.android.model.CallUser

/**
 * Represents the events coming in from the socket.
 */
public sealed class VideoEvent : java.io.Serializable

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
    val callCid: String,
    val ringing: Boolean,
    val users: Map<String, CallUser>,
    val info: CallInfo,
    val details: CallDetails
) : VideoEvent()

/**
 * Sent when a call gets updated.
 */
public data class CallUpdatedEvent(
    val callCid: String,
    val users: Map<String, CallUser>,
    val info: CallInfo,
    val details: CallDetails
) : VideoEvent()

/**
 * Sent when a calls gets ended.
 */
public data class CallEndedEvent(
    val callCid: String,
    val users: Map<String, CallUser>,
    val info: CallInfo,
    val details: CallDetails
) : VideoEvent()

/**
 * Sent when call members get updated.
 */
public data class CallMembersUpdatedEvent(
    val callCid: String,
    val users: Map<String, CallUser>,
    val info: CallInfo,
    val details: CallDetails
) : VideoEvent()

/**
 * Sent when call members get updated.
 */
public data class CallMembersDeletedEvent(
    val callCid: String,
    val users: Map<String, CallUser>,
    val info: CallInfo,
    val details: CallDetails
) : VideoEvent()

public data class CallAcceptedEvent(
    val callCid: String,
    val sentByUserId: String,
    val users: Map<String, CallUser>,
    val info: CallInfo,
    val details: CallDetails
) : VideoEvent()

public data class CallRejectedEvent(
    val callCid: String,
    val sentByUserId: String,
    val users: Map<String, CallUser>,
    val info: CallInfo,
    val details: CallDetails
) : VideoEvent()

public data class CallCanceledEvent(
    val callCid: String,
    val sentByUserId: String,
    val users: Map<String, CallUser>,
    val info: CallInfo,
    val details: CallDetails
) : VideoEvent()

public object UnknownEvent : VideoEvent()
