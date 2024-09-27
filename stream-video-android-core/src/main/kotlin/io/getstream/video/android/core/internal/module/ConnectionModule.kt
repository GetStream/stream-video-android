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
import androidx.lifecycle.Lifecycle
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.socket.common.token.CacheableTokenProvider
import io.getstream.video.android.core.socket.common.token.ConstantTokenProvider
import io.getstream.video.android.core.socket.common.token.TokenProvider
import io.getstream.video.android.core.socket.coordinator.CoordinatorSocketConnection
import io.getstream.video.android.core.socket.sfu.SfuSocket
import io.getstream.video.android.core.socket.sfu.SfuSocketConnection
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserToken
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.openapitools.client.apis.ProductvideoApi
import org.openapitools.client.infrastructure.Serializer
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.converter.wire.WireConverterFactory
import stream.video.sfu.models.WebsocketReconnectStrategy
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
    tokenProvider: TokenProvider,
    user: User,
    private val scope: CoroutineScope,
    internal val videoDomain: String,
    internal val connectionTimeoutInMs: Long,
    internal val loggingLevel: LoggingLevel = LoggingLevel(),
    internal val apiKey: ApiKey,
    internal val userToken: UserToken,
    internal val lifecycle: Lifecycle,
) {
    // Internals
    private val authInterceptor = CoordinatorAuthInterceptor(apiKey, userToken)
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder().baseUrl("https://$videoDomain")
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(Serializer.moshi))
            .client(okHttpClient).build()
    }
    // API
    /**
     * The OkHttpClient used for all network requests
     */
    val okHttpClient: OkHttpClient = OkHttpClient.Builder().addInterceptor(HeadersInterceptor())
        .addInterceptor(authInterceptor).addInterceptor(
            HttpLoggingInterceptor().apply {
                level = loggingLevel.httpLoggingLevel.level
            },
        ).retryOnConnectionFailure(true)
        .connectTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
        .writeTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
        .readTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
        .callTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS).build()

    val networkStateProvider: NetworkStateProvider by lazy {
        NetworkStateProvider(
            scope,
            connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
        )
    }
    val api: ProductvideoApi by lazy { retrofit.create(ProductvideoApi::class.java) }
    val coordinatorSocketConnection: CoordinatorSocketConnection = CoordinatorSocketConnection(
        apiKey = apiKey,
        url = "wss://$videoDomain/video/connect",
        user = user,
        token = userToken,
        httpClient = okHttpClient,
        networkStateProvider = networkStateProvider,
        scope = scope,
        lifecycle = lifecycle,
        tokenProvider = tokenProvider,
    )

    internal fun createSFUConnectionModule(
        sfuUrl: String,
        sessionId: String,
        sfuToken: String,
        getSubscriberSdp: suspend () -> String,
        onWebsocketReconnectStrategy: suspend (WebsocketReconnectStrategy?) -> Unit,
    ): SfuConnectionModule {
        return SfuConnectionModule(
            sfuUrl = sfuUrl,
            sessionId = sessionId,
            sfuToken = sfuToken,
            apiKey = apiKey,
            getSubscriberSdp = getSubscriberSdp,
            loggingLevel = loggingLevel,
            scope = scope,
            networkStateProvider = networkStateProvider,
            onWebsocketReconnectStrategy = onWebsocketReconnectStrategy,
        )
    }

    fun updateToken(newToken: String) {
        coordinatorSocketConnection.updateToken(newToken)
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
    /** The user */
    user: User,
    /** A token which gives you access to the sfu */
    private val sfuToken: String,
    /** A token which gives you access to the sfu */
    private val apiKey: String,
    /** Function that gives a fresh SDP */
    private val subscriberSDPProvider: SessionDescriptionProvider,
    private val loggingLevel: LoggingLevel = LoggingLevel(),
    /** The scope to use for the socket */
    scope: CoroutineScope = CoroutineScope(DispatcherProvider.IO),
    /** Network monitoring */
    networkStateProvider: NetworkStateProvider,
    /**
     * Rejoin strategy to be used when the websocket connection is lost.
     */
    onWebsocketReconnectStrategy: suspend (WebsocketReconnectStrategy?) -> Unit,
) {
    internal var sfuSocket: SfuSocketConnection = SfuSocketConnection(
        url = sfuUrl,
        apiKey = apiKey,
        scope = scope,
        tokenProvider = ConstantTokenProvider(sfuToken),
        networkStateProvider = networkStateProvider
    )
    private val updatedSignalUrl = if (sfuUrl.contains(Regex("https?://"))) {
        sfuUrl
    } else {
        "http://$sfuUrl"
    }.removeSuffix("/twirp")

    private fun buildSfuOkHttpClient(): OkHttpClient {
        val connectionTimeoutInMs = 10000L
        // create a new OkHTTP client and set timeouts
        val authInterceptor = CoordinatorAuthInterceptor(apiKey, sfuToken)
        return OkHttpClient.Builder().addInterceptor(authInterceptor).addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = loggingLevel.httpLoggingLevel.level
                },
            ).retryOnConnectionFailure(true)
            .connectTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .writeTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .readTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .callTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS).build()
    }

    val okHttpClient = buildSfuOkHttpClient()

    private val signalRetrofitClient: Retrofit by lazy {
        Retrofit.Builder().client(okHttpClient).addConverterFactory(WireConverterFactory.create())
            .baseUrl(updatedSignalUrl).build()
    }

    internal val signalService: SignalServerService by lazy {
        signalRetrofitClient.create(SignalServerService::class.java)
    }
}

