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
import io.getstream.video.android.core.api.ClientRPCService
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.call.signal.socket.SfuSocketFactory
import io.getstream.video.android.core.call.signal.socket.SfuSocketImpl
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.socket.SocketStateService
import io.getstream.video.android.core.socket.VideoSocket
import io.getstream.video.android.core.socket.internal.SocketFactory
import io.getstream.video.android.core.socket.internal.VideoSocketImpl
import io.getstream.video.android.core.user.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
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

internal class SFUConnectionModule(
    okHttpClient: OkHttpClient,
    SFUUrl: String,
    networkStateProvider: NetworkStateProvider
) {
    internal lateinit var sfuSocket: SfuSocketImpl
    val updatedSignalUrl = SFUUrl.removeSuffix(suffix = "/twirp")

    internal val signalRetrofitClient: Retrofit by lazy {
        Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(WireConverterFactory.create())
            .baseUrl(SFUUrl)
            .build()
    }

    internal val signalService: SignalServerService by lazy {
        signalRetrofitClient.create(SignalServerService::class.java)
    }

    init {
        val socketFactory = SfuSocketFactory(okHttpClient)
        sfuSocket = SfuSocketImpl(
            wssUrl = "$updatedSignalUrl/ws".replace("https", "wss"),
            networkStateProvider = networkStateProvider,
            coroutineScope = CoroutineScope(Dispatchers.IO),
            sfuSocketFactory = socketFactory
        )
    }
}

internal data class InterceptorWrapper(
    var baseUrl: HttpUrl?,
    val token: String
)

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
 * buildSFUSocket ()?
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
    internal var okHttpClient: OkHttpClient
    internal var oldService: ClientRPCService
    internal var videoCallsApi: VideoCallsApi
    internal var moderationApi: ModerationApi
    internal var recordingApi: RecordingApi
    internal var livestreamingApi: LivestreamingApi

    internal var eventsApi: EventsApi
    internal var coordinatorSocket: VideoSocket
    internal var networkStateProvider: NetworkStateProvider

    init {
        // setup the OKHttpClient
        val userToken = preferences.getUserToken()
        okHttpClient = buildOkHttpClient(
            preferences,
            interceptorWrapper = InterceptorWrapper(null, token = userToken)
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
        oldService = protoRetrofitClient.create(ClientRPCService::class.java)
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
    private val HEADER_AUTHORIZATION = "Authorization"

    /**
     * Query key used to authenticate to the API.
     */
    private val API_KEY = "api_key"
    private val STREAM_AUTH_TYPE = "stream-auth-type"

    private fun buildHostSelectionInterceptor(interceptorWrapper: InterceptorWrapper): Interceptor =
        Interceptor { chain ->
            interceptorWrapper.baseUrl =
                interceptorWrapper.baseUrl ?: return@Interceptor chain.proceed(chain.request())
            // TODO: Remove !!
            val host = interceptorWrapper.baseUrl?.host!!
            val original = chain.request()
            if (original.url.host == REPLACEMENT_HOST) {
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

    private fun buildCredentialsInterceptor(
        interceptorWrapper: InterceptorWrapper
    ): Interceptor = Interceptor {
        val original = it.request()

        val updatedUrl = if (original.url.toString().contains("video")) {
            original.url.newBuilder()
                .addQueryParameter(API_KEY, preferences.getApiKey())
                .build()
        } else {
            original.url
        }

        val updated = original.newBuilder()
            .url(updatedUrl)
            .addHeader(HEADER_AUTHORIZATION, interceptorWrapper.token)
            .header(STREAM_AUTH_TYPE, "jwt")
            .build()

        it.proceed(updated)
    }

    private fun buildOkHttpClient(
        preferences: UserPreferences,
        interceptorWrapper: InterceptorWrapper
    ): OkHttpClient {
        // create a new OkHTTP client and set timeouts
        // TODO: map logging level
        return OkHttpClient.Builder()
            .addInterceptor(
                buildCredentialsInterceptor(
                    interceptorWrapper = interceptorWrapper
                )
            )
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .addInterceptor(buildHostSelectionInterceptor(interceptorWrapper = interceptorWrapper))
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
    internal fun createCoordinatorSocket(): VideoSocket {
        val wssURL = "wss://$videoDomain/video/connect"

        return VideoSocketImpl(
            wssUrl = wssURL,
            preferences = preferences,
            user = user,
            socketFactory = SocketFactory(),
            networkStateProvider = networkStateProvider,
            coroutineScope = scope
        )
    }

    internal val coordinatorStateService: SocketStateService by lazy {
        SocketStateService()
    }

    internal fun createSFUConnectionModule(SFUUrl: String, SFUToken: String): SFUConnectionModule {
        val updatedSignalUrl = SFUUrl.removeSuffix(suffix = "/twirp")
        val wrapper = InterceptorWrapper(baseUrl = updatedSignalUrl.toHttpUrl(), SFUToken)
        val okHttpClient = buildOkHttpClient(preferences, interceptorWrapper = wrapper)

        return SFUConnectionModule(okHttpClient, SFUUrl, networkStateProvider)
    }
}
