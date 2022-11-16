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

package io.getstream.video.android.model

import java.io.Serializable
import java.util.Date
import stream.video.coordinator.call_v1.Call as CoordinatorCall
import stream.video.coordinator.call_v1.CallDetails as CoordinatorCallDetails
import stream.video.coordinator.member_v1.Member as CoordinatorMember
import stream.video.coordinator.user_v1.User as CoordinatorUser
import stream.video.sfu.models.Participant as SfuParticipant

public data class CallUser(
    val id: String,
    val name: String,
    val role: String,
    val state: CallUserState,
    val imageUrl: String,
    val createdAt: Date?,
    val updatedAt: Date?,
    val teams: List<String>
) : Serializable

public sealed class CallUserState {
    public data class Specified(
        val trackIdPrefix: String,
        val online: Boolean,
        val audio: Boolean,
        val video: Boolean
    ) : CallUserState()
    public object Undefined : CallUserState() { override fun toString(): String = "Undefined" }
}

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
    val broadcastingEnabled: Boolean,
    val recordingEnabled: Boolean,
) : Serializable

public fun Map<String, CoordinatorUser>.toCallUsers(): Map<String, CallUser> =
    map { (userId, protoUser) ->
        userId to protoUser.toCallUser()
    }.toMap()

public fun CoordinatorUser.toCallUser(): CallUser = CallUser(
    id = id,
    name = name,
    role = role,
    imageUrl = image_url,
    state = CallUserState.Undefined,
    createdAt = created_at?.let { Date(it.toEpochMilli()) },
    updatedAt = updated_at?.let { Date(it.toEpochMilli()) },
    teams = teams
)

public fun Map<String, CoordinatorMember>.toCallMembers(): Map<String, CallMember> = map { (userId, protoMember) ->
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
        broadcastingEnabled = options?.broadcasting?.enabled ?: false,
        recordingEnabled = options?.recording?.enabled ?: false
    )
}

public fun CoordinatorCall?.toCallInfo(): CallInfo {
    this ?: error("CallInfo is not provided")
    return CallInfo(
        cid = call_cid,
        id = id,
        type = type,
        createdByUserId = created_by_user_id,
        broadcastingEnabled = options?.broadcasting?.enabled ?: false,
        recordingEnabled = options?.recording?.enabled ?: false,
        createdAt = created_at?.let { Date(it.toEpochMilli()) },
        updatedAt = updated_at?.let { Date(it.toEpochMilli()) },
    )
}

public fun SfuParticipant.toCallUser(): CallUser = CallUser(
    id = user?.id ?: error("SfuParticipant has no userId"),
    name = user.name,
    role = user.role,
    imageUrl = user.image_url,
    state = CallUserState.Specified(
        trackIdPrefix = track_lookup_prefix,
        online = online,
        audio = audio,
        video = video,
    ),
    createdAt = /*TODO user.created_at*/ null,
    updatedAt = /*TODO user.updated_at*/ null,
    teams = user.teams
)

public fun List<SfuParticipant>.toCallUserMap(): Map<String, CallUser> = associate { participant ->
    participant.toCallUser().let {
        it.id to it
    }
}

public infix fun Map<String, CallUser>.merge(that: Map<String, CallUser>): Map<String, CallUser> {
    val new = this - that.keys
    val merged = this.map { (userId, user) ->
        userId to user.merge(that[userId])
    }.toMap()
    return merged + new
}

public infix fun Map<String, CallUser>.merge(that: CallUser): Map<String, CallUser> = when (contains(that.id)) {
    true -> this.map { (userId, user) ->
        userId to user.merge(that)
    }.toMap()
    else -> this.toMutableMap().also {
        it[that.id] = that
    }
}

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

public infix fun CallUserState.merge(that: CallUserState): CallUserState = when {
    this is CallUserState.Specified && that is CallUserState.Specified -> that.copy(
        trackIdPrefix = that.trackIdPrefix.ifEmpty { this.trackIdPrefix },
    )
    this is CallUserState.Undefined -> that
    else -> this
}
