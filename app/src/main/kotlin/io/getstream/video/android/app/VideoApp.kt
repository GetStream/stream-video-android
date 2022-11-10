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

package io.getstream.video.android.app

import android.app.Activity
import android.app.Application
import android.content.Context
import io.getstream.logging.StreamLog
import io.getstream.logging.android.AndroidStreamLogger
import io.getstream.video.android.StreamVideo
import io.getstream.video.android.StreamVideoBuilder
import io.getstream.video.android.app.lifecycle.StreamActivityLifecycleCallbacks
import io.getstream.video.android.app.ui.call.CallActivity
import io.getstream.video.android.app.ui.call.CallService
import io.getstream.video.android.app.user.UserPreferences
import io.getstream.video.android.app.user.UserPreferencesImpl
import io.getstream.video.android.input.CallActivityInput
import io.getstream.video.android.input.CallServiceInput
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.token.CredentialsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class VideoApp : Application() {

    private val appScope = CoroutineScope(Dispatchers.Main)

    val userPreferences: UserPreferences by lazy {
        UserPreferencesImpl(
            getSharedPreferences(KEY_PREFERENCES, MODE_PRIVATE)
        )
    }

    lateinit var credentialsProvider: CredentialsProvider
        private set

    lateinit var streamVideo: StreamVideo
        private set

    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StreamLog.setValidator { _, _ -> true }
            StreamLog.setLogger(AndroidStreamLogger())
        }
        StreamLog.i(TAG) { "[onCreate] no args" }
        registerActivityLifecycleCallbacks(
            StreamActivityLifecycleCallbacks(
                onActivityCreated = { currentActivity = it },
                onActivityStarted = { currentActivity = it },
                onLastActivityStopped = { currentActivity = null },
            )
        )
    }

    /**
     * Sets up and returns the [streamVideo] required to connect to the API.
     */
    fun initializeStreamCalls(
        credentialsProvider: CredentialsProvider,
        loggingLevel: LoggingLevel
    ): StreamVideo {
        StreamLog.d(TAG) { "[initializeStreamCalls] loggingLevel: $loggingLevel" }
        this.credentialsProvider = credentialsProvider

        return StreamVideoBuilder(
            context = this,
            credentialsProvider = credentialsProvider,
            androidInputs = setOf(
                CallServiceInput.from(CallService::class),
                CallActivityInput.from(CallActivity::class),
            ),
            loggingLevel = loggingLevel
        ).build().also {
            streamVideo = it
            StreamLog.v(TAG) { "[initializeStreamCalls] completed" }
        }
    }

    private companion object {
        /**
         * Preferences file name.
         */
        private const val KEY_PREFERENCES = "video-prefs"
        private const val TAG = "Call:App"
    }
}

internal val Context.videoApp get() = applicationContext as VideoApp
