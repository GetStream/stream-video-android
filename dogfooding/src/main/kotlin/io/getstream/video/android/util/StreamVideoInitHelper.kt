/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import io.getstream.android.push.firebase.FirebasePushDeviceGenerator
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory
import io.getstream.log.Priority
import io.getstream.video.android.API_KEY
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.notifications.NotificationConfig
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import io.getstream.video.android.token.StreamVideoNetwork
import kotlinx.coroutines.flow.firstOrNull

@SuppressLint("StaticFieldLeak")
object StreamVideoInitHelper {

    private var isInitialising = false
    private lateinit var context: Context

    fun init(appContext: Context) {
        context = appContext.applicationContext
    }

    /**
     * A helper function that will initialise the [StreamVideo] SDK and also the [ChatClient].
     * Set [useRandomUserAsFallback] to true if you want to use a guest fallback if the user is not
     * logged in.
     */
    suspend fun loadSdk(dataStore: StreamUserDataStore, useRandomUserAsFallback: Boolean = true) {
        if (StreamVideo.isInstalled) {
            Log.w("StreamVideoInitHelper", "[initStreamVideo] StreamVideo is already initialised.")
            return
        }

        if (isInitialising) {
            Log.d("StreamVideoInitHelper", "[initStreamVideo] StreamVideo is already initialising")
            return
        }

        isInitialising = true

        // Load the signed-in user (can be null)
        val storedUser = dataStore.data.firstOrNull()

        var loggedInUser = storedUser?.user
        var userToken = storedUser?.userToken

        // Create and login a random new user if user is null and we allow a random user login
        if (loggedInUser == null && useRandomUserAsFallback) {
            val userId = UserIdHelper.generateRandomString()

            val result = StreamVideoNetwork.tokenService.fetchToken(
                userId = userId,
                apiKey = BuildConfig.API_KEY,
            )
            val user = User(id = result.userId, role = "admin")

            // Store the data (note that this datastore belongs to the client - it's not
            // used by the SDK directly in any way)
            dataStore.updateUser(user)
            dataStore.updateUserToken(result.token)

            loggedInUser = user
            userToken = result.token
        }

        if (loggedInUser != null) {
            // there is a user - so we expect a token too
            val token = checkNotNull(userToken)

            initializeStreamChat(
                context = context,
                user = loggedInUser,
                token = token,
            )

            initializeStreamVideo(
                context = context,
                user = loggedInUser,
                token = token,
                apiKey = API_KEY,
                loggingLevel = LoggingLevel(priority = Priority.VERBOSE),
                dataStore = dataStore,
            )
        }
        isInitialising = false
    }

    /** Sets up and returns the [StreamVideo] required to connect to the API. */
    private fun initializeStreamVideo(
        context: Context,
        user: User,
        token: String,
        apiKey: ApiKey,
        loggingLevel: LoggingLevel,
        dataStore: StreamUserDataStore,
    ): StreamVideo {
        return StreamVideoBuilder(
            context = context,
            user = user,
            token = token,
            apiKey = apiKey,
            loggingLevel = loggingLevel,
            ensureSingleInstance = false,
            notificationConfig = NotificationConfig(
                pushDeviceGenerators = listOf(
                    FirebasePushDeviceGenerator(providerName = "firebase"),
                ),
            ),
            tokenProvider = {
                val email = user.custom["email"]
                val response = StreamVideoNetwork.tokenService.fetchToken(
                    userId = email,
                    apiKey = API_KEY,
                )
                dataStore.updateUserToken(response.token)
                response.token
            },
        ).build()
    }

    private fun initializeStreamChat(
        context: Context,
        user: User,
        token: String,
    ) {
        val offlinePlugin = StreamOfflinePluginFactory(context) // 1
        val statePluginFactory = StreamStatePluginFactory( // 2
            config = StatePluginConfig(
                backgroundSyncEnabled = true,
                userPresence = true,
            ),
            appContext = context,
        )

        val logLevel = if (BuildConfig.DEBUG) ChatLogLevel.ALL else ChatLogLevel.NOTHING
        val chatClient = ChatClient.Builder(API_KEY, context)
            .withPlugins(offlinePlugin, statePluginFactory)
            .logLevel(logLevel)
            .build()

        val chatUser = io.getstream.chat.android.models.User(
            id = user.id,
            name = user.name,
            image = user.image,
        )

        chatClient.connectUser(
            user = chatUser,
            token = token,
        ).enqueue()
    }
}
