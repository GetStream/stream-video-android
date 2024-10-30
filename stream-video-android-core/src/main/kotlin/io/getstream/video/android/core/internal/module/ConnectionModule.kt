/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.internal.module

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.appunite.mockwebserver_interceptor.TestInterceptor
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.socket.CoordinatorSocket
import io.getstream.video.android.core.socket.SfuSocket
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserToken
import kotlinx.coroutines.CoroutineScope
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.openapitools.client.apis.ProductvideoApi
import org.openapitools.client.infrastructure.Serializer
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.converter.wire.WireConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * ConnectionModule provides several helpful attributes
 *
 * preferences: token & user settings
 * oldService
 * eventsApi
 * defaultApi
 *
 * coordinatorSocket
 *
 * createSFUConnectionModule
 */
internal class ConnectionModule(
    context: Context,
    private val scope: CoroutineScope,
    internal val videoDomain: String,
    internal val connectionTimeoutInMs: Long,
    internal val loggingLevel: LoggingLevel = LoggingLevel(),
    private val user: User,
    internal val apiKey: ApiKey,
    internal val userToken: UserToken,
) {
    private val authInterceptor: CoordinatorAuthInterceptor by lazy {
        CoordinatorAuthInterceptor(apiKey, userToken)
    }
    private val headersInterceptor: HeadersInterceptor by lazy { HeadersInterceptor() }
    val okHttpClient: OkHttpClient by lazy { buildOkHttpClient() }
    val networkStateProvider: NetworkStateProvider by lazy {
        NetworkStateProvider(
            connectivityManager = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
        )
    }
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://$videoDomain")
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(Serializer.moshi))
            .client(okHttpClient)
            .build()
    }
    val api: ProductvideoApi by lazy { retrofit.create(ProductvideoApi::class.java) }
    val coordinatorSocket: CoordinatorSocket by lazy { createCoordinatorSocket() }

    val localApi: ProductvideoApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://c187-2a02-a46d-1c8b-1-b5c3-c938-b354-c7b0.ngrok-free.app")
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(Serializer.moshi))
            .client(okHttpClient)
            .build().create(ProductvideoApi::class.java)
    }

    /**
     * Key used to prove authorization to the API.
     */

    private fun buildOkHttpClient(): OkHttpClient {
        // create a new OkHTTP client and set timeouts
        return OkHttpClient.Builder()
            .addInterceptor(TestInterceptor)
            .addInterceptor(headersInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = loggingLevel.httpLoggingLevel.level
                },
            )
            .retryOnConnectionFailure(true)
            .connectTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .writeTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .readTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .callTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .build()
    }

    /**
     * Provider that handles connectivity and listens to state changes, exposing them to listeners.
     */

    /**
     * @return The WebSocket handler that is used to connect to different calls.
     */
    private fun createCoordinatorSocket(): CoordinatorSocket {
        val coordinatorUrl = "wss://$videoDomain/video/connect"

        return CoordinatorSocket(
            coordinatorUrl,
            user,
            userToken,
            scope,
            okHttpClient,
            networkStateProvider = networkStateProvider,
        )
    }

    internal fun createSFUConnectionModule(
        sfuUrl: String,
        sessionId: String,
        sfuToken: String,
        getSubscriberSdp: suspend () -> String,
        onFastReconnect: suspend () -> Unit,
    ): SfuConnectionModule {
        return SfuConnectionModule(
            sfuUrl = sfuUrl,
            sessionId = sessionId,
            sfuToken = sfuToken,
            apiKey = apiKey,
            getSubscriberSdp = getSubscriberSdp,
            scope = scope,
            networkStateProvider = networkStateProvider,
            loggingLevel = loggingLevel,
            onFastReconnect = onFastReconnect,
        )
    }

    fun updateToken(newToken: String) {
        // the coordinator socket also needs to update the token
        coordinatorSocket.token = newToken
        // update the auth token as well
        authInterceptor.token = newToken
    }

    fun updateAuthType(authType: String) {
        authInterceptor.authType = authType
    }
}

/**
 * Sets upt he signalService and socket for the SFU connection.
 */
internal class SfuConnectionModule(
    /** The url of the SFU */
    sfuUrl: String,
    /** the session id, generated when we join/ client side */
    sessionId: String,
    /** A token which gives you access to the sfu */
    private val sfuToken: String,
    /** A token which gives you access to the sfu */
    private val apiKey: String,
    /** Function that gives a fresh SDP */
    getSubscriberSdp: suspend () -> String,
    private val loggingLevel: LoggingLevel = LoggingLevel(),
    /** The scope to use for the socket */
    scope: CoroutineScope = CoroutineScope(DispatcherProvider.IO),
    /** Network monitoring */
    networkStateProvider: NetworkStateProvider,
    onFastReconnect: suspend () -> Unit,
) {
    internal var sfuSocket: SfuSocket
    private val updatedSignalUrl = if (sfuUrl.contains(Regex("https?://"))) {
        sfuUrl
    } else {
        "http://$sfuUrl"
    }.removeSuffix("/twirp")

    private fun buildSfuOkHttpClient(): OkHttpClient {
        val connectionTimeoutInMs = 10000L
        // create a new OkHTTP client and set timeouts
        val authInterceptor = CoordinatorAuthInterceptor(apiKey, sfuToken)
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = loggingLevel.httpLoggingLevel.level
                },
            )
            .retryOnConnectionFailure(true)
            .connectTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .writeTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .readTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .callTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .build()
    }

    val okHttpClient = buildSfuOkHttpClient()

    private val signalRetrofitClient: Retrofit by lazy {
        Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(WireConverterFactory.create())
            .baseUrl(updatedSignalUrl)
            .build()
    }

    internal val signalService: SignalServerService by lazy {
        signalRetrofitClient.create(SignalServerService::class.java)
    }

    init {
        val socketUrl = "$updatedSignalUrl/ws"
            .replace("https", "wss")
            .replace("http", "ws")

        sfuSocket = SfuSocket(
            socketUrl,
            sessionId,
            sfuToken,
            getSubscriberSdp,
            scope,
            okHttpClient,
            networkStateProvider,
            onFastReconnected = onFastReconnect,
        )
    }
}

/**
 * CoordinatorAuthInterceptor adds the token authentication to the API calls
 */
internal class CoordinatorAuthInterceptor(
    var apiKey: String,
    var token: String,
    var authType: String = "jwt",
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val updatedUrl = if (original.url.toString().contains("video")) {
            original.url.newBuilder()
                .addQueryParameter(API_KEY, apiKey)
                .build()
        } else {
            original.url
        }

        val updated = original.newBuilder()
            .url(updatedUrl)
            .addHeader(HEADER_AUTHORIZATION, token)
            .header(STREAM_AUTH_TYPE, authType)
            .build()

        return chain.proceed(updated)
    }

    private companion object {
        /**
         * Query key used to authenticate to the API.
         */
        private const val API_KEY = "api_key"
        private const val STREAM_AUTH_TYPE = "stream-auth-type"
        private const val HEADER_AUTHORIZATION = "Authorization"
    }
}

internal class HeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .addHeader("X-Stream-Client", StreamVideo.buildSdkTrackingHeaders())
            .build()
        return chain.proceed(request)
    }
}
