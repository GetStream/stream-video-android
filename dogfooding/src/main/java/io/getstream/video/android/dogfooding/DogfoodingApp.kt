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
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.HiltAndroidApp
import io.getstream.android.push.firebase.FirebasePushDeviceGenerator
import io.getstream.log.Priority
import io.getstream.log.android.AndroidStreamLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.ApiKey
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.user.UserPreferencesManager
import io.getstream.video.android.dogfooding.token.StreamVideoNetwork
import io.getstream.video.android.tooling.handler.StreamGlobalExceptionHandler

@HiltAndroidApp
class DogfoodingApp : Application() {

    private var video: StreamVideo? = null

    val streamVideo: StreamVideo
        get() = requireNotNull(video)

    fun isInitialized(): Boolean {
        return video != null
    }

    override fun onCreate() {
        super.onCreate()
        AndroidStreamLogger.installOnDebuggableApp(this, minPriority = Priority.DEBUG)
        StreamGlobalExceptionHandler.install(
            application = this,
            packageName = MainActivity::class.java.name,
            exceptionHandler = { stackTrace -> Firebase.crashlytics.log(stackTrace) }
        )
        UserPreferencesManager.initialize(this)
    }

    /**
     * Sets up and returns the [streamVideo] required to connect to the API.
     */
    fun initializeStreamVideo(
        user: User,
        token: String,
        apiKey: ApiKey,
        loggingLevel: LoggingLevel
    ): StreamVideo {
        return StreamVideoBuilder(
            context = this,
            user = user,
            userToken = token,
            apiKey = apiKey,
            loggingLevel = loggingLevel,
            pushDeviceGenerators = listOf(FirebasePushDeviceGenerator()),
            tokenProvider = {
                val email = user.custom["email"]
                val response = StreamVideoNetwork.tokenService.fetchToken(
                    userId = email,
                    apiKey = API_KEY
                )
                response.token
            }
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
        val token = credentials.getUserToken()

        if (user == null || apiKey.isBlank()) {
            return false
        }

        dogfoodingApp.initializeStreamVideo(
            apiKey = apiKey, user = user, loggingLevel = LoggingLevel.NONE, token = token
        )
        return true
    }
}

const val API_KEY = BuildConfig.DOGFOODING_API_KEY

val Context.dogfoodingApp get() = applicationContext as DogfoodingApp
