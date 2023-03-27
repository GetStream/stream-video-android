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
import io.getstream.android.push.PushDeviceGenerator
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.engine.StreamCallEngine
import io.getstream.video.android.core.engine.StreamCallEngineImpl
import io.getstream.video.android.core.events.VideoEvent
import io.getstream.video.android.core.input.CallAndroidInput
import io.getstream.video.android.core.input.CallAndroidInputLauncher
import io.getstream.video.android.core.input.internal.DefaultCallAndroidInputLauncher
import io.getstream.video.android.core.input.internal.StreamVideoStateLauncher
import io.getstream.video.android.core.internal.module.CallCoordinatorClientModule
import io.getstream.video.android.core.internal.module.HttpModule
import io.getstream.video.android.core.internal.module.VideoModule
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.ApiKey
import io.getstream.video.android.core.model.Call
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.UserType
import io.getstream.video.android.core.user.UserPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.*

public class StreamVideoBuilder(
    private val context: Context,
    private val user: User,
    private val apiKey: ApiKey,
    private val config: StreamVideoConfig = StreamVideoConfigDefault,
    private val androidInputs: Set<CallAndroidInput> = emptySet(),
    private val inputLauncher: CallAndroidInputLauncher = DefaultCallAndroidInputLauncher,
    private val loggingLevel: LoggingLevel = LoggingLevel.NONE,
    private inline val callEngineBuilder: ((CoroutineScope) -> StreamCallEngine)? = null,
    private val pushDeviceGenerators: List<PushDeviceGenerator> = emptyList(),
) {

    val videoDomain: String = "video-edge-frankfurt-ce1.stream-io-api.com"


    public fun build(): StreamVideo {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle

        if (apiKey.isBlank() ||
            user.id.isBlank()
        ) throw IllegalArgumentException("The API key, user ID and token cannot be empty!")

        val preferences = UserPreferencesManager.initialize(context).apply {
            storeUserCredentials(user)
            storeApiKey(apiKey)
        }

        val httpModule = HttpModule.getOrCreate(loggingLevel.httpLoggingLevel, preferences)

        val module = VideoModule(
            appContext = context,
            preferences = preferences,
            videoDomain = videoDomain
        )

        val socket = module.socket()
        val userState = module.userState()

        val callCoordinatorClientModule = CallCoordinatorClientModule(
            user = user,
            preferences = preferences,
            appContext = context,
            lifecycle = lifecycle,
            okHttpClient = httpModule.okHttpClient,
            videoDomain = videoDomain
        )

        val scope = CoroutineScope(DispatcherProvider.IO)

        val engine: StreamCallEngine =
            callEngineBuilder?.invoke(scope) ?: StreamCallEngineImpl(
                parentScope = scope,
                coordinatorClient = callCoordinatorClientModule.callCoordinatorClient(),
                config = config,
                getCurrentUserId = { preferences.getUserCredentials()?.id ?: "" }
            )

        return StreamVideoImpl(
            context = context,
            scope = scope,
            config = config,
            engine = engine,
            loggingLevel = loggingLevel,
            callCoordinatorClient = callCoordinatorClientModule.callCoordinatorClient(),
            preferences = preferences,
            lifecycle = lifecycle,
            socket = socket,
            socketStateService = module.socketStateService(),
            userState = userState,
            networkStateProvider = module.networkStateProvider()
        ).also { streamVideo ->
            StreamVideoStateLauncher(context, streamVideo, androidInputs, inputLauncher).run(scope)
            scope.launch {
                pushDeviceGenerators
                    .firstOrNull { it.isValidForThisDevice(context) }
                    ?.let {
                        it.onPushDeviceGeneratorSelected()
                        it.asyncGeneratePushDevice {
                            scope.launch {
                                streamVideo.createDevice(
                                    token = it.token,
                                    pushProvider = it.pushProvider.key
                                )
                            }
                        }
                    }
            }
        }
    }
}

public interface AudioFilter {
}
public interface VideoFilter {
}

sealed class GEO{
    /** Run calls over our global edge network, this is the default and right for most applications */
    object GlobalEdgeNetwork : GEO()
}

sealed class TokenType{
    /** A user token */
    object User : TokenType()
    /** A call specific token */
    object Call : TokenType()
}

public interface Disposable {
    public val isDisposed: Boolean
    public fun dispose()
}

public fun interface VideoEventListener<EventT : VideoEvent> {
    public fun onEvent(event: EventT)
}

public class StreamVideoBuilder2(
    private val context: Context,
    /** Your Stream API Key, you can find it in the dashboard */
    private val apiKey: ApiKey,
    /** Your GEO routing policy, supports geofencing for privacy concerns */
    private val geo: GEO,
    /** The user object, can be a regular user, guest user or anonymous */
    private val user: User,
    /** The token for this user generated using your API secret on your server */
    private val userToken: String = "",
    /** If a token is expired, the token provider makes a request to your backend for a new token */
    private val tokenProvider: ((type: TokenType, user: User?, call: Call?) -> String)? = null,
    /** Audio filters enable you to add custom effects to your audio before its send to the server */
    private val audioFilters: List<AudioFilter> = emptyList(),
    /** Video filters enable you to change the video before it's send. */
    private val videoFilters: List<VideoFilter> = emptyList(),
    /** Connection timeout in seconds */
    private val connectionTimeout: Float = 10.0F,
    /** Logging level */
    private val loggingLevel: LoggingLevel = LoggingLevel.NONE,
    /** Overwrite the default notification logic for incoming calls */
    private val ringNotification: ((call: Call) -> Notification?)? = null,
    /** Support for different push providers */
    private val pushDeviceGenerators: List<PushDeviceGenerator> = emptyList(),
) {
    val videoDomain: String = "video-edge-frankfurt-ce1.stream-io-api.com"

    public fun build(): StreamVideo {
        /** URL overwrite to allow for testing against a local instance of video */

        val androidInputs: Set<CallAndroidInput> = emptySet()
        val inputLauncher: CallAndroidInputLauncher = DefaultCallAndroidInputLauncher
        val lifecycle = ProcessLifecycleOwner.get().lifecycle

        if (apiKey.isBlank()
        ) throw IllegalArgumentException("The API key can not be empty")


        val preferences = UserPreferencesManager.initialize(context).apply {
            storeUserCredentials(user)
            storeApiKey(apiKey)
            if (userToken.isNotEmpty()) {
                storeUserToken(userToken)
            }
        }

        val httpModule = HttpModule.getOrCreate(loggingLevel.httpLoggingLevel, preferences)

        val module = VideoModule(
            appContext = context,
            preferences = preferences,
            videoDomain = videoDomain
        )

        val socket = module.socket()
        val userState = module.userState()

        val callCoordinatorClientModule = CallCoordinatorClientModule(
            user = user,
            preferences = preferences,
            appContext = context,
            lifecycle = lifecycle,
            okHttpClient = httpModule.okHttpClient,
            videoDomain = videoDomain
        )

        val scope = CoroutineScope(DispatcherProvider.IO)
        val config = StreamVideoConfigDefault

        val engine: StreamCallEngine = StreamCallEngineImpl(
            parentScope = scope,
            coordinatorClient = callCoordinatorClientModule.callCoordinatorClient(),
            config = config,
            getCurrentUserId = { preferences.getUserCredentials()?.id ?: "" }
        )

        return StreamVideoImpl(
            context = context,
            scope = scope,
            config = config,
            engine = engine,
            loggingLevel = loggingLevel,
            callCoordinatorClient = callCoordinatorClientModule.callCoordinatorClient(),
            preferences = preferences,
            lifecycle = lifecycle,
            socket = socket,
            socketStateService = module.socketStateService(),
            userState = userState,
            networkStateProvider = module.networkStateProvider()
        ).also { streamVideo ->
            StreamVideoStateLauncher(context, streamVideo, androidInputs, inputLauncher).run(scope)
            scope.launch {
                pushDeviceGenerators
                    .firstOrNull { it.isValidForThisDevice(context) }
                    ?.let {
                        it.onPushDeviceGeneratorSelected()
                        it.asyncGeneratePushDevice {
                            scope.launch {
                                streamVideo.createDevice(
                                    token = it.token,
                                    pushProvider = it.pushProvider.key
                                )
                            }
                        }
                    }
            }
        }
    }
}
