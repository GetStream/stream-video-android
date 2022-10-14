package io.getstream.video.android.router

import io.getstream.video.android.model.CallInput
import io.getstream.video.android.model.IncomingCallData
import io.getstream.video.android.model.OutgoingCallData

public interface StreamRouter {

    public fun navigateToCall(callInput: CallInput)

    public fun onIncomingCall(callData: IncomingCallData)

    public fun onOutgoingCall(callData: OutgoingCallData)

    public fun onCallAccepted()

    public fun onCallRejected()

    public fun finish()

    public fun onCallFailed(reason: String?)

    public fun onUserLoggedOut()
}