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

package io.getstream.video.android.demo

import android.app.Application
import android.content.Context
import io.getstream.log.Priority
import io.getstream.log.StreamLog
import io.getstream.log.android.AndroidStreamLogger
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.model.User

class DemoVideoApp : Application() {

    lateinit var streamVideo: StreamVideo
        private set

    override fun onCreate() {
        super.onCreate()
        AndroidStreamLogger.installOnDebuggableApp(this, minPriority = Priority.DEBUG)

        val token =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoidGhpZXJyeSJ9._4aZL6BR0VGKfZsKYdscsBm8yKVgG-2LatYeHRJUq0g"

        streamVideo = StreamVideoBuilder(
            context = applicationContext,
            apiKey = "hd8szvscpxvd",
            geo = GEO.GlobalEdgeNetwork,
            User(
                id = "thierry", role = "admin", name = "Thierry", image = "hello",
                teams = emptyList(), custom = mapOf()
            ),
            token,
            loggingLevel = LoggingLevel.BODY
        ).build()

        StreamLog.i(TAG) { "[onCreate] no args" }
    }

    fun logOut() {
        streamVideo.logOut()
    }

    companion object {
        private const val TAG = "Call:App"
        const val API_KEY = BuildConfig.SAMPLE_STREAM_VIDEO_API_KEY
    }
}

internal val Context.demoVideoApp get() = applicationContext as DemoVideoApp
