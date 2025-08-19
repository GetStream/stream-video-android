package io.getstream.video.android.core.ringingstatetransition

import io.getstream.android.video.generated.models.CallEndedEvent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState

//TODO Rahul Later
internal class CallEndedRingingReducer(call: Call): RingingStateReducer1<CallEndedEvent, Unit> {
    override fun reduce(
        originalState: RingingState,
        event: CallEndedEvent
    ): ReducerOutput1<RingingState, CallEndedEvent, Unit> {
        return ReducerOutput1.NoChange(originalState)
    }
}