package io.getstream.video.android.core.ringingstatetransition

import io.getstream.android.video.generated.models.CallRingEvent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState

internal class CallRingRingingReducer(call: Call) :
    RingingStateReducer1<CallRingEvent, RingingState.Incoming> {

    override fun reduce(
        originalState: RingingState,
        event: CallRingEvent
    ): ReducerOutput1<RingingState, CallRingEvent, RingingState.Incoming> {
        return ReducerOutput1.Change(RingingState.Incoming(false))
    }
}