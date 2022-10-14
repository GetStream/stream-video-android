package io.getstream.video.android.model

public data class OutgoingCallData(
    val callType: CallType,
    val callInfo: CallInfo,
    val participants: List<CallUser>
) : java.io.Serializable

public fun OutgoingCallData.toMetadata(): CallMetadata =
    CallMetadata(
        id = callInfo.callId,
        cid = callInfo.cid,
        type = callInfo.type,
        users = participants.associateBy { it.id },
        createdAt = callInfo.createdAt?.time ?: 0,
        updatedAt = callInfo.updatedAt?.time ?: 0,
        createdBy = callInfo.createdByUserId,
        broadcastingEnabled = false,
        recordingEnabled = false,
        extraData = emptyMap()
    )