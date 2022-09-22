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

package io.getstream.video.android.module

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.Lifecycle
import io.getstream.video.android.api.CallCoordinatorService
import io.getstream.video.android.client.coordinator.CallCoordinatorClient
import io.getstream.video.android.client.coordinator.CallCoordinatorClientImpl
import io.getstream.video.android.client.user.UserState
import io.getstream.video.android.dispatchers.DispatcherProvider
import io.getstream.video.android.network.NetworkStateProvider
import io.getstream.video.android.socket.SocketFactory
import io.getstream.video.android.socket.VideoSocket
import io.getstream.video.android.socket.VideoSocketImpl
import io.getstream.video.android.token.CredentialsManager
import io.getstream.video.android.token.CredentialsManagerImpl
import io.getstream.video.android.token.CredentialsProvider
import kotlinx.coroutines.CoroutineScope
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.wire.WireConverterFactory
import stream.video.sfu.User

/**
 * Serves as an internal DI framework that allows us to cache heavy components reused across the
 * SDK.
 *
 * @property user The currently logged in user.
 * @property credentialsProvider Provider of user-tokens.
 * @property appContext The context of the app, used for Android-based dependencies.
 * @property lifecycle The lifecycle of the process.
 */
internal class VideoModule(
    private val user: User,
    private val credentialsProvider: CredentialsProvider,
    private val appContext: Context,
    private val lifecycle: Lifecycle,
    private val okHttpClient: OkHttpClient
) {

    /**
     * Cached instance of the Retrofit client that builds API services.
     */
    private val retrofitClient: Retrofit by lazy {
        Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(WireConverterFactory.create())
            .baseUrl(REDIRECT_BASE_URL ?: BASE_URL)
            .build()
    }

    /**
     * Cached instance of the CallCoordinator service client for API calls.
     */
    private val callCoordinatorClient: CallCoordinatorClient by lazy {
        val service = retrofitClient.create(CallCoordinatorService::class.java)

        CallCoordinatorClientImpl(service, credentialsProvider)
    }

    /**
     * The [CoroutineScope] used for all business logic related operations.
     */
    private val scope = CoroutineScope(DispatcherProvider.IO)

    /**
     * User state that provides the information about the current user.
     */
    private val userState: UserState by lazy {
        UserState().apply {
            setUser(this@VideoModule.user)
        }
    }

    /**
     * Cached user token manager.
     */
    private val credentialsManager: CredentialsManager by lazy {
        CredentialsManagerImpl().apply {
            setCredentialsProvider(credentialsProvider)
        }
    }

    /**
     * Factory for providing sockets based on the connected user.
     */
    private val socketFactory: SocketFactory by lazy {
        SocketFactory()
    }

    /**
     * Provider that handles connectivity and listens to state changes, exposing them to listeners.
     */
    private val networkStateProvider: NetworkStateProvider by lazy {
        NetworkStateProvider(
            connectivityManager = appContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        )
    }

    // TODO - build notification handler/provider

    /**
     * Builds the HTTP interceptor that adds headers to all API calls.
     *
     * @param credentialsProvider Provider of the user token and API key.
     *
     * @return [Interceptor] which adds headers.
     */

    /**
     * Public providers used to set up other components.
     */

    /**
     * @return [CoroutineScope] used for all API requests.
     */
    internal fun scope(): CoroutineScope {
        return scope
    }

    /**
     * @return The [CallCoordinatorClient] used to communicate to the API.
     */
    internal fun callClient(): CallCoordinatorClient {
        return callCoordinatorClient
    }

    /**
     * @return The WebSocket handler that is used to connect to different calls.
     */
    internal fun socket(): VideoSocket {
        return VideoSocketImpl(
            wssUrl = REDIRECT_WS_BASE_URL ?: WS_BASE_URL,
            credentialsManager = credentialsManager,
            socketFactory = socketFactory,
            networkStateProvider = networkStateProvider,
            userState = userState,
            coroutineScope = scope
        )
    }

    /**
     * @return The [UserState] that serves us information about the currently logged in user.
     */
    internal fun userState(): UserState {
        return userState
    }

    internal companion object {
        /**
         * Used for testing on devices and redirecting from a public realm to localhost.
         *
         * Will only be used if the value is non-null, so if you're able to test locally, just
         * leave it as-is.
         */
        @Suppress("RedundantNullableReturnType")
        private val REDIRECT_BASE_URL: String? =
            "https://38d2-89-172-235-70.eu.ngrok.io" // e.g. "https://dc54-83-131-252-51.eu.ngrok.io"

        /**
         * The base URL of the API.
         */
        private const val BASE_URL = "http://10.0.2.2:26991"

        /**
         * Used for testing on devices and redirecting from a public realm to localhost.
         *
         * Will only be used if the value is non-null, so if you're able to test locally, just
         * leave it as-is.
         */
        @Suppress("RedundantNullableReturnType")
        internal val REDIRECT_PING_URL: String? =
            "https://ecef-89-172-235-70.eu.ngrok.io/ping" // "<redirect-url>/ping"

        /**
         * Used for testing on devices and redirecting from a public realm to localhost.
         *
         * Will only be used if the value is non-null, so if you're able to test locally, just
         * leave it as-is.
         */
        @Suppress("RedundantNullableReturnType")
        private val REDIRECT_WS_BASE_URL: String? =
            "ws://4.tcp.eu.ngrok.io:12921" // e.g. "ws://4.tcp.eu.ngrok.io:12265"
        private const val WS_BASE_URL = "ws://localhost:8989/"
    }
}
