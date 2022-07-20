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
import io.getstream.video.android.errors.VideoError
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.module.VideoModule
import io.getstream.video.android.socket.SocketListener
import io.getstream.video.android.socket.SocketState
import io.getstream.video.android.socket.SocketStateService
import io.getstream.video.android.socket.VideoSocket
import io.getstream.video.android.token.TokenProvider
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Result
import io.getstream.video.android.utils.Success
import io.getstream.video.android.utils.getLatencyMeasurements
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.logging.HttpLoggingInterceptor
import stream.video.CreateCallRequest
import stream.video.CreateCallResponse
import stream.video.Device
import stream.video.Edge
import stream.video.JoinCallRequest
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
    private val socketStateService: SocketStateService,
    private val userState: UserState,
    private val callCoordinatorClient: CallCoordinatorClient
) {

    private val lifecycleObserver = StreamLifecycleObserver(
        lifecycle,
        object : LifecycleHandler {
            override fun resume() = reconnectSocket()
            override fun stopped() {
                socket.releaseConnection()
            }
        }
    )

    init {
        scope.launch(Dispatchers.Main.immediate) {
            lifecycleObserver.observe()
        }

        socket.connectUser(userState.user.value)

        // TODO check if we need to add any listener logic
        socket.addListener(object : SocketListener() {
            override fun onConnecting() {
                super.onConnecting()
            }
        })
    }

    public fun registerDevice(device: Device) {
    }

    /**
     * @see CallCoordinatorClient.joinCall for details.
     */
    public suspend fun joinCall(type: String, id: String): Result<SelectEdgeServerResponse> {
        val createCallResult = callCoordinatorClient.createCall(
            CreateCallRequest(
                type = type,
                id = id
            )
        )

        return when (createCallResult) {
            is Success -> joinCreatedCall(createCallResult.data)
            is Failure -> return Failure(createCallResult.error)
        }
    }

    /**
     * Once the call is set up, we can initiate the Join flow, by analyzing the latency of servers
     * and choosing the correct one.
     *
     * @param response Information about the newly created call.
     * @return [Result] wrapper around [SelectEdgeServerResponse] once the correct server is chosen.
     */
    private suspend fun joinCreatedCall(response: CreateCallResponse): Result<SelectEdgeServerResponse> {
        val call = response.call!!

        val callResult = callCoordinatorClient.joinCall(
            JoinCallRequest(
                id = call.id,
                type = call.type
            )
        )

        if (callResult is Success) {
            val data = callResult.data

            return try {
                val latencyResults = data.edges.associate {
                    it.latency_url to measureLatency(it)
                }

                Log.d("latencyCheck", latencyResults.toString())

                selectEdgeServer(
                    request = SelectEdgeServerRequest(
                        call_id = call.id,
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

    /**
     * Measures and prepares the latency which describes how much time it takes to ping the server.
     *
     * @param edge The edge we want to measure.
     * @return [Latency] which contains measurements from ping connections.
     */
    private suspend fun measureLatency(edge: Edge): Latency = withContext(Dispatchers.IO) {
        val measurements = getLatencyMeasurements(edge.latency_url)

        Latency(measurements_seconds = measurements)
    }

    /**
     * @see CallCoordinatorClient.selectEdgeServer for details.
     */
    public suspend fun selectEdgeServer(request: SelectEdgeServerRequest): Result<SelectEdgeServerResponse> {
        return callCoordinatorClient.selectEdgeServer(request)
    }

    /**
     * Attempts to reconnect the socket if it's in a disconnected state and the user is available.
     */
    public fun reconnectSocket() {
        val user = userState.user.value

        if (socketStateService.state is SocketState.Disconnected && user.id.isNotBlank()) {
            socket.reconnectUser(user)
        }
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
            if (apiKey.isBlank() ||
                user.id.isBlank() ||
                tokenProvider.getCachedToken().isBlank()
            ) throw IllegalArgumentException("The API key, user ID and token cannot be empty!")

            val lifecycle = ProcessLifecycleOwner.get().lifecycle

            val videoModule = VideoModule(
                apiKey = apiKey,
                user = user,
                tokenProvider = tokenProvider,
                appContext = appContext,
                lifecycle = lifecycle,
                loggingLevel = loggingLevel
            )

            return VideoClient(
                lifecycle = lifecycle,
                tokenProvider = tokenProvider,
                scope = videoModule.scope(),
                socket = videoModule.socket(),
                socketStateService = SocketStateService(),
                userState = videoModule.userState(),
                callCoordinatorClient = videoModule.callClient()
            )
        }
    }
}
