package io.getstream.video.android.core.ringingstatetransition

import io.getstream.android.video.generated.models.CallSessionStartedEvent
import io.getstream.video.android.core.RingingState

//TODO Rahul Later
internal class CallSessionStartedRingingReducer:RingingStateReducer1<CallSessionStartedEvent, Unit> {
    override fun reduce(
        originalState: RingingState,
        event: CallSessionStartedEvent
    ): ReducerOutput1<RingingState, CallSessionStartedEvent, Unit> {
        return ReducerOutput1.NoChange(originalState)
    }
}