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
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.socket.CoordinatorSocket
import io.getstream.video.android.core.socket.SfuSocket
import io.getstream.video.android.core.user.UserPreferences
import kotlinx.coroutines.CoroutineScope
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
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
    private val context: Context,
    private val scope: CoroutineScope,
    internal val videoDomain: String,
    internal val preferences: UserPreferences,
    internal val connectionTimeoutInMs: Long,
    internal val loggingLevel: LoggingLevel = LoggingLevel.NONE,
    private val user: User,
) {
    private var baseUrlInterceptor: BaseUrlInterceptor
    private var authInterceptor: CoordinatorAuthInterceptor
    internal var okHttpClient: OkHttpClient
    internal var videoCallsApi: VideoCallsApi
    internal var moderationApi: ModerationApi
    internal var recordingApi: RecordingApi
    internal var livestreamingApi: LivestreamingApi

    internal var eventsApi: EventsApi
    internal var coordinatorSocket: CoordinatorSocket
    internal var networkStateProvider: NetworkStateProvider

    init {
        // setup the OKHttpClient
        authInterceptor =
            CoordinatorAuthInterceptor(preferences.getApiKey(), preferences.getUserToken())
        baseUrlInterceptor = BaseUrlInterceptor(null)

        okHttpClient = buildOkHttpClient(
            preferences,
            null
        )

        networkStateProvider = NetworkStateProvider(
            connectivityManager = context
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        )

        // setup the retrofit clients
        val baseUrl = "https://$videoDomain"
        val protoRetrofitClient = Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(WireConverterFactory.create())
            .baseUrl(baseUrl)
            .build()

        val retrofitClient = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(Serializer.moshi))
            .client(okHttpClient)
            .build()

        // setup the 4 retrofit APIs
        videoCallsApi = retrofitClient.create(VideoCallsApi::class.java)
        eventsApi = retrofitClient.create(EventsApi::class.java)
        moderationApi = retrofitClient.create(ModerationApi::class.java)
        recordingApi = retrofitClient.create(RecordingApi::class.java)
        livestreamingApi = retrofitClient.create(LivestreamingApi::class.java)

        // Note that it doesn't connect when you create the socket
        coordinatorSocket = createCoordinatorSocket()
    }

    /**
     * Host pattern to be replaced.
     */
    private val REPLACEMENT_HOST = "replacement.url"

    /**
     * Url pattern to be replaced.
     */
    internal val REPLACEMENT_URL = "https://$REPLACEMENT_HOST"

    /**
     * Key used to prove authorization to the API.
     */

    private fun buildOkHttpClient(
        preferences: UserPreferences,
        baseUrl: HttpUrl?
    ): OkHttpClient {
        // create a new OkHTTP client and set timeouts
        // TODO: map logging level
        return OkHttpClient.Builder()
            .addInterceptor(
                authInterceptor
            )
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
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
    internal fun createCoordinatorSocket(): CoordinatorSocket {
        val coordinatorUrl = "wss://$videoDomain/video/connect"

        val token = preferences.getUserToken()

        return CoordinatorSocket(
            coordinatorUrl,
            user,
            token,
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
        val updatedSignalUrl = sfuUrl.removeSuffix(suffix = "/twirp")
        val baseUrl = updatedSignalUrl.toHttpUrl()
        val okHttpClient = buildOkHttpClient(preferences, baseUrl)

        return SfuConnectionModule(
            sfuUrl,
            sessionId,
            sfuToken,
            getSubscriberSdp,
            scope,
            okHttpClient,
            networkStateProvider
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
    sfuToken: String,
    /** Function that gives a fresh SDP */
    getSubscriberSdp: suspend () -> String,
    /** The scope to use for the socket */
    scope: CoroutineScope = CoroutineScope(DispatcherProvider.IO),
    /** Inject your ok HttpClient */
    okHttpClient: OkHttpClient,
    /** Network monitoring */
    networkStateProvider: NetworkStateProvider
) {
    internal lateinit var sfuSocket: SfuSocket
    val updatedSignalUrl = sfuUrl.removeSuffix(suffix = "/twirp")

    internal val signalRetrofitClient: Retrofit by lazy {
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
internal class BaseUrlInterceptor(var baseUrl: HttpUrl?) : Interceptor {
    private val REPLACEMENT_HOST = "replacement.url"

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
}

/**
 * CoordinatorAuthInterceptor adds the token authentication to the API calls
 */
internal class CoordinatorAuthInterceptor(
    var apiKey: String,
    var token: String,
    var authType: String = "jwt"
) : Interceptor {
    private val REPLACEMENT_HOST = "replacement.url"

    /**
     * Query key used to authenticate to the API.
     */
    private val API_KEY = "api_key"
    private val STREAM_AUTH_TYPE = "stream-auth-type"
    private val HEADER_AUTHORIZATION = "Authorization"

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
}
