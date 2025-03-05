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
import io.getstream.log.AndroidStreamLogger
import io.getstream.log.StreamLog
import io.getstream.log.streamLog
import io.getstream.video.android.core.call.CallType
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.internal.module.CoordinatorConnectionModule
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.notifications.NotificationConfig
import io.getstream.video.android.core.notifications.internal.StreamNotificationManager
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfigRegistry
import io.getstream.video.android.core.notifications.internal.storage.DeviceTokenStorage
import io.getstream.video.android.core.permission.android.DefaultStreamPermissionCheck
import io.getstream.video.android.core.permission.android.StreamPermissionCheck
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.scope.UserScope
import io.getstream.video.android.core.socket.common.token.ConstantTokenProvider
import io.getstream.video.android.core.socket.common.token.TokenProvider
import io.getstream.video.android.core.sounds.Sounds
import io.getstream.video.android.core.sounds.defaultResourcesRingingConfig
import io.getstream.video.android.core.sounds.toSounds
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserToken
import io.getstream.video.android.model.UserType
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
 * @property callServiceConfig Configuration for the call foreground service. See [CallServiceConfig]. (Deprecated) Use `callServiceConfigRegistry` instead.
 * @property localSfuAddress Local SFU address (IP:port) to be used for testing. Leave null if not needed.
 * @property sounds Overwrite the default SDK sounds. See [Sounds].
 * @property permissionCheck Used to check for system permission based on call capabilities. See [StreamPermissionCheck].
 * @property crashOnMissingPermission Throw an exception or just log an error if [permissionCheck] fails.
 * @property appName Optional name for the application that is using the Stream Video SDK. Used for logging and debugging purposes.
 * @property audioProcessing The audio processor used for custom modifications to audio data within WebRTC.
 * @property callServiceConfigRegistry The audio processor used for custom modifications to audio data within WebRTC.
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
    private val legacyTokenProvider: (suspend (error: Throwable?) -> String)? = null,
    private val tokenProvider: TokenProvider = legacyTokenProvider?.let { legacy ->
        object : TokenProvider {
            override suspend fun loadToken(): String = legacy.invoke(null)
        }
    } ?: ConstantTokenProvider(token),
    private val loggingLevel: LoggingLevel = LoggingLevel(),
    private val notificationConfig: NotificationConfig = NotificationConfig(),
    private val ringNotification: ((call: Call) -> Notification?)? = null,
    private val connectionTimeoutInMs: Long = 10000,
    private var ensureSingleInstance: Boolean = true,
    private val videoDomain: String = "video.stream-io-api.com",
    @Deprecated(
        "Use 'callServiceConfigRegistry' instead",
        replaceWith = ReplaceWith("callServiceConfigRegistry"),
        level = DeprecationLevel.WARNING,
    )
    private val callServiceConfig: CallServiceConfig? = null,
    private val callServiceConfigRegistry: CallServiceConfigRegistry? = null,
    private val localSfuAddress: String? = null,
    private val sounds: Sounds = defaultResourcesRingingConfig(context).toSounds(),
    private val crashOnMissingPermission: Boolean = false,
    private val permissionCheck: StreamPermissionCheck = DefaultStreamPermissionCheck(),
    private val appName: String? = null,
    private val audioProcessing: ManagedAudioProcessingFactory? = null,
    private val leaveAfterDisconnectSeconds: Long = 30,
) {
    private val context: Context = context.applicationContext
    private val scope = UserScope(ClientScope())

    private var apiUrl: String? = null
    private var wssUrl: String? = null

    /**
     * Set the API URL to be used for the video client.
     *
     * For testing purposes only.
     */
    @InternalStreamVideoApi
    public fun forceApiUrl(value: String): StreamVideoBuilder = apply {
        apiUrl = value
    }

    /**
     * Set the WSS URL to be used for the video client.
     *
     * For testing purposes only.
     */
    @InternalStreamVideoApi
    public fun forceWssUrl(value: String): StreamVideoBuilder = apply {
        wssUrl = value
    }

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

        if (token.isBlank()) {
            throw IllegalArgumentException("The token cannot be blank")
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
        val coordinatorConnectionModule = CoordinatorConnectionModule(
            context = context,
            scope = scope,
            apiUrl = apiUrl ?: "https:///$videoDomain",
            wssUrl = wssUrl ?: "wss://$videoDomain/video/connect",
            connectionTimeoutInMs = connectionTimeoutInMs,
            loggingLevel = loggingLevel,
            user = user,
            apiKey = apiKey,
            userToken = token,
            tokenProvider = tokenProvider,
            lifecycle = lifecycle,
        )

        val deviceTokenStorage = DeviceTokenStorage(context)

        // Install the StreamNotificationManager to configure push notifications.
        val streamNotificationManager = StreamNotificationManager.install(
            context = context,
            scope = scope,
            notificationConfig = notificationConfig,
            api = coordinatorConnectionModule.api,
            deviceTokenStorage = deviceTokenStorage,
        )

        // Set call configuration
        var callConfigRegistry = createCallConfigurationRegistry(
            callServiceConfigRegistry,
            callServiceConfig,
        )

        // Create the client
        val client = StreamVideoClient(
            context = context,
            scope = scope,
            user = user,
            apiKey = apiKey,
            token = token,
            tokenProvider = tokenProvider,
            loggingLevel = loggingLevel,
            lifecycle = lifecycle,
            coordinatorConnectionModule = coordinatorConnectionModule,
            streamNotificationManager = streamNotificationManager,
            callServiceConfigRegistry = callConfigRegistry,
            testSfuAddress = localSfuAddress,
            sounds = sounds,
            permissionCheck = permissionCheck,
            crashOnMissingPermission = crashOnMissingPermission,
            appName = appName,
            audioProcessing = audioProcessing,
            leaveAfterDisconnectSeconds = leaveAfterDisconnectSeconds,
        )

        if (user.type == UserType.Guest) {
            coordinatorConnectionModule.updateAuthType("anonymous")
            client.setupGuestUser(user)
        } else if (user.type == UserType.Anonymous) {
            coordinatorConnectionModule.updateAuthType("anonymous")
        }

        // Establish a WS connection with the coordinator (we don't support this for anonymous users)
        if (user.type != UserType.Anonymous) {
            scope.launch {
                try {
                    val result = client.connectAsync().await()
                    if (notificationConfig.autoRegisterPushDevice) {
                        client.registerPushDevice()
                    }
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

    internal fun createCallConfigurationRegistry(
        callServiceConfigRegistry: CallServiceConfigRegistry? = null,
        callServiceConfig: CallServiceConfig? = null,
    ): CallServiceConfigRegistry {
        return if (callServiceConfigRegistry != null) {
            callServiceConfigRegistry
        } else {
            CallServiceConfigRegistry().apply {
                callServiceConfig?.let { legacyCallConfig ->
                    legacyCallConfig.callServicePerType.forEach {
                        CallType.fromName(it.key)?.let { callType ->
                            register(callType.name) {
                                setServiceClass(it.value)
                                setRunCallServiceInForeground(
                                    legacyCallConfig.runCallServiceInForeground,
                                )
                                setAudioUsage(legacyCallConfig.audioUsage)
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class GEO {
    /** Run calls over our global edge network, this is the default and right for most applications */
    object GlobalEdgeNetwork : GEO()
}
