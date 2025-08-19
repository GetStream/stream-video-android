package io.getstream.video.android.core.ringingstatetransition

import io.getstream.android.video.generated.models.CallSessionEndedEvent
import io.getstream.video.android.core.RingingState

//TODO Rahul Later
internal class CallSessionEndedRingingReducer: RingingStateReducer1<CallSessionEndedEvent, Unit> {
    override fun reduce(
        originalState: RingingState,
        event: CallSessionEndedEvent
    ): ReducerOutput1<RingingState, CallSessionEndedEvent, Unit> {
        return ReducerOutput1.NoChange(originalState)
    }
}