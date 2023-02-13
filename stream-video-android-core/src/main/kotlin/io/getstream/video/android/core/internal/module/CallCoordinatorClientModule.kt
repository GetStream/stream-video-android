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

package io.getstream.video.android.core.internal.module

import android.content.Context
import androidx.lifecycle.Lifecycle
import io.getstream.video.android.core.api.ClientRPCService
import io.getstream.video.android.core.coordinator.CallCoordinatorClient
import io.getstream.video.android.core.coordinator.CallCoordinatorClientImpl
import io.getstream.video.android.core.dispatchers.DispatcherProvider
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.token.CredentialsProvider
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import org.openapitools.client.apis.EventsApi
import org.openapitools.client.apis.VideoCallsApi
import org.openapitools.client.infrastructure.Serializer
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
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
internal class CallCoordinatorClientModule(
    private val user: User,
    private val credentialsProvider: CredentialsProvider,
    private val appContext: Context,
    private val lifecycle: Lifecycle,
    private val okHttpClient: OkHttpClient
) {

    /**
     * Cached instance of the Retrofit client that builds API services.
     */
    private val protoRetrofitClient: Retrofit by lazy {
        Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(WireConverterFactory.create())
            .baseUrl(BASE_URL)
            .build()
    }

    private val retrofitClient: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(Serializer.moshi))
            .client(okHttpClient)
            .build()
    }

    /**
     * Cached instance of the CallCoordinator service client for API calls.
     */
    private val callCoordinatorClient: CallCoordinatorClient by lazy {
        val oldService = protoRetrofitClient.create(ClientRPCService::class.java)
        val videoCallsApi = retrofitClient.create(VideoCallsApi::class.java)
        val eventsApi = retrofitClient.create(EventsApi::class.java)

        CallCoordinatorClientImpl(
            callCoordinatorService = oldService,
            videoCallApi = videoCallsApi,
            eventsApi = eventsApi
        )
    }

    /**
     * The [CoroutineScope] used for all business logic related operations.
     */
    private val scope = CoroutineScope(DispatcherProvider.IO)

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
        private const val BASE_URL = "https://video-edge-frankfurt-ce1.stream-io-api.com/"
    }
}
