package io.getstream.video.android.core.ringingstatetransition

import io.getstream.android.video.generated.models.CallRejectedEvent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState

//TODO Rahul Later
internal class CallRejectedRingingReducer(call: Call):RingingStateReducer1<CallRejectedEvent, RingingState.RejectedByAll> {
    override fun reduce(
        originalState: RingingState,
        event: CallRejectedEvent
    ): ReducerOutput1<RingingState, CallRejectedEvent, RingingState.RejectedByAll> {
        return ReducerOutput1.NoChange(originalState)
    }
}