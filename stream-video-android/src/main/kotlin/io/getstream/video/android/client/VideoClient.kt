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
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import io.getstream.video.android.client.coordinator.CallCoordinatorClient
import io.getstream.video.android.client.user.UserState
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.module.VideoModule
import io.getstream.video.android.socket.VideoSocket
import io.getstream.video.android.token.TokenProvider
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Result
import io.getstream.video.android.utils.Success
import io.getstream.video.android.utils.VideoError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.logging.HttpLoggingInterceptor
import stream.video.Device
import stream.video.Edge
import stream.video.JoinCallRequest
import stream.video.Latency
import stream.video.SelectEdgeServerRequest
import stream.video.SelectEdgeServerResponse
import stream.video.User
import java.net.URL

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

    public suspend fun joinCall(type: String, id: String): Result<SelectEdgeServerResponse> {
        val callResult = callCoordinatorClient.joinCall(
            JoinCallRequest(
                id = id,
                type = type
            )
        )

        if (callResult is Success) {
            val data = callResult.data

            return try {
                val latencyResults = data.edges.associate {
                    it.latency_url to measureLatency(it)
                }

                Log.d("latency", latencyResults.toString())

                selectEdgeServer(
                    request = SelectEdgeServerRequest(
                        call_id = id,
                        latency_by_edge = latencyResults
                    )
                )
            } catch (error: Throwable) {
                Failure(VideoError(error.message, error))
            }
        } else {
            return Failure((callResult as Failure).error)
        }
    }

    // TODO - pull this out in a utility or a helper class
    private suspend fun measureLatency(edge: Edge): Latency = withContext(Dispatchers.IO) {
        val measurements = mutableListOf<Float>()
        val latencyUrl = edge.latency_url

        // TODO - placeholder for localhost on android
        val url = if (latencyUrl.contains("localhost")) {
            latencyUrl.replace("localhost", "10.0.2.2")
        } else {
            latencyUrl
        }

        repeat(3) {
            val request = URL(url)
            val start = System.currentTimeMillis()
            val connection = request.openConnection()

            connection.connect()

            val end = System.currentTimeMillis()

            val seconds = (end - start) / 1000f
            measurements.add(seconds)
        }

        Latency(measurements_seconds = measurements)
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
