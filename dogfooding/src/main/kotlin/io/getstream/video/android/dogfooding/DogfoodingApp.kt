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
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.HiltAndroidApp
import io.getstream.android.push.firebase.FirebasePushDeviceGenerator
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.client.models.UploadAttachmentsNetworkType
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.notifications.NotificationConfig
import io.getstream.video.android.dogfooding.token.StreamVideoNetwork
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import io.getstream.video.android.tooling.handler.StreamGlobalExceptionHandler

@HiltAndroidApp
class DogfoodingApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (!BuildConfig.DEBUG) {
            StreamGlobalExceptionHandler.install(
                application = this,
                packageName = MainActivity::class.java.name,
                exceptionHandler = { stackTrace -> Firebase.crashlytics.log(stackTrace) }
            )
        }
    }

    /** Sets up and returns the [StreamVideo] required to connect to the API. */
    fun initializeStreamVideo(
        user: User,
        token: String,
        apiKey: ApiKey,
        loggingLevel: LoggingLevel
    ): StreamVideo {
        return StreamVideoBuilder(
            context = this,
            user = user,
            token = token,
            apiKey = apiKey,
            loggingLevel = loggingLevel,
            ensureSingleInstance = false,
            notificationConfig = NotificationConfig(
                pushDeviceGenerators = listOf(FirebasePushDeviceGenerator(providerName = "firebase"))
            ),
            tokenProvider = {
                val email = user.custom["email"]
                val response = StreamVideoNetwork.tokenService.fetchToken(
                    userId = email,
                    apiKey = API_KEY
                )
                response.token
            }
        ).build()
    }

    fun initializeStreamChat(
        user: User,
    ) {
        val offlinePlugin = StreamOfflinePluginFactory(this) // 1
        val statePluginFactory = StreamStatePluginFactory( // 2
            config = StatePluginConfig(
                backgroundSyncEnabled = true,
                userPresence = true,
            ),
            appContext = this
        )

        val logLevel = if (BuildConfig.DEBUG) ChatLogLevel.ALL else ChatLogLevel.NOTHING
        val chatClient = ChatClient.Builder("kqucevfhngu4", this)
            .withPlugins(offlinePlugin, statePluginFactory)
            .logLevel(logLevel)
            .uploadAttachmentsNetworkType(UploadAttachmentsNetworkType.NOT_ROAMING)
            .build()

        val token = chatClient.devToken(user.id)
        val chatUser = io.getstream.chat.android.client.models.User(
            id = user.id,
            name = user.name,
            image = user.image
        )

        chatClient.connectUser(
            user = chatUser,
            token = token
        ).enqueue()
    }
}

const val API_KEY = BuildConfig.DOGFOODING_API_KEY

val Context.dogfoodingApp get() = applicationContext as DogfoodingApp
