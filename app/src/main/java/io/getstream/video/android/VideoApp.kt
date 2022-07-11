package io.getstream.video.android

import android.app.Application
import io.getstream.video.android.client.CallCoordinatorClient
import io.getstream.video.android.init.CallClientInitializer

class VideoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        videoClient = CallClientInitializer
            .buildClient("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidG9tbWFzbyJ9.XGkxJKi33fHr3cHyLFc6HRnbPgLuwNHuETWQ2MWzz5c")
    }

    companion object {
        lateinit var instance: VideoApp

        lateinit var videoClient: CallCoordinatorClient
            private set
    }
}