package io.getstream.video.android.core.internal.module

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.Lifecycle
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.socket.common.token.ConstantTokenProvider
import io.getstream.video.android.core.socket.sfu.SfuSocketConnection
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.SfuToken
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.wire.WireConverterFactory
import stream.video.sfu.models.WebsocketReconnectStrategy
import java.util.concurrent.TimeUnit

internal class SfuConnectionModule(
    context: Context,
    override val apiKey: ApiKey,
    override val apiUrl: String,
    override val wssUrl: String,
    override val connectionTimeoutInMs: Long,
    override val userToken: SfuToken,
    override val lifecycle: Lifecycle,

) : ConnectionModuleDeclaration<SignalServerService, SfuSocketConnection, OkHttpClient, SfuToken> {

    // Internal logic
    private val signalRetrofitClient: Retrofit by lazy {
        Retrofit.Builder().client(http).addConverterFactory(WireConverterFactory.create())
            .baseUrl(apiUrl).build()
    }
    private fun buildSfuOkHttpClient(): OkHttpClient {
        val connectionTimeoutInMs = 10000L
        // create a new OkHTTP client and set timeouts
        val authInterceptor = CoordinatorAuthInterceptor(apiKey, userToken)
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
    override val api: SignalServerService = signalRetrofitClient.create(SignalServerService::class.java)
    override val http: OkHttpClient = buildSfuOkHttpClient()
    override val networkStateProvider: NetworkStateProvider by lazy {
        NetworkStateProvider(
            scope,
            connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
        )
    }
    private var _internalSocketConnection : SfuSocketConnection = SfuSocketConnection(
                url = wssUrl,
                apiKey = apiKey,
                scope = scope,
                httpClient = http,
                tokenProvider = ConstantTokenProvider(userToken),
                lifecycle = lifecycle,
                networkStateProvider = networkStateProvider
            )
    override val socketConnection: SfuSocketConnection = _internalSocketConnection

    override fun updateToken(token: SfuToken) {
        _internalSocketConnection.onDisconnected()
    }

    override fun updateAuthType(authType: String) {
        throw UnsupportedOperationException("Not supported for SFU")
    }
}