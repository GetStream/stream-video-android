package io.getstream.video.android.core.ringingstatetransition

import io.getstream.android.video.generated.models.CallSessionParticipantJoinedEvent
import io.getstream.video.android.core.RingingState

internal class CallSessionParticipantJoinedRingingReducer: RingingStateReducer1<CallSessionParticipantJoinedEvent, Unit> {
    override fun reduce(
        originalState: RingingState,
        event: CallSessionParticipantJoinedEvent
    ): ReducerOutput1<RingingState, CallSessionParticipantJoinedEvent, Unit> {
        return ReducerOutput1.NoChange(originalState)
    }
}