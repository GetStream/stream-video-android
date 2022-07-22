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
import io.getstream.video.android.parser.VideoParser
import io.getstream.video.android.socket.SocketFactory
import io.getstream.video.android.socket.VideoSocket
import io.getstream.video.android.socket.VideoSocketImpl
import io.getstream.video.android.token.TokenManager
import io.getstream.video.android.token.TokenManagerImpl
import io.getstream.video.android.token.TokenProvider
import kotlinx.coroutines.CoroutineScope
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.wire.WireConverterFactory
import stream.video.User

/**
 * Serves as an internal DI framework that allows us to cache heavy components reused across the
 * SDK.
 *
 * @property apiKey The key used to authenticate user apps with the API.
 * @property user The currently logged in user.
 * @property tokenProvider Provider of user-tokens.
 * @property appContext The context of the app, used for Android-based dependencies.
 * @property lifecycle The lifecycle of the process.
 * @property loggingLevel Log level used for all HTTP requests towards the API.
 */
internal class VideoModule(
    private val apiKey: String,
    private val user: User,
    private val tokenProvider: TokenProvider,
    private val appContext: Context,
    private val lifecycle: Lifecycle,
    private val loggingLevel: HttpLoggingInterceptor.Level
) {
    /**
     * Cached instance of the HTTP client.
     */
    private val okHttpClient: OkHttpClient by lazy {
        buildOkHttpClient(tokenProvider)
    }

    /**
     * Cached instance of the Retrofit client that builds API services.
     */
    private val retrofitClient: Retrofit by lazy {
        Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(WireConverterFactory.create().apply {})
            .baseUrl(BASE_URL)
            .build()
    }

    /**
     * Cached instance of the CallCoordinator service client for API calls.
     */
    private val callCoordinatorClient: CallCoordinatorClient by lazy {
        val service = retrofitClient.create(CallCoordinatorService::class.java)

        CallCoordinatorClientImpl(service)
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
    private val tokenManager: TokenManager by lazy {
        TokenManagerImpl().apply {
            setTokenProvider(tokenProvider)
        }
    }

    private val videoParser: VideoParser by lazy { VideoParser() }

    /**
     * Factory for providing sockets based on the connected user.
     */
    private val socketFactory: SocketFactory by lazy {
        SocketFactory(parser = videoParser)
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
     * Builds the [OkHttpClient] used for all API calls.
     *
     * @param tokenProvider The user-token provider used to attach authorization headers.
     * @return [OkHttpClient] that allows us API calls.
     */
    private fun buildOkHttpClient(tokenProvider: TokenProvider): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(
                buildInterceptor(
                    apiKey = apiKey,
                    tokenProvider = tokenProvider
                )
            )
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = loggingLevel
                }
            )
            .build()
    }

    /**
     * Builds the HTTP interceptor that adds headers to all API calls.
     *
     * @param apiKey The API key of the app.
     * @param tokenProvider Provider of the user token.
     *
     * @return [Interceptor] which adds headers.
     */
    private fun buildInterceptor(
        apiKey: String,
        tokenProvider: TokenProvider
    ): Interceptor = Interceptor {
        val original = it.request()
        val updated = original.newBuilder()
            .addHeader(HEADER_AUTHORIZATION, tokenProvider.getCachedToken())
            // TODO - add API key to auth or use to authenticate the user?
            .build()

        it.proceed(updated)
    }

    /**
     * Public providers used to set up other components.
     */

    /**
     * @return [CoroutineScope] used for all API requests.
     */
    public fun scope(): CoroutineScope {
        return scope
    }

    /**
     * @return The [CallCoordinatorClient] used to communicate to the API.
     */
    public fun callClient(): CallCoordinatorClient {
        return callCoordinatorClient
    }

    /**
     * @return The WebSocket handler that is used to connect to different calls.
     */
    public fun socket(): VideoSocket {
        return VideoSocketImpl(
            apiKey = apiKey,
            wssUrl = "ws://localhost:8989/",
            tokenManager = tokenManager,
            socketFactory = socketFactory,
            networkStateProvider = networkStateProvider,
            parser = videoParser,
            userState = userState,
            coroutineScope = scope
        )
    }

    /**
     * @return The [UserState] that serves us information about the currently logged in user.
     */
    public fun userState(): UserState {
        return userState
    }

    private companion object {
        /**
         * Key used to prove authorization to the API.
         */
        private const val HEADER_AUTHORIZATION = "authorization"

        /**
         * The base URL of the API.
         */
        private const val BASE_URL = "http://10.0.2.2:26991"
    }
}
