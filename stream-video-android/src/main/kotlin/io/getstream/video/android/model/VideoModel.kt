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
import stream.video.coordinator.call_v1.Call as ProtoCall
import stream.video.coordinator.call_v1.CallDetails as ProtoCallDetails
import stream.video.coordinator.member_v1.Member as ProtoMember
import stream.video.coordinator.user_v1.User as ProtoUser

public data class CallUser(
    val id: String,
    val name: String,
    val role: String,
    val imageUrl: String,
    val createdAt: Date?,
    val updatedAt: Date?
) : Serializable

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

public fun Map<String, ProtoUser>.toCallUsers(): Map<String, CallUser> =
    map { (userId, protoUser) ->
        userId to protoUser.toCallUser()
    }.toMap()

public fun ProtoUser.toCallUser(): CallUser = CallUser(
    id = id,
    name = name,
    role = role,
    imageUrl = image_url,
    createdAt = created_at?.let { Date(it.toEpochMilli()) },
    updatedAt = updated_at?.let { Date(it.toEpochMilli()) },
)

public fun Map<String, ProtoMember>.toCallMembers(): Map<String, CallMember> = map { (userId, protoMember) ->
    userId to protoMember.toCallMember()
}.toMap()

public fun ProtoMember.toCallMember(): CallMember = CallMember(
    callCid = call_cid,
    role = role,
    userId = user_id,
    createdAt = created_at?.let { Date(it.toEpochMilli()) },
    updatedAt = updated_at?.let { Date(it.toEpochMilli()) },
)

public fun ProtoCallDetails?.toCallDetails(): CallDetails {
    this ?: error("CallDetails is not provided")
    return CallDetails(
        memberUserIds = member_user_ids,
        members = members.toCallMembers(),
        broadcastingEnabled = options?.broadcasting?.enabled ?: false,
        recordingEnabled = options?.recording?.enabled ?: false
    )
}

public fun ProtoCall?.toCallInfo(): CallInfo {
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
