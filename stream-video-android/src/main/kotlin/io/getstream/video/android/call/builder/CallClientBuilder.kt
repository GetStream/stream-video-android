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
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.model.IceServer
import io.getstream.video.android.module.CallClientModule
import io.getstream.video.android.module.HttpModule
import io.getstream.video.android.network.NetworkStateProvider
import io.getstream.video.android.token.CredentialsProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.logging.HttpLoggingInterceptor

/**
 * @param context Used to set up internal factories that depend on Android.
 * @param credentialsProvider Used to propagate logged in user's credentials.
 * @param networkStateProvider Listens to events of the network state, used for socket connections.
 * @param callEngine Provides the state of active calls.
 * @param signalUrl The URL used to connect to a call.
 * @param iceServers Servers used to authenticate and connect to the call and its tracks.
 */
internal class CallClientBuilder(
    private val context: Context,
    private val credentialsProvider: CredentialsProvider,
    private val networkStateProvider: NetworkStateProvider,
    private val callEngine: StreamCallEngine,
    private val signalUrl: String,
    private val iceServers: List<IceServer>,
) {

    /**
     * Represents the level of logs we'll have for all the HTTP communication.
     */
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
     *
     * @return [CallClient] used for this particular call.
     */
    fun build(): CallClient {
        val user = credentialsProvider.getUserCredentials()
        if (credentialsProvider.loadApiKey().isBlank() ||
            user.id.isBlank() ||
            credentialsProvider.getSfuToken().isBlank()
        ) throw IllegalArgumentException("The API key, user ID and token cannot be empty!")

        val updatedSignalUrl = signalUrl.removeSuffix(suffix = "/twirp")
        val httpModule = HttpModule.getOrCreate(
            loggingLevel = loggingLevel,
            credentialsProvider = credentialsProvider
        ).apply {
            this.baseUrl = updatedSignalUrl
        }

        val callClientModule = CallClientModule.getOrCreate(
            okHttpClient = httpModule.okHttpClient,
            signalUrl = HttpModule.REPLACEMENT_URL
        )

        val socketFactory = SignalSocketFactory(httpModule.okHttpClient)

        return CallClientImpl(
            context = context,
            getCurrentUserId = { credentialsProvider.getUserCredentials().id },
            getSfuToken = { credentialsProvider.getSfuToken() },
            callEngine = callEngine,
            signalClient = callClientModule.signalClient,
            remoteIceServers = iceServers,
            signalSocket = SignalSocketImpl(
                wssUrl = "$updatedSignalUrl/ws".replace("https", "wss"),
                networkStateProvider = networkStateProvider,
                coroutineScope = CoroutineScope(Dispatchers.IO),
                signalSocketFactory = socketFactory
            )
        )
    }
}
