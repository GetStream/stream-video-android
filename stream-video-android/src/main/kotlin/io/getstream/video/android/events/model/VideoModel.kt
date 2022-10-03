package io.getstream.video.android.events.model

import stream.video.coordinator.member_v1.Member as ProtoMember
import stream.video.coordinator.user_v1.User as ProtoUser
import stream.video.coordinator.call_v1.CallDetails as ProtoCallDetails
import stream.video.coordinator.call_v1.Call as ProtoCall
import java.util.Date

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



