package io.getstream.video.android

import android.app.Application
import io.getstream.video.android.initializer.VideoAppInitializer

class VideoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        VideoAppInitializer.init(instance)
    }

    companion object {
        lateinit var instance: VideoApp
    }
}