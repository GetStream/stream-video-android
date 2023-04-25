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
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import io.getstream.android.push.firebase.FirebasePushDeviceGenerator
import io.getstream.log.Priority
import io.getstream.log.android.AndroidStreamLogger
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.ApiKey
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.user.UserPreferencesManager
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import okhttp3.Request

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
//        StreamGlobalExceptionHandler.install(
//            application = this,
//            packageName = LoginActivity::class.java.name
//        )
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
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://stream-calls-dogfood.vercel.app/api/auth/create-token?user_id=$email&api_key=$API_KEY")
                    .build()
                val response = async { client.newCall(request).execute() }.await()
                Toast.makeText(applicationContext, "${response.body?.string()}", Toast.LENGTH_SHORT)
                    .show()
                response.body?.string() ?: ""
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

internal const val API_KEY = BuildConfig.DOGFOODING_API_KEY

val Context.dogfoodingApp get() = applicationContext as DogfoodingApp
