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
import java.util.concurrent.TimeUnit

class ConnectionModule2(
    context: Context,
    private val scope: CoroutineScope,
    private val tokenProvider: TokenProvider,
    private val videoDomain: String,
    private val connectionTimeoutInMs: Long = 30000L,
    private val loggingLevel: LoggingLevel = LoggingLevel(),
    private val user: User,
    internal val apiKey: ApiKey,
    internal val userToken: UserToken,
    internal val lifecycle: Lifecycle,
) {
    private val authInterceptor = CoordinatorAuthInterceptor(apiKey, userToken)
    private val headersInterceptor = HeadersInterceptor()
    private val networkStateProvider = NetworkStateProvider(
        scope,
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    )
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://$videoDomain")
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(Serializer.moshi))
            .client(okHttpClient)
            .build()
    }
    private val api: ProductvideoApi by lazy { retrofit.create(ProductvideoApi::class.java) }
    private  val coordinator: CoordinatorSocketConnection by lazy {
        CoordinatorSocketConnection(
            scope = scope,
            tokenProvider = tokenProvider,
            url = videoDomain,
            user = user,
            httpClient = okHttpClient,
            apiKey = apiKey,
            token = userToken,
            networkStateProvider = networkStateProvider,
            lifecycle = lifecycle,
        )
    }

    // API
    /**
     * Get the ProductvideoApi
     */
    public fun api() : ProductvideoApi = api

    /**
     * Get the CoordinatorSocketConnection
     */
    public fun coordinatorConnection(): CoordinatorSocketConnection = coordinator

    // Internal implementation
    private val okHttpClient: OkHttpClient by lazy {
        // create a new OkHTTP client and set timeouts
        OkHttpClient.Builder()
            .addInterceptor(headersInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = loggingLevel.httpLoggingLevel.level
                },
            )
            .retryOnConnectionFailure(true)
            .connectTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .writeTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .readTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .callTimeout(connectionTimeoutInMs, TimeUnit.MILLISECONDS)
            .build()
    }
}