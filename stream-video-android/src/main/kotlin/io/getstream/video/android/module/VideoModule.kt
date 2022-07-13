package io.getstream.video.android.module

import android.content.Context
import androidx.lifecycle.Lifecycle
import io.getstream.video.android.api.CallCoordinatorService
import io.getstream.video.android.client.coordinator.CallCoordinatorClient
import io.getstream.video.android.client.coordinator.CallCoordinatorClientImpl
import io.getstream.video.android.client.user.UserState
import io.getstream.video.android.dispatchers.DispatcherProvider
import io.getstream.video.android.socket.VideoSocket
import io.getstream.video.android.socket.VideoSocketImpl
import io.getstream.video.android.token.TokenProvider
import kotlinx.coroutines.CoroutineScope
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.wire.WireConverterFactory
import stream.video.User

/**
 * Serves as an internal DI framework that allows us to cache heavy components reused across the
 * SDK.
 *
 * @property apiKey The key used to authenticate user apps with the API.
 * @property user The currently logged in user.
 * @property tokenProvider Provider of user-tokens.
 * @property appContext The context of the app, used for Android-based dependencies.
 * @property lifecycle The lifecycle of the process.
 * @property loggingLevel Log level used for all HTTP requests towards the API.
 */
internal class VideoModule(
    private val apiKey: String,
    private val user: User,
    private val tokenProvider: TokenProvider,
    private val appContext: Context,
    private val lifecycle: Lifecycle,
    private val loggingLevel: HttpLoggingInterceptor.Level
) {
    /**
     * Cached instance of the HTTP client.
     */
    private val okHttpClient: OkHttpClient by lazy {
        buildOkHttpClient(tokenProvider)
    }

    /**
     * Cached instance of the Retrofit client that builds API services.
     */
    private val retrofitClient: Retrofit by lazy {
        Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(WireConverterFactory.create())
            .baseUrl(BASE_URL)
            .build()
    }

    /**
     * Cached instance of the CallCoordinator service client for API calls.
     */
    private val callCoordinatorClient: CallCoordinatorClient by lazy {
        val service = retrofitClient.create(CallCoordinatorService::class.java)

        CallCoordinatorClientImpl(service)
    }

    /**
     * The [CoroutineScope] used for all business logic related operations.
     */
    private val scope = CoroutineScope(DispatcherProvider.IO)

    /**
     * User state that provides the information about the current user.
     */
    private val userState: UserState by lazy {
        UserState().apply {
            setUser(this@VideoModule.user)
        }
    }

    // TODO - build notification handler/provider

    /**
     * Builds the [OkHttpClient] used for all API calls.
     *
     * @param tokenProvider The user-token provider used to attach authorization headers.
     * @return [OkHttpClient] that allows us API calls.
     */
    private fun buildOkHttpClient(tokenProvider: TokenProvider): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(
                buildInterceptor(
                    apiKey = apiKey,
                    tokenProvider = tokenProvider
                )
            )
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = loggingLevel
                }
            )
            .build()
    }

    /**
     * Builds the HTTP interceptor that adds headers to all API calls.
     *
     * @param apiKey The API key of the app.
     * @param tokenProvider Provider of the user token.
     *
     * @return [Interceptor] which adds headers.
     */
    private fun buildInterceptor(
        apiKey: String,
        tokenProvider: TokenProvider
    ): Interceptor = Interceptor {
        val original = it.request()
        val updated = original.newBuilder()
            .addHeader(HEADER_AUTHORIZATION, tokenProvider.provideUserToken())
            // TODO - add API key to auth or use to authenticate the user?
            .build()

        it.proceed(updated)
    }

    /**
     * Public providers used to set up other components.
     */

    /**
     * @return [CoroutineScope] used for all API requests.
     */
    public fun scope(): CoroutineScope {
        return scope
    }

    /**
     * @return The [CallCoordinatorClient] used to communicate to the API.
     */
    public fun callClient(): CallCoordinatorClient {
        return callCoordinatorClient
    }

    /**
     * @return The WebSocket handler that is used to connect to different calls.
     */
    public fun socket(): VideoSocket {
        return VideoSocketImpl()
    }

    /**
     * @return The [UserState] that serves us information about the currently logged in user.
     */
    public fun userState(): UserState {
        return userState
    }

    private companion object {
        /**
         * Key used to prove authorization to the API.
         */
        private const val HEADER_AUTHORIZATION = "authorization"

        /**
         * The base URL of the API.
         */
        private const val BASE_URL = "http://10.0.2.2:26991"
    }
}