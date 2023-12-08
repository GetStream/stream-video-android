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

package io.getstream.video.android.core

import android.app.Notification
import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import com.jakewharton.threetenabp.AndroidThreeTen
import io.getstream.log.StreamLog
import io.getstream.log.android.AndroidStreamLogger
import io.getstream.log.streamLog
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.internal.module.ConnectionModule
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.notifications.NotificationConfig
import io.getstream.video.android.core.notifications.internal.StreamNotificationManager
import io.getstream.video.android.core.notifications.internal.storage.DeviceTokenStorage
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserToken
import io.getstream.video.android.model.UserType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The [StreamVideoBuilder] is used to create a new instance of the [StreamVideo] client. This is the
 * most essential class to connect to the Stream server and initialize everything you need to implement audio/video calls.
 *
 * ```kotlin
 * val client = StreamVideoBuilder(
 *      context = context,
 *      apiKey = apiKey,
 *      geo = GEO.GlobalEdgeNetwork,
 *      user = user,
 *      token = token,
 *      loggingLevel = LoggingLevel.BODY
 *  )
 *```
 *
 * @property context Android [Context] to be used for initializing Android resources.
 * @property apiKey Your Stream API Key, you can find it in the dashboard.
 * @property geo Your GEO routing policy, supports geofencing for privacy concerns.
 * @property user The user object, can be a regular user, guest user or anonymous.
 * @property token The token for this user generated using your API secret on your server.
 * @property tokenProvider If a token is expired, the token provider makes a request to your backend for a new token.
 * @property loggingLevel Represents and wraps the HTTP logging level for our API service.
 * @property notificationConfig The configurations for handling push notification.
 * @property ringNotification Overwrite the default notification logic for incoming calls.
 * @property connectionTimeoutInMs Connection timeout in seconds.
 * @property ensureSingleInstance Verify that only 1 version of the video client exists, prevents integration mistakes.
 * @property videoDomain URL overwrite to allow for testing against a local instance of video.
 * @property runForegroundServiceForCalls If set to true, when there is an active call the SDK will run a foreground service to keep the process alive. (default: true)
 * @property testSfuAddress SFU address (IP:port) to be used for testing. Leave null if not needed.
 */
public class StreamVideoBuilder @JvmOverloads constructor(
    context: Context,
    private val apiKey: ApiKey,
    private val geo: GEO = GEO.GlobalEdgeNetwork,
    private var user: User = User(
        type = UserType.Anonymous,
    ),
    private val token: UserToken = "",
    private val tokenProvider: (suspend (error: Throwable?) -> String)? = null,
    private val loggingLevel: LoggingLevel = LoggingLevel(),
    private val notificationConfig: NotificationConfig = NotificationConfig(),
    private val ringNotification: ((call: Call) -> Notification?)? = null,
    private val connectionTimeoutInMs: Long = 10000,
    private var ensureSingleInstance: Boolean = true,
    private val videoDomain: String = "video.stream-io-api.com",
    private val runForegroundServiceForCalls: Boolean = true,
    private val testSfuAddress: String? = null,
) {
    private val context: Context = context.applicationContext

    val scope = CoroutineScope(DispatcherProvider.IO)

    public fun build(): StreamVideo {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle

        val existingInstance = StreamVideo.instanceOrNull()
        if (existingInstance != null && ensureSingleInstance) {
            throw IllegalArgumentException(
                "Creating 2 instance of the video client will cause bugs with call.state. Before creating a new client, please remove the old one. You can remove the old client using StreamVideo.removeClient()",
            )
        }

        if (apiKey.isBlank()) {
            throw IllegalArgumentException("The API key can not be empty")
        }

        if (token.isBlank() && tokenProvider == null && user.type == UserType.Authenticated) {
            throw IllegalArgumentException(
                "Either a user token or a token provider must be provided",
            )
        }

        if (user.type == UserType.Authenticated && user.id.isEmpty()) {
            throw IllegalArgumentException(
                "Please specify the user id for authenticated users",
            )
        } else if (user.type == UserType.Anonymous && user.id.isEmpty()) {
            val randomId = UUID.randomUUID().toString()
            user = user.copy(id = "anon-$randomId")
        }

        /** initialize Stream internal loggers. */
        StreamLog.install(AndroidStreamLogger())
        StreamLog.setValidator { priority, _ -> priority.level >= loggingLevel.priority.level }

        /** android JSR-310 backport backport. */
        AndroidThreeTen.init(context)

        // This connection module class exposes the connections to the various retrofit APIs.
        val connectionModule = ConnectionModule(
            context = context,
            scope = scope,
            videoDomain = videoDomain,
            connectionTimeoutInMs = connectionTimeoutInMs,
            loggingLevel = loggingLevel,
            user = user,
            apiKey = apiKey,
            userToken = token,
        )

        val deviceTokenStorage = DeviceTokenStorage(context)

        // install the StreamNotificationManager to configure push notifications.
        val streamNotificationManager = StreamNotificationManager.install(
            context = context,
            scope = scope,
            notificationConfig = notificationConfig,
            api = connectionModule.api,
            deviceTokenStorage = deviceTokenStorage,
        )

        // create the client
        val client = StreamVideoImpl(
            context = context,
            _scope = scope,
            user = user,
            apiKey = apiKey,
            token = token,
            tokenProvider = tokenProvider,
            loggingLevel = loggingLevel,
            lifecycle = lifecycle,
            connectionModule = connectionModule,
            streamNotificationManager = streamNotificationManager,
            runForeGroundService = runForegroundServiceForCalls,
            testSfuAddress = testSfuAddress,
        )

        if (user.type == UserType.Guest) {
            connectionModule.updateAuthType("anonymous")
            client.setupGuestUser(user)
        } else if (user.type == UserType.Anonymous) {
            connectionModule.updateAuthType("anonymous")
        }

        // establish a ws connection with the coordinator (we don't support this for anonymous users)
        if (user.type != UserType.Anonymous) {
            scope.launch {
                val result = client.connectAsync().await()
                result.onSuccess {
                    streamLog { "connection succeed! (duration: ${result.getOrNull()})" }
                }.onError {
                    streamLog { it.message }
                }
            }
        }

        // see which location is best to connect to
        scope.launch {
            val location = client.loadLocationAsync().await()
            streamLog { "location initialized: ${location.getOrNull()}" }
        }

        // installs Stream Video instance
        StreamVideo.install(client)

        // Needs to be started after the client is initialised because the VideoPushDelegate
        // is accessing the StreamVideo instance
        scope.launch {
            if (user.type == UserType.Authenticated) {
                client.registerPushDevice()
            }
        }

        return client
    }
}

sealed class GEO {
    /** Run calls over our global edge network, this is the default and right for most applications */
    object GlobalEdgeNetwork : GEO()
}
