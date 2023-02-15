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

package io.getstream.video.android.core.model

import io.getstream.video.android.core.utils.toCallUser
import org.openapitools.client.models.CallResponse
import org.openapitools.client.models.MemberResponse
import java.io.Serializable
import java.util.Date
import stream.video.coordinator.call_v1.Call as CoordinatorCall
import stream.video.coordinator.call_v1.CallDetails as CoordinatorCallDetails
import stream.video.coordinator.member_v1.Member as CoordinatorMember
import stream.video.coordinator.user_v1.User as CoordinatorUser

public data class CallUser(
    val id: String,
    val name: String,
    val role: String,
    val imageUrl: String,
    val teams: List<String>,
    val state: CallUserState?,
    val createdAt: Date?,
    val updatedAt: Date?
) : Serializable

public data class CallUserState(
    val trackIdPrefix: String,
    val online: Boolean,
    val audio: Boolean,
    val video: Boolean
)

public data class CallMember(
    val callCid: String,
    val role: String,
    val userId: String,
    val createdAt: Date?,
    val updatedAt: Date?
) : Serializable

public data class CallInfo(
    val cid: String,
    val id: String,
    val type: String,
    val createdByUserId: String,
    val broadcastingEnabled: Boolean,
    val recordingEnabled: Boolean,
    val createdAt: Date?,
    val updatedAt: Date?
) : Serializable

public data class CallDetails(
    val memberUserIds: List<String>,
    val members: Map<String, CallMember>,
) : Serializable

public fun Map<String, CoordinatorUser>.toCallUsers(): Map<String, CallUser> =
    map { (userId, protoUser) ->
        userId to protoUser.toCallUser()
    }.toMap()

internal fun List<MemberResponse>.toCallUsers(): Map<String, CallUser> =
    associate { it.userId to it.toCallUser() }

public fun CoordinatorUser.toCallUser(): CallUser = CallUser(
    id = id,
    name = name,
    role = role,
    imageUrl = image_url,
    state = null,
    createdAt = created_at?.let { Date(it.toEpochMilli()) },
    updatedAt = updated_at?.let { Date(it.toEpochMilli()) },
    teams = teams
)

public fun Map<String, CoordinatorMember>.toCallMembers(): Map<String, CallMember> =
    map { (userId, protoMember) ->
        userId to protoMember.toCallMember()
    }.toMap()

public fun CoordinatorMember.toCallMember(): CallMember = CallMember(
    callCid = call_cid,
    role = role,
    userId = user_id,
    createdAt = created_at?.let { Date(it.toEpochMilli()) },
    updatedAt = updated_at?.let { Date(it.toEpochMilli()) },
)

public fun CoordinatorCallDetails?.toCallDetails(): CallDetails {
    this ?: error("CallDetails is not provided")
    return CallDetails(
        memberUserIds = member_user_ids,
        members = members.toCallMembers(),
    )
}

public fun CoordinatorCall?.toCallInfo(): CallInfo {
    this ?: error("CallInfo is not provided")
    return CallInfo(
        cid = call_cid,
        id = id,
        type = type,
        createdByUserId = created_by_user_id,
        broadcastingEnabled = settings_overrides?.broadcasting?.enabled ?: false,
        recordingEnabled = settings_overrides?.recording?.enabled ?: false,
        createdAt = created_at?.let { Date(it.toEpochMilli()) },
        updatedAt = updated_at?.let { Date(it.toEpochMilli()) },
    )
}

internal fun CallResponse.toCallInfo(): CallInfo { // TODO - expose more properties based on what the events have
    return CallInfo(
        cid = cid,
        id = id,
        type = type,
        createdByUserId = createdBy.id,
        broadcastingEnabled = settings.broadcasting.enabled,
        recordingEnabled = settings.recording.audioOnly, // TODO - how do we know if it's enabled or not
        createdAt = Date(createdAt.toEpochSecond() * 1000L),
        updatedAt = Date(updatedAt.toEpochSecond() * 1000L),
    )
}

/**
 * Merges [CallUser] maps to absorb as many non-null and non-empty data from both collections.
 */
public infix fun Map<String, CallUser>.merge(that: Map<String, CallUser>): Map<String, CallUser> {
    val new = this - that.keys
    val merged = this.map { (userId, user) ->
        userId to user.merge(that[userId])
    }.toMap()
    return merged + new
}

/**
 * Merges [that] [CallUser] into [this] map.
 */
public infix fun Map<String, CallUser>.merge(that: CallUser): Map<String, CallUser> =
    when (contains(that.id)) {
        true -> this.map { (userId, user) ->
            userId to user.merge(that)
        }.toMap()
        else -> this.toMutableMap().also {
            it[that.id] = that
        }
    }

/**
 * Merges [that] into [this] CallUser to absorb as much data as possible from both instances.
 */
public infix fun CallUser.merge(that: CallUser?): CallUser = when (that) {
    null -> this
    else -> copy(
        id = that.id.ifEmpty { this.id },
        name = that.name.ifEmpty { this.name },
        role = that.role.ifEmpty { this.role },
        imageUrl = that.imageUrl.ifEmpty { this.imageUrl },
        state = that.state merge this.state,
        createdAt = that.createdAt ?: this.createdAt,
        updatedAt = that.updatedAt ?: this.updatedAt,
        teams = (that.teams + this.teams).distinct()
    )
}

/**
 * Merges [that] into [this] CallUserState to absorb as much data as possible from both instances.
 */
public infix fun CallUserState?.merge(that: CallUserState?): CallUserState? = when {
    this != null && that != null -> that.copy(
        trackIdPrefix = that.trackIdPrefix.ifEmpty { this.trackIdPrefix },
    )
    this == null -> that
    else -> this
}
