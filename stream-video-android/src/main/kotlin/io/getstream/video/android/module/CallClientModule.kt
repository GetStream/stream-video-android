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

package io.getstream.video.android.module

import android.content.Context
import androidx.lifecycle.Lifecycle
import io.getstream.video.android.api.CallCoordinatorService
import io.getstream.video.android.client.coordinator.CallCoordinatorClient
import io.getstream.video.android.client.coordinator.CallCoordinatorClientImpl
import io.getstream.video.android.dispatchers.DispatcherProvider
import io.getstream.video.android.model.User
import io.getstream.video.android.token.CredentialsProvider
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.wire.WireConverterFactory

/**
 * Serves as an internal DI framework that allows us to cache heavy components reused across the
 * SDK.
 *
 * @property user The currently logged in user.
 * @property credentialsProvider Provider of user-tokens.
 * @property appContext The context of the app, used for Android-based dependencies.
 * @property lifecycle The lifecycle of the process.
 */
internal class CallClientModule(
    private val user: User,
    private val credentialsProvider: CredentialsProvider,
    private val appContext: Context,
    private val lifecycle: Lifecycle,
    private val okHttpClient: OkHttpClient
) {

    /**
     * Cached instance of the Retrofit client that builds API services.
     */
    private val retrofitClient: Retrofit by lazy {
        Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(WireConverterFactory.create())
            .baseUrl(REDIRECT_BASE_URL ?: BASE_URL)
            .build()
    }

    /**
     * Cached instance of the CallCoordinator service client for API calls.
     */
    private val callCoordinatorClient: CallCoordinatorClient by lazy {
        val service = retrofitClient.create(CallCoordinatorService::class.java)

        CallCoordinatorClientImpl(service, credentialsProvider)
    }

    /**
     * The [CoroutineScope] used for all business logic related operations.
     */
    private val scope = CoroutineScope(DispatcherProvider.IO)

    // TODO - build notification handler/provider

    /**
     * Public providers used to set up other components.
     */

    /**
     * @return [CoroutineScope] used for all API requests.
     */
    internal fun scope(): CoroutineScope {
        return scope
    }

    /**
     * @return The [CallCoordinatorClient] used to communicate to the API.
     */
    internal fun callCoordinatorClient(): CallCoordinatorClient {
        return callCoordinatorClient
    }

    internal companion object {
        /**
         * Used for testing on devices and redirecting from a public realm to localhost.
         *
         * Will only be used if the value is non-null, so if you're able to test locally, just
         * leave it as-is.
         */
        @Suppress("RedundantNullableReturnType")
        private val REDIRECT_BASE_URL: String? = "https://1281-93-140-69-118.eu.ngrok.io" // "https://a769-93-140-102-246.eu.ngrok.io"
        // e.g. "https://dc54-83-131-252-51.eu.ngrok.io"

        /**
         * The base URL of the API.
         */
        private const val BASE_URL = "https://rpc-video-coordinator.oregon-v1.stream-io-video.com/"

        /**
         * Used for testing on devices and redirecting from a public realm to localhost.
         *
         * Will only be used if the value is non-null, so if you're able to test locally, just
         * leave it as-is.
         */
        @Suppress("RedundantNullableReturnType")
        internal val REDIRECT_PING_URL: String? = "https://ba93-93-140-69-118.eu.ngrok.io/ping" // "https://c99c-93-140-102-246.eu.ngrok.io/ping" // "<redirect-url>/ping"
    }
}
