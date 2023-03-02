/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.tutorial_starter

import android.app.Application
import android.content.Context
import io.getstream.log.StreamLog
import io.getstream.log.android.AndroidStreamLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.ApiKey
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.user.UserPreferencesManager

class VideoApp : Application() {

    private var video: StreamVideo? = null
    val streamVideo: StreamVideo
        get() = requireNotNull(video)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StreamLog.setValidator { _, _ -> true }
            StreamLog.install(AndroidStreamLogger())
        }
        StreamLog.i(TAG) { "[onCreate] no args" }
        UserPreferencesManager.initialize(this)
    }

    /**
     * Sets up and returns the [streamVideo] required to connect to the API.
     */
    fun initializeStreamVideo(
        user: User,
        apiKey: ApiKey,
        loggingLevel: LoggingLevel
    ): StreamVideo {
        TODO("Implement Stream Video")
    }

    fun logOut() {
        // TODO - log the user out
    }

    companion object {
        private const val TAG = "Call:App"
        const val API_KEY = BuildConfig.SAMPLE_STREAM_VIDEO_API_KEY
    }
}

internal val Context.videoApp get() = applicationContext as VideoApp
