package io.getstream.video.android.core.notifications.internal.service.repository

import io.getstream.video.android.model.StreamCallId

class CallServiceRepository {
    internal  var callId: StreamCallId? = null
    internal var callIds: HashSet<StreamCallId> = HashSet()

    fun addCallId(callId: StreamCallId) {
        this.callId = callId
        callIds.add(callId)
    }

    fun removeCallId(callId: StreamCallId) {
        this.callId = callId
        callIds.remove(callId)
    }

    fun getCallIds() = callIds

    fun noActiveCall() = callIds.isEmpty()

}