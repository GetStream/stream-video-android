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

package io.getstream.video.android.call.builder

import android.content.Context
import io.getstream.video.android.call.CallClient
import io.getstream.video.android.call.CallClientImpl
import io.getstream.video.android.call.signal.socket.SignalSocketFactory
import io.getstream.video.android.call.signal.socket.SignalSocketImpl
import io.getstream.video.android.engine.StreamCallEngine
import io.getstream.video.android.engine.StreamCallEngineImpl
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.model.IceServer
import io.getstream.video.android.module.CallClientModule
import io.getstream.video.android.module.HttpModule
import io.getstream.video.android.network.NetworkStateProvider
import io.getstream.video.android.token.CredentialsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.logging.HttpLoggingInterceptor

internal class CallClientBuilder(
    private val context: Context,
    private val credentialsProvider: CredentialsProvider,
    private val networkStateProvider: NetworkStateProvider,
    private val callEngine: StreamCallEngine,
    private val signalUrl: String,
    private val iceServers: List<IceServer>,
) {
    private var loggingLevel: HttpLoggingInterceptor.Level =
        HttpLoggingInterceptor.Level.NONE

    /**
     * Sets the Logging level for all API calls.
     *
     * @param loggingLevel The level of information to log.
     */
    fun loggingLevel(loggingLevel: LoggingLevel): CallClientBuilder {
        this.loggingLevel = loggingLevel.httpLoggingLevel

        return this
    }

    /**
     * Builds the [CallClient] and its respective dependencies, used to set up all the business
     * logic of the SDK.
     */
    fun build(): CallClient {
        val user = credentialsProvider.getUserCredentials()
        if (credentialsProvider.loadApiKey().isBlank() ||
            user.id.isBlank() ||
            credentialsProvider.getSfuToken().isBlank()
        ) throw IllegalArgumentException("The API key, user ID and token cannot be empty!")

        val httpModule = HttpModule.getOrCreate(
            loggingLevel = loggingLevel,
            credentialsProvider = credentialsProvider
        )

        val callClientModule = CallClientModule(
            okHttpClient = httpModule.okHttpClient,
            signalUrl = signalUrl
        )

        val socketFactory = SignalSocketFactory()

        return CallClientImpl(
            context = context,
            credentialsProvider = credentialsProvider,
            callEngine = callEngine,
            signalClient = callClientModule.signalClient,
            remoteIceServers = iceServers,
            signalSocket = SignalSocketImpl(
                wssUrl = "$signalUrl/ws".replace("https", "wss"),
                networkStateProvider = networkStateProvider,
                coroutineScope = CoroutineScope(Dispatchers.IO),
                signalSocketFactory = socketFactory
            )
        )
    }
}
