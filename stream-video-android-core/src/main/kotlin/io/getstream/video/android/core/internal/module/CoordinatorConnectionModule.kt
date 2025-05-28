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
import io.getstream.android.video.generated.apis.ProductvideoApi
import io.getstream.android.video.generated.infrastructure.Serializer
import io.getstream.log.streamLog
import io.getstream.video.android.core.header.HeadersUtil
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.socket.common.token.CacheableTokenProvider
import io.getstream.video.android.core.socket.common.token.ConstantTokenProvider
import io.getstream.video.android.core.socket.common.token.TokenManagerImpl
import io.getstream.video.android.core.socket.common.token.TokenProvider
import io.getstream.video.android.core.socket.coordinator.CoordinatorSocketConnection
import io.getstream.video.android.core.trace.Tracer
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserToken
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

/**
 * ConnectionModule for the coordinator socket.
 */
internal class CoordinatorConnectionModule(
    // Coordinator API
    context: Context,
    tokenProvider: TokenProvider,
    user: User,
    override val scope: CoroutineScope,
    // Common API
    override val apiUrl: String,
    override val wssUrl: String,
    override val connectionTimeoutInMs: Long,
    override val loggingLevel: LoggingLevel = LoggingLevel(),
    override val apiKey: ApiKey,
    override val userToken: UserToken,
    override val lifecycle: Lifecycle,
    override val tracer: Tracer = Tracer("coordinator"),
) : ConnectionModuleDeclaration<ProductvideoApi, CoordinatorSocketConnection, OkHttpClient, UserToken> {

    private val tokenManager = TokenManagerImpl(CacheableTokenProvider(tokenProvider))

    // Internals
    private val authInterceptor = CoordinatorAuthInterceptor(tokenManager, apiKey)
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder().baseUrl(apiUrl)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(Serializer.moshi))
            .client(http).build()
    }

    // API

    override val http: OkHttpClient = OkHttpClient.Builder().addInterceptor(
        HeadersInterceptor(HeadersUtil()),
    )
        .addInterceptor(authInterceptor).addInterceptor(
            HttpLoggingInterceptor {
                streamLog(tag = "Video:Http") { it }
            }.apply {
                level = loggingLevel.httpLoggingLevel.level
            },
        ).retryOnConnectionFailure(true)
        .connectTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
        .writeTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
        .readTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
        .callTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS).build()
    override val networkStateProvider: NetworkStateProvider by lazy {
        NetworkStateProvider(
            scope,
            connectivityManager = context.getSystemService(
                Context.CONNECTIVITY_SERVICE,
            ) as ConnectivityManager,
        )
    }
    override val api: ProductvideoApi by lazy { retrofit.create(ProductvideoApi::class.java) }
    override val socketConnection: CoordinatorSocketConnection = CoordinatorSocketConnection(
        apiKey = apiKey,
        url = wssUrl,
        user = user,
        token = userToken,
        httpClient = http,
        networkStateProvider = networkStateProvider,
        scope = scope,
        lifecycle = lifecycle,
        tokenManager = tokenManager,
    )

    override fun updateToken(token: UserToken?) {
        token?.let { CacheableTokenProvider(ConstantTokenProvider(it)) }
            ?.let { tokenManager.updateTokenProvider(it) }
        ?: tokenManager.loadSync()
    }

    override fun updateAuthType(authType: String) {
        authInterceptor.authType = authType
    }
}
