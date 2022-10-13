package io.getstream.video.android.engine

import io.getstream.video.android.model.state.StreamCallState
import kotlinx.coroutines.flow.StateFlow

internal interface StreamCallEngine {

    val callState: StateFlow<StreamCallState>

}