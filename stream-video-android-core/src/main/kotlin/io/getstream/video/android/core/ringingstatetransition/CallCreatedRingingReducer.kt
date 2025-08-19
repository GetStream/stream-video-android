package io.getstream.video.android.core.ringingstatetransition

import io.getstream.android.video.generated.models.CallCreatedEvent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState

internal class CallCreatedRingingReducer(call: Call) :
    RingingStateReducer1<CallCreatedEvent, RingingState.Incoming> {

    override fun reduce(
        originalState: RingingState,
        event: CallCreatedEvent
    ): ReducerOutput1<RingingState, CallCreatedEvent, RingingState.Incoming> {
        return ReducerOutput1.NoChange(originalState)
    }
}