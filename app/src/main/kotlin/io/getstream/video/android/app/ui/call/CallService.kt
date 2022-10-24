package io.getstream.video.android.app.ui.call

import android.content.Context
import io.getstream.video.android.StreamCalls
import io.getstream.video.android.app.videoApp
import io.getstream.video.android.service.StreamCallService

class CallService : StreamCallService() {

    override fun getStreamCalls(context: Context): StreamCalls = videoApp.streamCalls

    companion object {
        fun start(context: Context) = start<CallService>(context)
        fun stop(context: Context) = stop<CallService>(context)
    }

}