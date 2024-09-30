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
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.socket.common.token.TokenProvider
import io.getstream.video.android.core.socket.coordinator.CoordinatorSocketConnection
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
            sfuToken = sfuToken,
            apiKey = apiKey,
            loggingLevel = loggingLevel,
            scope = scope,
            networkStateProvider = networkStateProvider,
            lifecycle = lifecycle,
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

