package io.getstream.video.android.core.ringingstatetransition

import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.events.JoinCallResponseEvent

//TODO Rahul Later
internal class JoinCallResponseRingingReducer:RingingStateReducer1<JoinCallResponseEvent, Unit> {
    override fun reduce(
        originalState: RingingState,
        event: JoinCallResponseEvent
    ): ReducerOutput1<RingingState, JoinCallResponseEvent, Unit> {
        return ReducerOutput1.NoChange(originalState)
    }

}