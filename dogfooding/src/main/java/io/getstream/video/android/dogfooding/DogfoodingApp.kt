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

package io.getstream.video.android.dogfooding

import android.app.Application
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import io.getstream.android.push.firebase.FirebasePushDeviceGenerator
import io.getstream.log.StreamLog
import io.getstream.log.android.AndroidStreamLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.input.CallActivityInput
import io.getstream.video.android.core.input.CallServiceInput
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.ApiKey
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.user.UserPreferencesManager

class DogfoodingApp : Application() {

    private var video: StreamVideo? = null

    val streamVideo: StreamVideo
        get() = requireNotNull(video)

    fun isInitialized(): Boolean {
        return video != null
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StreamLog.setValidator { _, _ -> true }
            StreamLog.install(AndroidStreamLogger())
        }
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
        return StreamVideoBuilder(
            context = this,
            user = user,
            apiKey = apiKey,
            loggingLevel = loggingLevel,
            pushDeviceGenerators = listOf(FirebasePushDeviceGenerator()),
            androidInputs = setOf(
                CallServiceInput.from(CallService::class),
                CallActivityInput.from(CallActivity::class),
            )
        ).build().also {
            video = it
        }
    }

    fun logOut() {
        FirebaseAuth.getInstance().signOut()
        streamVideo.logOut()
        video = null
    }

    fun initializeFromCredentials(): Boolean {
        val credentials = UserPreferencesManager.initialize(this)
        val user = credentials.getUserCredentials()
        val apiKey = credentials.getApiKey()

        if (user == null || apiKey.isBlank()) {
            return false
        }

        dogfoodingApp.initializeStreamVideo(
            apiKey = apiKey,
            user = user,
            loggingLevel = LoggingLevel.NONE
        )
        return true
    }
}

internal const val API_KEY = BuildConfig.DOGFOODING_API_KEY

val Context.dogfoodingApp get() = applicationContext as DogfoodingApp
