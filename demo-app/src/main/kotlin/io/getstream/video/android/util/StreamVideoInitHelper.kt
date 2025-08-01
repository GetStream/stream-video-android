/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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
import android.os.Bundle
import android.util.Log
import io.getstream.android.push.firebase.FirebasePushDeviceGenerator
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.logger.ChatLogLevel
import io.getstream.chat.android.offline.plugin.factory.StreamOfflinePluginFactory
import io.getstream.chat.android.state.plugin.config.StatePluginConfig
import io.getstream.chat.android.state.plugin.factory.StreamStatePluginFactory
import io.getstream.log.Priority
import io.getstream.video.android.BuildConfig
import io.getstream.video.android.app
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.internal.ExperimentalStreamVideoApi
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.notifications.DefaultNotificationIntentBundleResolver
import io.getstream.video.android.core.notifications.DefaultStreamIntentResolver
import io.getstream.video.android.core.notifications.NotificationConfig
import io.getstream.video.android.core.notifications.handlers.CompatibilityStreamNotificationHandler
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfigRegistry
import io.getstream.video.android.core.notifications.internal.service.DefaultCallConfigurations
import io.getstream.video.android.core.socket.common.token.TokenProvider
import io.getstream.video.android.data.services.stream.GetAuthDataResponse
import io.getstream.video.android.data.services.stream.StreamService
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.StreamCallId
import io.getstream.video.android.model.User
import io.getstream.video.android.noise.cancellation.NoiseCancellation
import io.getstream.video.android.notification.LiveStreamMediaNotificationInterceptor
import io.getstream.video.android.notification.PausePlayMediaSessionCallback
import io.getstream.video.android.ui.common.StreamCallActivity
import io.getstream.video.android.ui.common.StreamCallActivityConfiguration
import io.getstream.video.android.util.config.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull

public enum class InitializedState {
    NOT_STARTED, RUNNING, FINISHED, FAILED
}

@SuppressLint("StaticFieldLeak")
object StreamVideoInitHelper {

    private var isInitialising = false
    private lateinit var context: Context
    private val _initState = MutableStateFlow(InitializedState.NOT_STARTED)
    public val initializedState: StateFlow<InitializedState> = _initState
    private var testNotificationConfig: NotificationConfig? = null

    fun init(appContext: Context, testNotificationConfig: NotificationConfig? = null) {
        context = appContext.applicationContext
        this.testNotificationConfig = testNotificationConfig
    }

    suspend fun reloadSdk(
        dataStore: StreamUserDataStore,
        useRandomUserAsFallback: Boolean = true,
    ) {
        StreamVideo.removeClient()
        loadSdk(dataStore, useRandomUserAsFallback)
    }

    /**
     * A helper function that will initialise the [StreamVideo] SDK and also the [ChatClient].
     * Set [useRandomUserAsFallback] to true if you want to use a guest fallback if the user is not
     * logged in.
     */
    suspend fun loadSdk(
        dataStore: StreamUserDataStore,
        useRandomUserAsFallback: Boolean = true,
    ) = AppConfig.load(context) {
        if (StreamVideo.isInstalled) {
            _initState.value = InitializedState.FINISHED
            Log.w("StreamVideoInitHelper", "[initStreamVideo] StreamVideo is already initialised.")
            return@load
        }

        if (isInitialising) {
            _initState.value = InitializedState.RUNNING
            Log.d("StreamVideoInitHelper", "[initStreamVideo] StreamVideo is already initialising")
            return@load
        }

        isInitialising = true
        _initState.value = InitializedState.RUNNING

        try {
            // Load the signed-in user (can be null)
            var loggedInUser = dataStore.data.firstOrNull()?.user
            var authData: GetAuthDataResponse? = null

            // Create and login a random new user if user is null and we allow a random user login
            if (loggedInUser == null && useRandomUserAsFallback) {
                val userId = UserHelper.generateRandomString()

                authData = StreamService.instance.getAuthData(
                    environment = AppConfig.currentEnvironment.value!!.env,
                    userId = userId,
                )

                loggedInUser = User(id = authData.userId, role = "admin")

                // Store the data (note that this datastore belongs to the client - it's not
                // used by the SDK directly in any way)
                dataStore.updateUser(loggedInUser)
            }

            // If we have a logged in user (from the data store or randomly created above)
            // then we can initialise the SDK
            if (loggedInUser != null) {
                if (authData == null) {
                    authData = StreamService.instance.getAuthData(
                        environment = AppConfig.currentEnvironment.value!!.env,
                        userId = loggedInUser.id,
                    )
                }

                initializeStreamChat(
                    context = context,
                    apiKey = authData.apiKey,
                    user = loggedInUser,
                    token = authData.token,
                )

                initializeStreamVideo(
                    context = context,
                    apiKey = authData.apiKey,
                    user = loggedInUser,
                    token = authData.token,
                    loggingLevel = LoggingLevel(priority = Priority.VERBOSE),
                )
            }
            Log.i("StreamVideoInitHelper", "Init successful.")
            _initState.value = InitializedState.FINISHED
        } catch (e: Exception) {
            _initState.value = InitializedState.FAILED
            Log.e("StreamVideoInitHelper", "Init failed.", e)
        }

        isInitialising = false
    }

    private fun initializeStreamChat(
        context: Context,
        apiKey: String,
        user: User,
        token: String,
    ) {
        val offlinePlugin = StreamOfflinePluginFactory(context)
        val statePluginFactory = StreamStatePluginFactory(
            config = StatePluginConfig(
                backgroundSyncEnabled = true,
                userPresence = true,
            ),
            appContext = context,
        )

        val logLevel = if (BuildConfig.DEBUG) ChatLogLevel.ALL else ChatLogLevel.NOTHING
        val chatClient = ChatClient.Builder(apiKey, context)
            .withPlugins(offlinePlugin, statePluginFactory)
            .logLevel(logLevel)
            .build()

        val chatUser = io.getstream.chat.android.models.User(
            id = user.id,
            name = user.name.orEmpty(),
            image = user.image.orEmpty(),
        )

        chatClient.connectUser(
            user = chatUser,
            token = token,
        ).enqueue()
    }

    /** Sets up and returns the [StreamVideo] required to connect to the API. */
    @OptIn(ExperimentalStreamVideoApi::class)
    private fun initializeStreamVideo(
        context: Context,
        apiKey: ApiKey,
        user: User,
        token: String,
        loggingLevel: LoggingLevel,
    ): StreamVideo {
        val callServiceConfigRegistry = CallServiceConfigRegistry()
        callServiceConfigRegistry.register(
            DefaultCallConfigurations.getLivestreamGuestCallServiceConfig(),
        )
        return StreamVideoBuilder(
            context = context,
            apiKey = apiKey,
            user = user,
            token = token,
            loggingLevel = loggingLevel,
            ensureSingleInstance = false,
            callServiceConfigRegistry = callServiceConfigRegistry,
            notificationConfig = testNotificationConfig ?: NotificationConfig(
                pushDeviceGenerators = listOf(
                    FirebasePushDeviceGenerator(
                        context = context,
                        providerName = "firebase",
                    ),
                ),
                hideRingingNotificationInForeground = true,
                notificationHandler = CompatibilityStreamNotificationHandler(
                    application = context.app,
                    mediaSessionCallback = PausePlayMediaSessionCallback(),
                    updateNotificationBuilderInterceptor = LiveStreamMediaNotificationInterceptor(context),
                    hideRingingNotificationInForeground = true,
                    intentResolver = DefaultStreamIntentResolver(
                        context,
                        object : DefaultNotificationIntentBundleResolver() {

                            override fun getIncomingCallBundle(
                                callId: StreamCallId,
                                notificationId: Int,
                                payload: Map<String, Any?>,
                            ): Bundle {
                                return StreamCallActivity.callIntentBundle(
                                    callId,
                                    configuration = StreamCallActivityConfiguration(
                                        closeScreenOnCallEnded = true,
                                    ),
                                    leaveWhenLastInCall = true,
                                )
                            }

                            override fun getAcceptCallBundle(
                                callId: StreamCallId,
                                notificationId: Int,
                                payload: Map<String, Any?>,
                            ): Bundle {
                                return StreamCallActivity.callIntentBundle(
                                    callId,
                                    configuration = StreamCallActivityConfiguration(
                                        closeScreenOnCallEnded = true,
                                    ),
                                    leaveWhenLastInCall = true,
                                )
                            }

                            override fun getRejectCallBundle(
                                callId: StreamCallId,
                                payload: Map<String, Any?>,
                            ): Bundle {
                                return StreamCallActivity.callIntentBundle(
                                    callId,
                                    configuration = StreamCallActivityConfiguration(
                                        closeScreenOnCallEnded = true,
                                    ),
                                    leaveWhenLastInCall = true,
                                )
                            }
                        },
                    ),
                ),
            ),
            tokenProvider = object : TokenProvider {
                override suspend fun loadToken(): String {
                    val email = user.custom?.get("email")
                    val authData = StreamService.instance.getAuthData(
                        environment = AppConfig.currentEnvironment.value!!.env,
                        userId = email,
                    )
                    return authData.token
                }
            },
            callUpdatesAfterLeave = true,
            appName = "Stream Video Demo App",
            audioProcessing = NoiseCancellation(context),
        ).build()
    }
}
