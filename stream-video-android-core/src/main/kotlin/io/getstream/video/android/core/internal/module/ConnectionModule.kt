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

package io.getstream.video.android.core.internal.module

import android.content.Context
import android.net.ConnectivityManager
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.socket.CoordinatorSocket
import io.getstream.video.android.core.socket.SfuSocket
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserToken
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.openapitools.client.apis.DefaultApi
import org.openapitools.client.apis.EventsApi
import org.openapitools.client.apis.LivestreamingApi
import org.openapitools.client.apis.ModerationApi
import org.openapitools.client.apis.RecordingApi
import org.openapitools.client.apis.VideoCallsApi
import org.openapitools.client.infrastructure.Serializer
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.converter.wire.WireConverterFactory

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
    internal val loggingLevel: LoggingLevel = LoggingLevel.NONE,
    private val user: User,
    internal val apiKey: ApiKey,
    internal val userToken: UserToken,
) {
    private val baseUrlInterceptor: BaseUrlInterceptor = BaseUrlInterceptor(null)
    private val authInterceptor: CoordinatorAuthInterceptor =
        CoordinatorAuthInterceptor(apiKey, userToken)
    internal val okHttpClient: OkHttpClient = buildOkHttpClient()

    internal var videoCallsApi: VideoCallsApi
    internal var moderationApi: ModerationApi
    internal var recordingApi: RecordingApi
    internal var livestreamingApi: LivestreamingApi
    internal var defaultApi: DefaultApi
    internal var eventsApi: EventsApi

    internal var coordinatorSocket: CoordinatorSocket
    internal var networkStateProvider: NetworkStateProvider = NetworkStateProvider(
        connectivityManager = context
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    )

    init {
        // setup the retrofit clients
        val baseUrl = "https://$videoDomain"
        val retrofitClient = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(Serializer.moshi))
            .client(okHttpClient)
            .build()

        // setup the retrofit services
        videoCallsApi = retrofitClient.create(VideoCallsApi::class.java)
        eventsApi = retrofitClient.create(EventsApi::class.java)
        moderationApi = retrofitClient.create(ModerationApi::class.java)
        recordingApi = retrofitClient.create(RecordingApi::class.java)
        livestreamingApi = retrofitClient.create(LivestreamingApi::class.java)
        defaultApi = retrofitClient.create(DefaultApi::class.java)

        // Note that it doesn't connect when you create the socket
        coordinatorSocket = createCoordinatorSocket()
    }

    /**
     * Key used to prove authorization to the API.
     */

    private fun buildOkHttpClient(): OkHttpClient {
        // create a new OkHTTP client and set timeouts
        return OkHttpClient.Builder()
            .addInterceptor(
                authInterceptor
            )
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = loggingLevel.httpLoggingLevel
                }
            )
            .addInterceptor(
                baseUrlInterceptor
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
            networkStateProvider = networkStateProvider
        )
    }

    internal fun createSFUConnectionModule(
        sfuUrl: String,
        sessionId: String,
        sfuToken: String,
        getSubscriberSdp: suspend () -> String,
    ): SfuConnectionModule {
//        val updatedSignalUrl = sfuUrl.removeSuffix(suffix = "/twirp")
//        val baseUrl = updatedSignalUrl.toHttpUrl()
//        val okHttpClient = buildOkHttpClient(baseUrl)
        return SfuConnectionModule(
            sfuUrl = sfuUrl,
            sessionId = sessionId,
            sfuToken = sfuToken,
            apiKey = apiKey,
            getSubscriberSdp = getSubscriberSdp,
            scope = scope,
            networkStateProvider = networkStateProvider,
            loggingLevel = loggingLevel
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

    internal companion object {
        /**
         * Host pattern to be replaced.
         */
        private const val REPLACEMENT_HOST = "replacement.url"

        /**
         * Url pattern to be replaced.
         */
        internal const val REPLACEMENT_URL = "https://$REPLACEMENT_HOST"
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
    val apiKey: String,
    /** Function that gives a fresh SDP */
    getSubscriberSdp: suspend () -> String,
    private val loggingLevel: LoggingLevel = LoggingLevel.NONE,
    /** The scope to use for the socket */
    scope: CoroutineScope = CoroutineScope(DispatcherProvider.IO),
    /** Network monitoring */
    networkStateProvider: NetworkStateProvider
) {
    internal var sfuSocket: SfuSocket
    private val updatedSignalUrl = sfuUrl.removeSuffix(suffix = "/twirp")

    private fun buildSfuOkHttpClient(): OkHttpClient {
        val connectionTimeoutInMs = 10000L
        // create a new OkHTTP client and set timeouts
        val authInterceptor =
            CoordinatorAuthInterceptor(apiKey, sfuToken)
        val baseUrlInterceptor = BaseUrlInterceptor(null)
        return OkHttpClient.Builder()
            .addInterceptor(
                authInterceptor
            )
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = loggingLevel.httpLoggingLevel
                }
            )
            .addInterceptor(
                baseUrlInterceptor
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
        val socketUrl = "$updatedSignalUrl/ws".replace("https", "wss")
        sfuSocket = SfuSocket(
            socketUrl,
            sessionId,
            sfuToken,
            getSubscriberSdp,
            scope,
            okHttpClient,
            networkStateProvider
        )
    }
}

/**
 * Interceptor that changes urls for the coordinator
 */
internal class BaseUrlInterceptor(private var baseUrl: HttpUrl?) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        baseUrl = baseUrl ?: return chain.proceed(chain.request())
        val host = baseUrl?.host!!
        val original = chain.request()
        return if (original.url.host == REPLACEMENT_HOST) {
            val updatedBaseUrl = original.url.newBuilder()
                .host(host)
                .build()
            val updated = original.newBuilder()
                .url(updatedBaseUrl)
                .build()
            chain.proceed(updated)
        } else {
            chain.proceed(chain.request())
        }
    }

    private companion object {
        private const val REPLACEMENT_HOST = "replacement.url"
    }
}

/**
 * CoordinatorAuthInterceptor adds the token authentication to the API calls
 */
internal class CoordinatorAuthInterceptor(
    var apiKey: String,
    var token: String,
    var authType: String = "jwt"
) : Interceptor {

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
        private const val REPLACEMENT_HOST = "replacement.url"

        /**
         * Query key used to authenticate to the API.
         */
        private const val API_KEY = "api_key"
        private const val STREAM_AUTH_TYPE = "stream-auth-type"
        private const val HEADER_AUTHORIZATION = "Authorization"
    }
}
