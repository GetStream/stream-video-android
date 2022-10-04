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

package io.getstream.video.android.events.model

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
)

public data class CallMember(
    val callId: String,
    val role: String,
    val userId: String,
    val createdAt: Date?,
    val updatedAt: Date?
)

public data class CallInfo(
    val callId: String,
    val type: String,
    val createdByUserId: String,
    val createdAt: Date?,
    val updatedAt: Date?
)

public data class CallDetails(
    val memberUserIds: List<String>,
    val members: Map<String, CallMember>
)

public fun Map<String, ProtoUser>.toCallUsers(): Map<String, CallUser> = map { (userId, protoUser) ->
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

public fun ProtoMember.toCallMember(): CallMember = CallMember(
    callId = call_cid,
    role = role,
    userId = user_id,
    createdAt = created_at?.let { Date(it.toEpochMilli()) },
    updatedAt = updated_at?.let { Date(it.toEpochMilli()) },
)

public fun ProtoCallDetails.toCallDetails(): CallDetails = CallDetails(
    memberUserIds = member_user_ids,
    members = members.map { (userId, protoMember) ->
        userId to protoMember.toCallMember()
    }.toMap()
)

public fun ProtoCall.toCallInfo(): CallInfo = CallInfo(
    callId = call_cid,
    type = type,
    createdByUserId = created_by_user_id,
    createdAt = created_at?.let { Date(it.toEpochMilli()) },
    updatedAt = updated_at?.let { Date(it.toEpochMilli()) },
)
