package io.getstream.video.android.core.internal.module

import androidx.lifecycle.Lifecycle
import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.socket.common.token.ConstantTokenProvider
import io.getstream.video.android.core.socket.sfu.SfuSocketConnection
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.wire.WireConverterFactory
import stream.video.sfu.models.WebsocketReconnectStrategy
import java.util.concurrent.TimeUnit

/**
 * Sets upt he signalService and socket for the SFU connection.
 */
internal class SfuConnectionModule(
    /** The url of the SFU */
    sfuUrl: String,
    /** The lifecycle of the application */
    private val lifecycle: Lifecycle,
    /** A token which gives you access to the sfu */
    private val sfuToken: String,
    /** A token which gives you access to the sfu */
    private val apiKey: String,
    /** Logging levels */
    private val loggingLevel: LoggingLevel = LoggingLevel(),
    /** The scope to use for the socket */
    scope: CoroutineScope = CoroutineScope(DispatcherProvider.IO),
    /** Network monitoring */
    networkStateProvider: NetworkStateProvider,
) {
    // Internal logic
    private val signalRetrofitClient: Retrofit by lazy {
        Retrofit.Builder().client(okHttpClient).addConverterFactory(WireConverterFactory.create())
            .baseUrl(updatedSignalUrl).build()
    }
    private val updatedSignalUrl = if (sfuUrl.contains(Regex("https?://"))) {
        sfuUrl
    } else {
        "http://$sfuUrl"
    }.removeSuffix("/twirp")
    private val okHttpClient = buildSfuOkHttpClient()

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

    // Internal API
    internal var sfuSocket: SfuSocketConnection = SfuSocketConnection(
        url = sfuUrl,
        apiKey = apiKey,
        scope = scope,
        httpClient = okHttpClient,
        tokenProvider = ConstantTokenProvider(sfuToken),
        lifecycle = lifecycle,
        networkStateProvider = networkStateProvider
    )

    internal val signalService: SignalServerService by lazy {
        signalRetrofitClient.create(SignalServerService::class.java)
    }
}