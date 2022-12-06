package io.getstream.video.android.dogfooding

import android.content.Context
import io.getstream.video.android.StreamVideo
import io.getstream.video.android.service.AbstractStreamCallService

class CallService : AbstractStreamCallService() {

    override fun getStreamVideo(context: Context): StreamVideo {
        return dogfoodingApp.streamVideo
    }
}