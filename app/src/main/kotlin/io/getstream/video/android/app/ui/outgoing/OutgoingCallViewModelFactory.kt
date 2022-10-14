package io.getstream.video.android.app.ui.outgoing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.getstream.video.android.StreamCalls
import io.getstream.video.android.model.OutgoingCallData
import io.getstream.video.android.router.StreamRouter

class OutgoingCallViewModelFactory(
    private val streamCalls: StreamCalls,
    private val streamRouter: StreamRouter,
    private val input: OutgoingCallData
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return OutgoingCallViewModel(
            streamCalls = streamCalls,
            streamRouter = streamRouter,
            callData = input
        ) as T
    }
}