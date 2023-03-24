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

package io.getstream.video.android.core.call.builder

import android.content.Context
import io.getstream.video.android.core.call.CallClient
import io.getstream.video.android.core.call.CallClientImpl
import io.getstream.video.android.core.call.signal.socket.SfuSocketFactory
import io.getstream.video.android.core.call.signal.socket.SfuSocketImpl
import io.getstream.video.android.core.coordinator.CallCoordinatorClient
import io.getstream.video.android.core.engine.StreamCallEngine
import io.getstream.video.android.core.internal.module.HttpModule
import io.getstream.video.android.core.internal.module.SfuClientModule
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.IceServer
import io.getstream.video.android.core.model.StreamCallGuid
import io.getstream.video.android.core.user.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.logging.HttpLoggingInterceptor

/**
 * @param context Used to set up internal factories that depend on Android.
 * @param coordinatorClient The client used to communicate to the Video Call API.
 * @param preferences Used to propagate logged in user's credentials.
 * @param networkStateProvider Listens to events of the network state, used for socket connections.
 * @param callEngine Provides the state of active calls.
 * @param signalUrl The URL used to connect to a call.
 * @param iceServers Servers used to authenticate and connect to the call and its tracks.
 * @param callGuid The GUID of the Call, containing the ID and its type.
 */
internal class CallClientBuilder(
    private val context: Context,
    private val coordinatorClient: CallCoordinatorClient,
    private val preferences: UserPreferences,
    private val networkStateProvider: NetworkStateProvider,
    private val callEngine: StreamCallEngine,
    private val signalUrl: String,
    private val iceServers: List<IceServer>,
    private val callGuid: StreamCallGuid,
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
        val user = preferences.getUserCredentials()
        if (preferences.getApiKey().isBlank() ||
            user?.id.isNullOrBlank() ||
            preferences.getSfuToken().isBlank()
        ) throw IllegalArgumentException("The API key, user ID and token cannot be empty!")

        val updatedSignalUrl = signalUrl.removeSuffix(suffix = "/twirp")
        val httpModule = HttpModule.getOrCreate(
            loggingLevel = loggingLevel,
            credentialsProvider = preferences
        ).apply {
            this.baseUrl = updatedSignalUrl.toHttpUrl()
        }

        val sfuClientModule = SfuClientModule.getOrCreate(
            okHttpClient = httpModule.okHttpClient,
            signalUrl = HttpModule.REPLACEMENT_URL
        )

        val socketFactory = SfuSocketFactory(httpModule.okHttpClient)

        return CallClientImpl(
            context = context,
            coordinatorClient = coordinatorClient,
            callGuid = callGuid,
            getCurrentUserId = { preferences.getUserCredentials()?.id ?: "" },
            getSfuToken = { preferences.getSfuToken() },
            callEngine = callEngine,
            sfuClient = sfuClientModule.sfuClient,
            remoteIceServers = iceServers,
            sfuSocket = SfuSocketImpl(
                wssUrl = "$updatedSignalUrl/ws".replace("https", "wss"),
                networkStateProvider = networkStateProvider,
                coroutineScope = CoroutineScope(Dispatchers.IO),
                sfuSocketFactory = socketFactory
            ),
        )
    }
}
