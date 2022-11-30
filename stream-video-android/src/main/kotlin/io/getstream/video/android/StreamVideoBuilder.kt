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

package io.getstream.video.android

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import io.getstream.android.push.PushDeviceGenerator
import io.getstream.video.android.dispatchers.DispatcherProvider
import io.getstream.video.android.engine.StreamCallEngine
import io.getstream.video.android.engine.StreamCallEngineImpl
import io.getstream.video.android.input.CallAndroidInput
import io.getstream.video.android.input.CallAndroidInputLauncher
import io.getstream.video.android.input.DefaultCallAndroidInputLauncher
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.module.CallCoordinatorClientModule
import io.getstream.video.android.module.HttpModule
import io.getstream.video.android.module.VideoModule
import io.getstream.video.android.token.CredentialsProvider
import io.getstream.video.android.user.UserCredentialsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

public class StreamVideoBuilder(
    private val context: Context,
    private val credentialsProvider: CredentialsProvider,
    private val config: StreamVideoConfig = StreamVideoConfigDefault,
    private val androidInputs: Set<CallAndroidInput> = emptySet(),
    private val inputLauncher: CallAndroidInputLauncher = DefaultCallAndroidInputLauncher,
    private val loggingLevel: LoggingLevel = LoggingLevel.NONE,
    private inline val callEngineBuilder: ((CoroutineScope) -> StreamCallEngine)? = null,
    private val pushDeviceGenerators: List<PushDeviceGenerator> = emptyList(),
) {

    public fun build(): StreamVideo {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val user = credentialsProvider.getUserCredentials()

        if (credentialsProvider.loadApiKey().isBlank() ||
            user.id.isBlank() ||
            user.token.isBlank()
        ) throw IllegalArgumentException("The API key, user ID and token cannot be empty!")

        UserCredentialsManager.initialize(context).apply {
            storeUserCredentials(user)
        }

        val httpModule = HttpModule.getOrCreate(loggingLevel.httpLoggingLevel, credentialsProvider)

        val module = VideoModule(
            appContext = context,
            credentialsProvider = credentialsProvider
        )

        val socket = module.socket()
        val userState = module.userState()

        val callCoordinatorClientModule = CallCoordinatorClientModule(
            user = user,
            credentialsProvider = credentialsProvider,
            appContext = context,
            lifecycle = lifecycle,
            okHttpClient = httpModule.okHttpClient
        )

        val scope = CoroutineScope(DispatcherProvider.IO)

        val engine: StreamCallEngine =
            callEngineBuilder?.invoke(scope) ?: StreamCallEngineImpl(scope, config) {
                credentialsProvider.getUserCredentials().id
            }

        return StreamVideoImpl(
            context = context,
            scope = scope,
            config = config,
            engine = engine,
            loggingLevel = loggingLevel,
            callCoordinatorClient = callCoordinatorClientModule.callCoordinatorClient(),
            credentialsProvider = credentialsProvider,
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
