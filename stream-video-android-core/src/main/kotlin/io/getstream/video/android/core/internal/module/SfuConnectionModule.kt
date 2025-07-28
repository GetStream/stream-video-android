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
import io.getstream.video.android.core.internal.network.ApiKeyInterceptor
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.internal.network.TokenAuthInterceptor
import io.getstream.video.android.core.socket.common.token.CacheableTokenProvider
import io.getstream.video.android.core.socket.common.token.ConstantTokenProvider
import io.getstream.video.android.core.socket.common.token.TokenManager
import io.getstream.video.android.core.socket.common.token.TokenManagerImpl
import io.getstream.video.android.core.socket.sfu.SfuSocketConnection
import io.getstream.video.android.core.trace.Tracer
import io.getstream.video.android.core.trace.tracedWith
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.SfuToken
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.wire.WireConverterFactory
import java.util.concurrent.TimeUnit

internal class SfuConnectionModule(
    context: Context,
    override val apiKey: ApiKey,
    override val apiUrl: String,
    override val wssUrl: String,
    override val connectionTimeoutInMs: Long,
    override val userToken: SfuToken,
    override val lifecycle: Lifecycle,
    override val tracer: Tracer,
) : ConnectionModuleDeclaration<SignalServerService, SfuSocketConnection, OkHttpClient, SfuToken> {

    // Internal logic
    private val tokenManager: TokenManager = TokenManagerImpl(
        CacheableTokenProvider(ConstantTokenProvider(userToken)),
    )
    override val http: OkHttpClient = buildSfuOkHttpClient()

    private val signalRetrofitClient: Retrofit by lazy {
        Retrofit.Builder().client(http).addConverterFactory(WireConverterFactory.create())
            .baseUrl("$apiUrl/").build()
    }
    private fun buildSfuOkHttpClient(): OkHttpClient {
        val connectionTimeoutInMs = 10000L
        // create a new OkHTTP client and set timeouts
        return OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor(apiKey))
            .addInterceptor(TokenAuthInterceptor(tokenManager))
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = loggingLevel.httpLoggingLevel.level
                },
            ).retryOnConnectionFailure(true)
            .connectTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .writeTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .readTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .callTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS).build()
    }

    // API
    override val api: SignalServerService = tracedWith(
        signalRetrofitClient.create(
            SignalServerService::class.java,
        ),
        tracer,
    )
    override val networkStateProvider: NetworkStateProvider by lazy {
        NetworkStateProvider(
            scope,
            connectivityManager = context.getSystemService(
                Context.CONNECTIVITY_SERVICE,
            ) as ConnectivityManager,
        )
    }
    private var _internalSocketConnection: SfuSocketConnection = SfuSocketConnection(
        url = wssUrl,
        apiKey = apiKey,
        scope = scope,
        httpClient = http,
        tokenManager = tokenManager,
        lifecycle = lifecycle,
        networkStateProvider = networkStateProvider,
    )
    override val socketConnection: SfuSocketConnection = _internalSocketConnection

    override fun updateToken(token: SfuToken?) {
        throw UnsupportedOperationException(
            "Update token is not supported for SFU. Create a new socket instead.",
        )
    }

    override fun updateAuthType(authType: String) {
        throw UnsupportedOperationException("Not supported for SFU, do not call.")
    }
}
