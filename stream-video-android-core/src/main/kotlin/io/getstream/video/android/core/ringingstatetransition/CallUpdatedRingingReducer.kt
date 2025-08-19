package io.getstream.video.android.core.ringingstatetransition

import io.getstream.android.video.generated.models.CallUpdatedEvent
import io.getstream.video.android.core.RingingState

//TODO Rahul Later
internal class CallUpdatedRingingReducer: RingingStateReducer1<CallUpdatedEvent, Unit> {
    override fun reduce(
        originalState: RingingState,
        event: CallUpdatedEvent
    ): ReducerOutput1<RingingState, CallUpdatedEvent, Unit> {
        return ReducerOutput1.NoChange(originalState)
    }
}