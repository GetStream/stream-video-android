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
import io.getstream.video.android.client.CallClient
import io.getstream.video.android.dispatchers.DispatcherProvider
import io.getstream.video.android.input.CallServiceInput
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.module.CallsModule
import io.getstream.video.android.token.CredentialsProvider
import kotlinx.coroutines.CoroutineScope

public class StreamCallsBuilder(
    private val context: Context,
    private val credentialsProvider: CredentialsProvider,
    private val config: StreamCallsConfig = StreamCallsConfigDefault,
    private val serviceInput: CallServiceInput? = null,
    private val loggingLevel: LoggingLevel = LoggingLevel.NONE
) {

    public fun build(): StreamCalls {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle

        val module = CallsModule(
            appContext = context,
            credentialsProvider = credentialsProvider
        )

        val socket = module.socket()
        val userState = module.userState()

        val callClient = CallClient.Builder(
            appContext = context,
            credentialsProvider = credentialsProvider,
            loggingLevel = loggingLevel,
            userState = userState,
            socket = socket,
            lifecycle = lifecycle
        )

        val scope = CoroutineScope(DispatcherProvider.IO)
        return StreamCallsImpl(
            context = context,
            scope = scope,
            loggingLevel = loggingLevel,
            callClient = callClient.build(),
            credentialsProvider = credentialsProvider,
            lifecycle = lifecycle,
            socket = socket,
            socketStateService = module.socketStateService(),
            userState = userState,
            networkStateProvider = module.networkStateProvider()
        ).also { streamCalls ->
            StreamCallsLauncher(context, streamCalls, config, serviceInput).run(scope)
        }
    }
}
