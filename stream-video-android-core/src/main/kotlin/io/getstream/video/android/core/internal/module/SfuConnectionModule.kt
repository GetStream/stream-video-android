/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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
import io.getstream.video.android.core.call.utils.SignalLostSignalingServiceDecorator
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.socket.common.token.ConstantTokenProvider
import io.getstream.video.android.core.socket.common.token.TokenRepository
import io.getstream.video.android.core.socket.sfu.SfuSocketConnection
import io.getstream.video.android.core.trace.Tracer
import io.getstream.video.android.core.trace.tracedWith
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.SfuToken
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.wire.WireConverterFactory
import stream.video.sfu.models.Error
import java.util.concurrent.TimeUnit

internal class SfuConnectionModule(
    context: Context,
    val tokenRepository: TokenRepository,
    override val apiKey: ApiKey,
    override val apiUrl: String,
    override val wssUrl: String,
    override val connectionTimeoutInMs: Long,
    override val lifecycle: Lifecycle,
    override val tracer: Tracer,
    val onSignalingLost: (Error) -> Unit,
) : ConnectionModuleDeclaration<SignalServerService, SfuSocketConnection, OkHttpClient, SfuToken> {

    // Internal logic
    override val http: OkHttpClient = buildSfuOkHttpClient()

    private val signalRetrofitClient: Retrofit by lazy {
        Retrofit.Builder().client(http).addConverterFactory(WireConverterFactory.create())
            .baseUrl("$apiUrl/").build()
    }
    private fun buildSfuOkHttpClient(): OkHttpClient {
        val connectionTimeoutInMs = 10000L
        // create a new OkHTTP client and set timeouts
        val authInterceptor = CoordinatorAuthInterceptor(apiKey, tokenRepository)
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

    // API
    override val api: SignalServerService = SignalLostSignalingServiceDecorator(
        tracedWith(
            signalRetrofitClient.create(
                SignalServerService::class.java,
            ),
            tracer,
        ),
    ) {
        onSignalingLost(it)
    }
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
        tokenProvider = ConstantTokenProvider(tokenRepository.getToken()),
        lifecycle = lifecycle,
        networkStateProvider = networkStateProvider,
        tokenRepository = tokenRepository,
    )
    override val socketConnection: SfuSocketConnection = _internalSocketConnection

    override fun updateToken(token: SfuToken) {
        tokenRepository.updateToken(token)
        _internalSocketConnection.updateToken(token)
    }

    override fun updateAuthType(authType: String) {
        throw UnsupportedOperationException("Not supported for SFU, do not call.")
    }
}
