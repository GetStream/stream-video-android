package io.getstream.video.android.core.notifications.internal.service

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.model.StreamCallId

class ServiceIncomingCallHandler {

    internal val logger by taggedLogger("ServiceIncomingCallHandler")

    suspend fun updateRingingCall(
        streamVideo: StreamVideo,
        callId: StreamCallId,
        ringingState: RingingState,
    ) {
        val call = streamVideo.call(callId.type, callId.id)
        streamVideo.state.addRingingCall(call, ringingState)
    }
}