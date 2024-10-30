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
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.callServiceConfig
import io.getstream.video.android.core.notifications.internal.storage.DeviceTokenStorage
import io.getstream.video.android.core.permission.android.DefaultStreamPermissionCheck
import io.getstream.video.android.core.permission.android.StreamPermissionCheck
import io.getstream.video.android.core.sounds.Sounds
import io.getstream.video.android.core.sounds.defaultResourcesRingingConfig
import io.getstream.video.android.core.sounds.toSounds
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserToken
import io.getstream.video.android.model.UserType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.webrtc.ManagedAudioProcessingFactory
import java.net.ConnectException

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
 *      loggingLevel = LoggingLevel.BODY,
 *      // ...
 * ).build()
 *```
 *
 * @property context Android [Context] to be used for initializing Android resources.
 * @property apiKey Your Stream API Key. You can find it in the dashboard.
 * @property geo Your GEO routing policy. Supports geofencing for privacy concerns.
 * @property user The user object. Can be an authenticated user, guest user or anonymous.
 * @property token The token for this user, generated using your API secret on your server.
 * @property tokenProvider Used to make a request to your backend for a new token when the token has expired.
 * @property loggingLevel Represents and wraps the HTTP logging level for our API service.
 * @property notificationConfig The configurations for handling push notification.
 * @property ringNotification Overwrite the default notification logic for incoming calls.
 * @property connectionTimeoutInMs Connection timeout in seconds.
 * @property ensureSingleInstance Verify that only 1 version of the video client exists. Prevents integration mistakes.
 * @property videoDomain URL overwrite to allow for testing against a local instance of video.
 * @property runForegroundServiceForCalls If set to true, when there is an active call the SDK will run a foreground service to keep the process alive. (default: true)
 * @property callServiceConfig Configuration for the call foreground service. See [CallServiceConfig].
 * @property localSfuAddress Local SFU address (IP:port) to be used for testing. Leave null if not needed.
 * @property sounds Overwrite the default SDK sounds. See [Sounds].
 * @property permissionCheck Used to check for system permission based on call capabilities. See [StreamPermissionCheck].
 * @property crashOnMissingPermission Throw an exception or just log an error if [permissionCheck] fails.
 * @property audioUsage Used to signal to the system how to treat the audio tracks (voip or media).
 * @property appName Optional name for the application that is using the Stream Video SDK. Used for logging and debugging purposes.
 * @property audioProcessing The audio processor used for custom modifications to audio data within WebRTC.
 *
 * @see build
 * @see ClientState.connection
 *
 */
public class StreamVideoBuilder @JvmOverloads constructor(
    context: Context,
    private val apiKey: ApiKey,
    private val geo: GEO = GEO.GlobalEdgeNetwork,
    private var user: User = User.anonymous(),
    private val token: UserToken = "",
    private val tokenProvider: (suspend (error: Throwable?) -> String)? = null,
    private val loggingLevel: LoggingLevel = LoggingLevel(),
    private val notificationConfig: NotificationConfig = NotificationConfig(),
    private val ringNotification: ((call: Call) -> Notification?)? = null,
    private val connectionTimeoutInMs: Long = 10000,
    private var ensureSingleInstance: Boolean = true,
    private val videoDomain: String = "video.stream-io-api.com",
    private val hintDomain: String? = null,
    private val runForegroundServiceForCalls: Boolean = true,
    private val callServiceConfig: CallServiceConfig? = null,
    private val localSfuAddress: String? = null,
    private val sounds: Sounds = defaultResourcesRingingConfig(context).toSounds(),
    private val crashOnMissingPermission: Boolean = false,
    private val permissionCheck: StreamPermissionCheck = DefaultStreamPermissionCheck(),
    private val audioUsage: Int = defaultAudioUsage,
    private val appName: String? = null,
    private val audioProcessing: ManagedAudioProcessingFactory? = null,
) {
    private val context: Context = context.applicationContext
    private val scope = CoroutineScope(DispatcherProvider.IO)

    /**
     * Builds the [StreamVideo] client.
     *
     * @return The [StreamVideo] client.
     *
     * @throws RuntimeException If an instance of the client already exists and [ensureSingleInstance] is set to true.
     * @throws IllegalArgumentException If [apiKey] is blank.
     * @throws IllegalArgumentException If [user] type is [UserType.Authenticated] and the [user] id is blank.
     * @throws IllegalArgumentException If [user] type is [UserType.Authenticated] and both [token] and [tokenProvider] are empty.
     * @throws ConnectException If the WebSocket connection fails.
     */
    public fun build(): StreamVideo {

        hintDomain?.let {
            StreamVideoImpl.hintDomain = it
        }

        val lifecycle = ProcessLifecycleOwner.get().lifecycle

        val existingInstance = StreamVideo.instanceOrNull()
        if (existingInstance != null && ensureSingleInstance) {
            throw RuntimeException(
                "Creating 2 instance of the video client will cause bugs with call.state. Before creating a new client, please remove the old one. You can remove the old client using StreamVideo.removeClient()",
            )
        }

        if (apiKey.isBlank()) {
            throw IllegalArgumentException("The API key cannot be blank")
        }

        if (token.isBlank() && tokenProvider == null && user.type == UserType.Authenticated) {
            throw IllegalArgumentException(
                "Either a user token or a token provider must be provided",
            )
        }

        if (user.type == UserType.Authenticated && user.id.isBlank()) {
            throw IllegalArgumentException(
                "The user ID cannot be empty for authenticated users",
            )
        }

        if (user.role.isNullOrBlank()) {
            user = user.copy(role = "user")
        }

        // Initialize Stream internal loggers
        StreamLog.install(AndroidStreamLogger())
        StreamLog.setValidator { priority, _ -> priority.level >= loggingLevel.priority.level }

        // Android JSR-310 backport backport
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

        // Install the StreamNotificationManager to configure push notifications.
        val streamNotificationManager = StreamNotificationManager.install(
            context = context,
            scope = scope,
            notificationConfig = notificationConfig,
            api = connectionModule.api,
            deviceTokenStorage = deviceTokenStorage,
        )

        // Create the client
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
            callServiceConfig = callServiceConfig
                ?: callServiceConfig().copy(
                    runCallServiceInForeground = runForegroundServiceForCalls,
                    audioUsage = audioUsage,
                ),
            testSfuAddress = localSfuAddress,
            sounds = sounds,
            permissionCheck = permissionCheck,
            crashOnMissingPermission = crashOnMissingPermission,
            audioUsage = audioUsage,
            appName = appName,
            audioProcessing = audioProcessing,
        )

        if (user.type == UserType.Guest) {
            connectionModule.updateAuthType("anonymous")
            client.setupGuestUser(user)
        } else if (user.type == UserType.Anonymous) {
            connectionModule.updateAuthType("anonymous")
        }

        // Establish a WS connection with the coordinator (we don't support this for anonymous users)
        if (user.type != UserType.Anonymous) {
            scope.launch {
                try {
                    val result = client.connectAsync().await()
                    result.onSuccess {
                        streamLog { "Connection succeeded! (duration: ${result.getOrNull()})" }
                    }.onError {
                        streamLog { it.message }
                    }
                } catch (e: Throwable) {
                    // If the connect continuation was resumed with an exception, we catch it here.
                    streamLog { e.message.orEmpty() }
                }
            }
        }

        // See which location is best to connect to
        scope.launch {
            val location = client.loadLocationAsync().await()
            streamLog { "location initialized: ${location.getOrNull()}" }
        }

        // Installs Stream Video instance
        StreamVideo.install(client)

        return client
    }
}

sealed class GEO {
    /** Run calls over our global edge network, this is the default and right for most applications */
    object GlobalEdgeNetwork : GEO()
}
