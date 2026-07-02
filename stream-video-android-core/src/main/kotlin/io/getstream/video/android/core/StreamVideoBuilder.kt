/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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
import android.os.Build
import androidx.lifecycle.ProcessLifecycleOwner
import com.jakewharton.threetenabp.AndroidThreeTen
import io.getstream.android.core.api.StreamClient
import io.getstream.android.core.api.authentication.StreamTokenProvider
import io.getstream.android.core.api.log.StreamLoggerProvider
import io.getstream.android.core.api.model.StreamUser
import io.getstream.android.core.api.model.config.StreamClientSerializationConfig
import io.getstream.android.core.api.model.config.StreamComponentProvider
import io.getstream.android.core.api.model.config.StreamSocketConfig
import io.getstream.android.core.api.model.value.StreamApiKey
import io.getstream.android.core.api.model.value.StreamHttpClientInfoHeader
import io.getstream.android.core.api.model.value.StreamUserId
import io.getstream.android.core.api.model.value.StreamWsUrl
import io.getstream.android.core.api.observers.lifecycle.StreamLifecycleMonitor
import io.getstream.android.core.api.subscribe.StreamSubscriptionManager
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
import io.getstream.video.android.core.notifications.internal.telecom.TelecomConfig
import io.getstream.video.android.core.permission.android.DefaultStreamPermissionCheck
import io.getstream.video.android.core.permission.android.StreamPermissionCheck
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.scope.UserScope
import io.getstream.video.android.core.socket.common.token.RepositoryTokenProvider
import io.getstream.video.android.core.socket.common.token.TokenProvider
import io.getstream.video.android.core.socket.common.token.TokenRepository
import io.getstream.video.android.core.socket.coordinator.v2.GuestStreamTokenProvider
import io.getstream.video.android.core.socket.coordinator.v2.IntegrationStreamTokenProvider
import io.getstream.video.android.core.socket.coordinator.v2.VideoEventSerialization
import io.getstream.video.android.core.socket.coordinator.v2.WritableUserRepository
import io.getstream.video.android.core.sounds.RingingCallVibrationConfig
import io.getstream.video.android.core.sounds.Sounds
import io.getstream.video.android.core.sounds.defaultResourcesRingingConfig
import io.getstream.video.android.core.sounds.disableVibrationConfig
import io.getstream.video.android.core.sounds.toSounds
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserToken
import io.getstream.video.android.model.UserType
import io.getstream.webrtc.ManagedAudioProcessingFactory
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.util.concurrent.atomic.AtomicReference

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
 *      sounds = ringingConfig(
 *                 defaultResourcesRingingConfig(context),
 *                 defaultMutedRingingConfig(true, true)
 *             ),
 *      loggingLevel = LoggingLevel.BODY,
 *
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
 * @property loggingLevel Represents and wraps SDK logging levels for Stream logger, HTTP interceptor and native WebRTC logging.
 * @property notificationConfig The configurations for handling push notification.
 * @property ringNotification Overwrite the default notification logic for incoming calls.
 * @property connectionTimeoutInMs Connection timeout in seconds.
 * @property ensureSingleInstance Verify that only 1 version of the video client exists. Prevents integration mistakes.
 * @property videoDomain URL overwrite to allow for testing against a local instance of video.
 * @property callServiceConfig Configuration for the call foreground service. See [CallServiceConfig]. (Deprecated) Use `callServiceConfigRegistry` instead.
 * @property localCoordinatorAddress Local coordinator address (IP:port) to be used for testing. Leave null if not needed.
 * @property sounds Overwrite the default SDK sounds. See [io.getstream.video.android.core.sounds.RingingConfig].
 * @property permissionCheck Used to check for system permission based on call capabilities. See [StreamPermissionCheck].
 * @property crashOnMissingPermission Throw an exception or just log an error if [permissionCheck] fails.
 * @property appName Optional name for the application that is using the Stream Video SDK. Used for logging and debugging purposes.
 * @property audioProcessing The audio processor used for custom modifications to audio data within WebRTC.
 * @property callServiceConfigRegistry The audio processor used for custom modifications to audio data within WebRTC.
 * @property leaveAfterDisconnectSeconds The number of seconds to wait before leaving the call after the connection is disconnected.
 * @property callUpdatesAfterLeave Whether to update the call state after leaving the call.
 * @property connectOnInit Determines whether the socket should automatically connect as soon as a user is set.
 *          If `false`, the connection is established only when explicitly requested or when core SDK features
 *          (such as audio or video calls) are used.
 *          When `false` and the socket is not connected, incoming calls will not be delivered via WebSocket events;
 *          the SDK will rely on push notifications instead.
 *          To start receiving WebSocket events, explicitly invoke `client.connect()`.
 * @property rejectCallWhenBusy Automatically rejects incoming calls when the user is already in another call.
 *          When enabled, the SDK suppresses incoming call notifications.
 *          CallRingEvent will not be propagated if there is an active or ongoing ringing call.
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
    } ?: RepositoryTokenProvider(tokenRepository),
    private val loggingLevel: LoggingLevel = LoggingLevel(),
    private val notificationConfig: NotificationConfig = NotificationConfig(),
    private val ringNotification: ((call: Call) -> Notification?)? = null,
    private val connectionTimeoutInMs: Long = 10_000,
    private var ensureSingleInstance: Boolean = true,
    private val videoDomain: String = "video.stream-io-api.com",
    @Deprecated(
        "This property is ignored. Set runCallServiceInForeground in the callServiceConfigRegistry parameter instead.",
        replaceWith = ReplaceWith("callServiceConfigRegistry"),
        level = DeprecationLevel.WARNING,
    )
    private val runForegroundServiceForCalls: Boolean = true,
    @Deprecated(
        "Use callServiceConfigRegistry instead",
        replaceWith = ReplaceWith("callServiceConfigRegistry"),
        level = DeprecationLevel.WARNING,
    )
    private val callServiceConfig: CallServiceConfig? = null,
    private val callServiceConfigRegistry: CallServiceConfigRegistry? = null,
    private val localCoordinatorAddress: String? = null,
    private val sounds: Sounds = defaultResourcesRingingConfig(context).toSounds(),
    private val vibrationConfig: RingingCallVibrationConfig = disableVibrationConfig(),
    private val crashOnMissingPermission: Boolean = false,
    private val permissionCheck: StreamPermissionCheck = DefaultStreamPermissionCheck(),
    @Deprecated(
        message = "This property is ignored. Set audioUsage in the callServiceConfigRegistry parameter instead.",
        level = DeprecationLevel.WARNING,
    )
    private val audioUsage: Int = defaultAudioUsage,
    private val appName: String? = null,
    private val audioProcessing: ManagedAudioProcessingFactory? = null,
    private val leaveAfterDisconnectSeconds: Long = 30,
    private val callUpdatesAfterLeave: Boolean = false,
    private val enableStatsReporting: Boolean = true,
    @InternalStreamVideoApi
    private val enableStereoForSubscriber: Boolean = true,
    private val telecomConfig: TelecomConfig? = null,
    private val connectOnInit: Boolean = true,
    private val rejectCallWhenBusy: Boolean = false,
) {
    private val context: Context = context.applicationContext
    private val scope = UserScope(ClientScope())

    private var apiUrl: String? = null
    private var wssUrl: String? = null

    /**
     * Seam that constructs the core [StreamClient] instance. Tests substitute this lambda so they
     * can assert factory arguments without invoking core's Android-service-dependent
     * initialisation (RESEARCH Pitfall 2).
     */
    @InternalStreamVideoApi
    internal var streamClientFactory: (StreamClientFactoryArgs) -> StreamClient =
        ::defaultStreamClientFactory

    /**
     * Substitute the [StreamClient] factory used by [build]. Intended for preview/snapshot
     * harnesses that instantiate the video client under Paparazzi's layoutlib bridge, where
     * core's default factory reaches Android system services (e.g. WifiManager) that are
     * unavailable in the test environment.
     */
    @InternalStreamVideoApi
    public fun streamClientFactory(
        factory: (StreamClientFactoryArgs) -> StreamClient,
    ): StreamVideoBuilder = apply { streamClientFactory = factory }

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

        if (user.type == UserType.Authenticated && token.isBlank()) {
            throw IllegalArgumentException("The token cannot be blank for authenticated users")
        }

        if (user.type == UserType.Authenticated && user.id.isBlank()) {
            throw IllegalArgumentException(
                "The user ID cannot be empty for authenticated users",
            )
        }

        if (user.role.isNullOrBlank()) {
            user = user.copy(role = "user")
        }

        setupStreamLog()

        // Android JSR-310 backport backport
        AndroidThreeTen.init(context)
        tokenRepository.updateToken(token)
        // This connection module class exposes the connections to the various retrofit APIs.
        val resolvedApiUrl = localCoordinatorAddress?.let {
            "http://${it.trimEnd('/')}"
        } ?: apiUrl ?: "https:///$videoDomain"
        val resolvedWssUrl = localCoordinatorAddress?.let {
            "ws://${it.trimEnd('/')}/video/connect"
        } ?: wssUrl ?: "wss://$videoDomain/video/connect"

        val coordinatorConnectionModule = CoordinatorConnectionModule(
            context = context,
            scope = scope,
            apiUrl = resolvedApiUrl,
            wssUrl = resolvedWssUrl,
            connectionTimeoutInMs = connectionTimeoutInMs,
            loggingLevel = loggingLevel,
            user = user,
            apiKey = apiKey,
            tokenProvider = tokenProvider,
            lifecycle = lifecycle,
            tokenRepository = tokenRepository,
        )

        // Adapter that satisfies GuestStreamTokenProvider's WritableUserRepository contract until
        // the SDK-wide user repository (PR #1708-equivalent) lands. See Plan 02-01 SUMMARY.
        val userRepository = InMemoryWritableUserRepository(user)

        // Select the core token provider based on user type.
        //  - Authenticated: adapt the integration-supplied TokenProvider.
        //  - Guest: SDK obtains the JWT via createGuest REST call (D-06).
        //  - Anonymous: rejected — anonymous users do not open a coordinator socket (D-07).
        val streamTokenProvider: StreamTokenProvider = when (user.type) {
            UserType.Authenticated -> IntegrationStreamTokenProvider(tokenProvider)
            UserType.Guest -> GuestStreamTokenProvider(
                api = coordinatorConnectionModule.api,
                initialUser = user,
                userRepository = userRepository,
            )
            UserType.Anonymous -> error(
                "Anonymous users do not open a coordinator socket (D-07). " +
                    "Construct StreamVideoBuilder with UserType.Authenticated or UserType.Guest, " +
                    "or use the SDK's REST-only operations directly.",
            )
        }

        // Build the core client and eagerly inject it into StreamVideoClient (D-03).
        val streamClient = streamClientFactory.invoke(
            StreamClientFactoryArgs(
                scope = scope,
                context = context,
                user = user,
                apiKey = apiKey,
                resolvedWssUrl = resolvedWssUrl,
                appName = appName,
                appVersion = null,
                lifecycle = lifecycle,
                tokenProvider = streamTokenProvider,
            ),
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
        val callConfigRegistry = createCallConfigurationRegistry(
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
            lifecycle = lifecycle,
            coordinatorConnectionModule = coordinatorConnectionModule,
            streamClient = streamClient,
            tokenProvider = tokenProvider,
            streamNotificationManager = streamNotificationManager,
            enableCallNotificationUpdates = notificationConfig.enableCallNotificationUpdates,
            callServiceConfigRegistry = callConfigRegistry,
            sounds = sounds,
            permissionCheck = permissionCheck,
            crashOnMissingPermission = crashOnMissingPermission,
            appName = appName,
            audioProcessing = audioProcessing,
            loggingLevel = loggingLevel,
            leaveAfterDisconnectSeconds = leaveAfterDisconnectSeconds,
            enableCallUpdatesAfterLeave = callUpdatesAfterLeave,
            enableStatsCollection = enableStatsReporting,
            vibrationConfig = vibrationConfig,
            enableStereoForSubscriber = enableStereoForSubscriber,
            telecomConfig = telecomConfig,
            tokenRepository = tokenRepository,
            rejectCallWhenBusy = rejectCallWhenBusy,
        )

        // Establish a WS connection with the coordinator (we don't support this for anonymous users)
        if (user.type == UserType.Authenticated) {
            scope.launch {
                try {
                    if (notificationConfig.autoRegisterPushDevice) {
                        client.registerPushDevice()
                    }
                    if (connectOnInit) {
                        val result = client.connectAsync().await()
                        result.onSuccess {
                            streamLog { "Connection succeeded! (duration: ${result.getOrNull()})" }
                        }.onError {
                            streamLog { it.message }
                        }
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

    private fun setupStreamLog() {
        if (!StreamLog.isInstalled) {
            // Initialize Stream internal loggers
            StreamLog.install(AndroidStreamLogger())
            StreamLog.setValidator { priority, _ -> priority.level >= loggingLevel.priority.level }
        }
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
                                enableTelecom(legacyCallConfig.enableTelecom)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Refactor Later
 */
internal val tokenRepository = TokenRepository("")

sealed class GEO {
    /** Run calls over our global edge network, this is the default and right for most applications */
    object GlobalEdgeNetwork : GEO()
}

/**
 * Aggregates the arguments handed to [StreamClient]'s factory function. Grouping them makes the
 * `streamClientFactory` seam a single-parameter lambda that is easy to substitute in tests.
 */
@InternalStreamVideoApi
public data class StreamClientFactoryArgs(
    val scope: kotlinx.coroutines.CoroutineScope,
    val context: Context,
    val user: User,
    val apiKey: ApiKey,
    val resolvedWssUrl: String,
    val appName: String?,
    val appVersion: String?,
    val lifecycle: androidx.lifecycle.Lifecycle,
    val tokenProvider: StreamTokenProvider,
)

private fun defaultStreamClientFactory(args: StreamClientFactoryArgs): StreamClient {
    val logProvider: StreamLoggerProvider = StreamLoggerProvider.defaultAndroidLogger()

    val clientInfoHeader = StreamHttpClientInfoHeader.create(
        product = "video",
        productVersion = BuildConfig.STREAM_VIDEO_VERSION,
        os = "Android",
        apiLevel = Build.VERSION.SDK_INT,
        deviceModel = Build.MODEL,
        app = args.appName ?: "unknown",
        appVersion = args.appVersion ?: "unknown",
    )

    // D-06 / D-07: guest users authenticate via jwt (JWT obtained via createGuest);
    // never use the anonymous factory for the coordinator socket.
    val socketConfig = StreamSocketConfig.jwt(
        url = StreamWsUrl.fromString(args.resolvedWssUrl),
        apiKey = StreamApiKey.fromString(args.apiKey),
        clientInfoHeader = clientInfoHeader,
    )

    val serializationConfig = StreamClientSerializationConfig.default(
        productEvents = VideoEventSerialization(),
    )

    // Route the integration-supplied Lifecycle through core's LifecycleMonitor so that the
    // legacy StreamLifecycleObserver is no longer needed on the coordinator path (D-10).
    val streamLifecycleMonitor = StreamLifecycleMonitor(
        logger = logProvider.taggedLogger("SVLifecycleMonitor"),
        lifecycle = args.lifecycle,
        subscriptionManager = StreamSubscriptionManager(
            logger = logProvider.taggedLogger("SVLifecycleSubs"),
        ),
    )

    return StreamClient(
        scope = args.scope,
        context = args.context.applicationContext,
        user = StreamUser(
            id = StreamUserId.fromString(args.user.id),
            name = args.user.name,
            imageURL = args.user.image,
            customData = args.user.custom ?: emptyMap(),
        ),
        tokenProvider = args.tokenProvider,
        products = listOf("video"),
        socketConfig = socketConfig,
        serializationConfig = serializationConfig,
        // Phase 2: keep video's separate Retrofit OkHttpClient; HTTP unification is Phase 4+.
        httpConfig = null,
        components = StreamComponentProvider(
            logProvider = logProvider,
            lifecycleMonitor = streamLifecycleMonitor,
        ),
    )
}

/**
 * In-memory implementation of [WritableUserRepository] used by the guest-user JWT flow.
 * Replaced by the SDK-wide user repository once the concrete type lands (PR #1708-equivalent).
 */
internal class InMemoryWritableUserRepository(initial: User) : WritableUserRepository {
    private val ref = AtomicReference(initial)
    val user: User get() = ref.get()
    override fun setUser(user: User) {
        ref.set(user)
    }
}
