package io.getstream.video.android.core.ringingstatetransition

import io.getstream.android.video.generated.models.CallAcceptedEvent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState

internal class CallAcceptedRingingReducer (private val call:Call):
    RingingStateReducer2<CallAcceptedEvent, RingingState.Active, RingingState.ActiveOnOtherDevice> {

        override fun reduce(
        originalState: RingingState,
        event: CallAcceptedEvent
    ): ReducerOutput2<RingingState, CallAcceptedEvent, RingingState.Active, RingingState.ActiveOnOtherDevice> {
        if (call.state.acceptedOnThisDevice) {
            return ReducerOutput2.FirstOption(RingingState.Active)
        } else {
            return ReducerOutput2.SecondOption(RingingState.ActiveOnOtherDevice)
        }
    }
}