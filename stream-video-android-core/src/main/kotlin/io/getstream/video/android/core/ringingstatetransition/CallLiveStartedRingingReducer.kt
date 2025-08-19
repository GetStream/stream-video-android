package io.getstream.video.android.core.ringingstatetransition

import io.getstream.android.video.generated.models.CallLiveStartedEvent
import io.getstream.video.android.core.RingingState

//TODO Rahul Later
internal class CallLiveStartedRingingReducer:RingingStateReducer1<CallLiveStartedEvent, Unit> {
    override fun reduce(
        originalState: RingingState,
        event: CallLiveStartedEvent
    ): ReducerOutput1<RingingState, CallLiveStartedEvent, Unit> {
        return ReducerOutput1.NoChange(originalState)
    }
}