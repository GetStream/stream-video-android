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

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import io.getstream.android.push.PushDeviceGenerator
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.engine.StreamCallEngine
import io.getstream.video.android.core.engine.StreamCallEngineImpl
import io.getstream.video.android.core.input.CallAndroidInput
import io.getstream.video.android.core.input.CallAndroidInputLauncher
import io.getstream.video.android.core.input.internal.DefaultCallAndroidInputLauncher
import io.getstream.video.android.core.input.internal.StreamVideoStateLauncher
import io.getstream.video.android.core.internal.module.CallCoordinatorClientModule
import io.getstream.video.android.core.internal.module.HttpModule
import io.getstream.video.android.core.internal.module.VideoModule
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.ApiKey
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.user.UserPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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

    public fun build(): StreamVideo {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle

        if (apiKey.isBlank() ||
            user.id.isBlank() ||
            user.token.isBlank()
        ) throw IllegalArgumentException("The API key, user ID and token cannot be empty!")

        val preferences = UserPreferencesManager.initialize(context).apply {
            storeUserCredentials(user)
            storeApiKey(apiKey)
        }

        val httpModule = HttpModule.getOrCreate(loggingLevel.httpLoggingLevel, preferences)

        val module = VideoModule(
            appContext = context,
            preferences = preferences
        )

        val socket = module.socket()
        val userState = module.userState()

        val callCoordinatorClientModule = CallCoordinatorClientModule(
            user = user,
            preferences = preferences,
            appContext = context,
            lifecycle = lifecycle,
            okHttpClient = httpModule.okHttpClient
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
