package io.getstream.video.android.model.state

import io.getstream.video.android.model.CallDetails
import io.getstream.video.android.model.CallInfo
import io.getstream.video.android.model.CallUser

public sealed interface StreamCallState {

    public object Idle : StreamCallState

    public data class Incoming(
        val callId: String,
        val users: Map<String, CallUser>,
        val info: CallInfo?,
        val details: CallDetails?
    ) : StreamCallState


}

