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

package io.getstream.video.android.client

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import io.getstream.video.android.client.coordinator.CallCoordinatorClient
import io.getstream.video.android.client.user.UserState
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.module.VideoModule
import io.getstream.video.android.socket.VideoSocket
import io.getstream.video.android.token.TokenProvider
import io.getstream.video.android.utils.Result
import io.livekit.android.ConnectOptions
import kotlinx.coroutines.CoroutineScope
import okhttp3.logging.HttpLoggingInterceptor
import stream.video.Call
import stream.video.Device
import stream.video.Latency
import stream.video.SelectEdgeServerRequest
import stream.video.SelectEdgeServerResponse
import stream.video.User

/**
 * The core client that handles all API and socket communication and acts as a central place to
 * request information from.
 *
 * @param lifecycle The lifecycle used to observe changes in the process.
 * @property scope CoroutinesScope used for the lifecycle of coroutines.
 * @property socket Handler for socket connection and logic.
 * @property userState State of the user, used to update internal services.
 * @property callCoordinatorClient The client used for API communication.
 */
public class VideoClient(
    lifecycle: Lifecycle,
    private val scope: CoroutineScope,
    private val tokenProvider: TokenProvider,
    private val socket: VideoSocket,
    private val userState: UserState,
    private val callCoordinatorClient: CallCoordinatorClient
) {

    // TODO lifecyle observer

    public fun setUser(user: User) {
        this.userState.setUser(user)
    }

    public fun registerDevice(device: Device) {
    }

    public fun joinCall(type: String, id: String, connectOptions: ConnectOptions): Call {
        // TODO - implement
        return Call()
    }

    public fun measureLatency(call: Call): Latency {

        // TODO - implement
        return Latency()
    }

    /**
     * @see CallCoordinatorClient.selectEdgeServer for details.
     */
    public suspend fun selectEdgeServer(request: SelectEdgeServerRequest): Result<SelectEdgeServerResponse> {
        return callCoordinatorClient.selectEdgeServer(request)
    }

    /**
     * Builder for the [VideoClient] that sets all the dependencies up.
     *
     * @property apiKey The key used to validate the user app.
     * @property user Currently logged in user.
     * @property appContext Context of the app.
     * @property tokenProvider Handle that provides the user token.
     */
    public class Builder(
        private val apiKey: String,
        private val user: User,
        private val appContext: Context,
        private val tokenProvider: TokenProvider
    ) {
        private var loggingLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.NONE

        /**
         * Sets the Logging level for all API calls.
         *
         * @param loggingLevel The level of information to log.
         */
        public fun loggingLevel(loggingLevel: LoggingLevel): Builder {
            this.loggingLevel = loggingLevel.httpLoggingLevel

            return this
        }

        /**
         * Builds the [VideoClient] and its respective dependencies, used to set up all the business
         * logic of the SDK.
         */
        public fun build(): VideoClient {
            if (apiKey.isBlank()) throw IllegalArgumentException("API key cannot be empty!")

            val lifecycle = ProcessLifecycleOwner.get().lifecycle

            val videoModule = VideoModule(
                apiKey = apiKey,
                user = user,
                tokenProvider = tokenProvider,
                appContext = appContext,
                lifecycle = lifecycle,
                loggingLevel = loggingLevel
            )

            val videoClient = VideoClient(
                lifecycle = lifecycle,
                tokenProvider = tokenProvider,
                scope = videoModule.scope(),
                socket = videoModule.socket(),
                userState = videoModule.userState(),
                callCoordinatorClient = videoModule.callClient()
            )

            return videoClient
        }
    }
}
