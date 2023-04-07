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

package io.getstream.video.android.core.events

import io.getstream.video.android.core.model.CallDetails
import io.getstream.video.android.core.model.CallInfo
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.model.StreamCallCid
import io.getstream.video.android.core.model.User
import org.openapitools.client.models.OwnCapability
import java.util.Date

/**
 * Represents the events coming in from the socket.
 */
public sealed class VideoEvent(open val callCid: String = "") : java.io.Serializable

public sealed class CoordinatorEvent() : VideoEvent()

/**
 * Triggered when a user gets connected to the WS.
 */
public data class ConnectedEvent(
    val clientId: String,
) : CoordinatorEvent()

/**
 * Sent periodically by the server to keep the connection alive.
 */
public data class CoordinatorHealthCheckEvent(
    val clientId: String,
) : CoordinatorEvent()

/**
 * Sent when someone creates a call and invites another person to participate.
 */
public data class CallCreatedEvent(
    override val callCid: String,
    val ringing: Boolean,
    val users: Map<String, CallUser>,
    val callInfo: CallInfo,
    val callDetails: CallDetails,
) : CoordinatorEvent()

/**
 * Sent when a call gets updated.
 */
public data class CallUpdatedEvent(
    override val callCid: String,
    val capabilitiesByRole: Map<String, List<String>>,
    val info: CallInfo,
    val ownCapabilities: List<OwnCapability>
) : CoordinatorEvent()

/**
 * Sent when a calls gets ended.
 */
public data class CallEndedEvent(
    override val callCid: String,
    val endedByUser: User?
) : CoordinatorEvent()

/**
 * Sent when call members get updated.
 */
public data class CallMembersUpdatedEvent(
    val users: Map<String, CallUser>,
    val info: CallInfo,
    val details: CallDetails
) : CoordinatorEvent()

/**
 * Sent when call members get updated.
 */
public data class CallMembersDeletedEvent(
    val users: Map<String, CallUser>,
    val info: CallInfo,
    val details: CallDetails
) : CoordinatorEvent()

public data class CallAcceptedEvent(
    override val callCid: String,
    val sentByUserId: String,
) : CoordinatorEvent()

public data class CallRejectedEvent(
    override val callCid: String,
    val user: User,
    val updatedAt: Date
) : CoordinatorEvent()

public data class CallCancelledEvent(
    override val callCid: String,
    val sentByUserId: String,
) : CoordinatorEvent()

public data class CustomEvent(
    val cid: StreamCallCid?,
    val sentByUser: User?,
    val custom: Map<String, Any>?,
) : CoordinatorEvent()

public data class BlockedUserEvent(
    val cid: StreamCallCid?,
    val type: String,
    val userId: String
) : CoordinatorEvent()

public data class UnblockedUserEvent(
    val cid: StreamCallCid?,
    val type: String,
    val userId: String
) : CoordinatorEvent()

public data class RecordingStartedEvent(
    // TODO: Tommaso decide whats what plz
    override val callCid: String,
    val cid: StreamCallCid?,
    val type: String
) : CoordinatorEvent()

public data class RecordingStoppedEvent(
    override val callCid: String,
    val cid: StreamCallCid?,
    val type: String
) : CoordinatorEvent()

public data class PermissionRequestEvent(
    val cid: StreamCallCid?,
    val type: String,
    val permissions: List<String>,
    val user: User
) : CoordinatorEvent()

public data class UpdatedCallPermissionsEvent(
    val cid: StreamCallCid?,
    val type: String,
    val ownCapabilities: List<OwnCapability>,
    val user: User
) : CoordinatorEvent()

public object UnknownEvent : CoordinatorEvent()
