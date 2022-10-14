package io.getstream.video.android.app.ui.incoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.getstream.video.android.StreamCalls
import io.getstream.video.android.model.IncomingCallData
import io.getstream.video.android.router.StreamRouter

class IncomingCallViewModelFactory(
    private val streamCalls: StreamCalls,
    private val streamRouter: StreamRouter,
    private val input: IncomingCallData
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return IncomingCallViewModel(
            streamCalls = streamCalls,
            streamRouter = streamRouter,
            callData = input
        ) as T
    }
}