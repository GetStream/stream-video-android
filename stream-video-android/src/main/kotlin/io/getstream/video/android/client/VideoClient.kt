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
import io.getstream.video.android.errors.VideoError
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.model.JoinCallResponse
import io.getstream.video.android.module.HttpModule
import io.getstream.video.android.module.VideoModule
import io.getstream.video.android.socket.SocketListener
import io.getstream.video.android.socket.SocketState
import io.getstream.video.android.socket.SocketStateService
import io.getstream.video.android.socket.VideoSocket
import io.getstream.video.android.token.CredentialsProvider
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Result
import io.getstream.video.android.utils.Success
import io.getstream.video.android.utils.enrichSFUURL
import io.getstream.video.android.utils.getLatencyMeasurements
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.logging.HttpLoggingInterceptor
import stream.video.coordinator.client_v1_rpc.CreateCallInput
import stream.video.coordinator.client_v1_rpc.CreateCallRequest
import stream.video.coordinator.client_v1_rpc.CreateCallResponse
import stream.video.coordinator.client_v1_rpc.GetCallEdgeServerRequest
import stream.video.coordinator.client_v1_rpc.GetCallEdgeServerResponse
import stream.video.coordinator.client_v1_rpc.JoinCallRequest
import stream.video.coordinator.client_v1_rpc.MemberInput
import stream.video.coordinator.edge_v1.Latency
import stream.video.coordinator.edge_v1.LatencyMeasurements
import stream.video.coordinator.push_v1.Device
import stream.video.sfu.User

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
    private val applicationContext: Context,
    private val scope: CoroutineScope,
    private val credentialsProvider: CredentialsProvider,
    private val socket: VideoSocket,
    private val socketStateService: SocketStateService,
    private val userState: UserState,
    private val callCoordinatorClient: CallCoordinatorClient
) {

    /**
     * Observes the app lifecycle and attempts to reconnect/release the socket connection.
     */
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

        socket.connectSocket()
    }

    /**
     * Start region - API calls.
     */

    public fun registerDevice(device: Device) {
    }

    /**
     * @see CallCoordinatorClient.joinCall for details.
     */
    public suspend fun joinCall(
        type: String,
        id: String,
        participantIds: List<String> = emptyList()
    ): Result<JoinCallResponse> {
        val createCallResult = callCoordinatorClient.getOrCreateCall(
            CreateCallRequest(
                type = type,
                id = id,
                input = CreateCallInput(
                    members = participantIds.associateWith {
                        MemberInput(role = "admin")
                    }
                )
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
     * @return [Result] wrapper around [JoinCallResponse] once the correct server is chosen.
     */
    private suspend fun joinCreatedCall(response: CreateCallResponse): Result<JoinCallResponse> {
        val call = response.call?.call!!

        val callResult = callCoordinatorClient.joinCall(
            JoinCallRequest(
                id = call.id,
                type = call.type
            )
        )

        if (callResult is Success) {
            val data = callResult.data

            return try {
                val latencyResults = data.latency_claim?.endpoints?.associate {
                    it.url to measureLatency(it.url)
                } ?: emptyMap()

                val selectEdgeServerResult = selectEdgeServer(
                    request = GetCallEdgeServerRequest(
                        call_cid = call.call_cid,
                        measurements = LatencyMeasurements(measurements = latencyResults)
                    )
                )

                when (selectEdgeServerResult) {
                    is Success -> {
                        socket.updateCallState(call)
                        val credentials = selectEdgeServerResult.data.credentials
                        val url = credentials?.server?.url

                        Success(
                            JoinCallResponse(
                                call = call,
                                callUrl = enrichSFUURL(url!!),
                                userToken = credentials.token
                            )
                        )
                    }
                    is Failure -> Failure(selectEdgeServerResult.error)
                }
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
     * @param edgeUrl The edge we want to measure.
     * @return [Latency] which contains measurements from ping connections.
     */
    private suspend fun measureLatency(edgeUrl: String): Latency = withContext(Dispatchers.IO) {
        val measurements = getLatencyMeasurements(edgeUrl)

        Latency(measurements_seconds = measurements)
    }

    /**
     * @see CallCoordinatorClient.selectEdgeServer for details.
     */
    public suspend fun selectEdgeServer(request: GetCallEdgeServerRequest): Result<GetCallEdgeServerResponse> {
        return callCoordinatorClient.selectEdgeServer(request)
    }

    /**
     * @see CallCoordinatorClient.sendUserEvent for details. TODO - fix this
     */
//    public suspend fun sendUserEvent(userEventType: UserEventType): Result<Boolean> {
//        val call = socket.getCallState()
//            ?: return Failure(error = VideoError(message = "No call is active!"))
//
//        return callCoordinatorClient.sendUserEvent(
//            SendEventRequest(
//                user_id = userState.user.value.id,
//                call_id = call.id,
//                call_type = call.type,
//                event_type = userEventType
//            )
//        )
//    }

    /**
     * End region - API calls.
     */

    /**
     * Notifies the client that we've left the call and can clean up state.
     */
    public fun leaveCall() {
        socket.updateCallState(null)
    }

    /**
     * Attempts to reconnect the socket if it's in a disconnected state and the user is available.
     */
    public fun reconnectSocket() {
        val user = userState.user.value

        if (socketStateService.state !is SocketState.Connected && user.id.isNotBlank()) {
            socket.reconnect()
        }
    }

    /**
     * Returns the currently logged in user.
     */
    public fun getUser(): User = userState.user.value

    /**
     * Attaches a listener to the active Socket.
     *
     * @param socketListener The listener that will consume events.
     */
    public fun addSocketListener(socketListener: SocketListener) {
        socket.addListener(socketListener)
    }

    /**
     * Removes a listener from the active Socket.
     *
     * @param socketListener The listener that will be removed.
     */
    public fun removeSocketListener(socketListener: SocketListener) {
        socket.removeListener(socketListener)
    }

    /**
     * Builder for the [VideoClient] that sets all the dependencies up.
     *
     * @property user Currently logged in user.
     * @property appContext Context of the app.
     * @property credentialsProvider Handle that provides the user token.
     */
    public class Builder(
        private val user: User,
        private val appContext: Context,
        private val credentialsProvider: CredentialsProvider
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
            if (credentialsProvider.loadApiKey().isBlank() ||
                user.id.isBlank() ||
                credentialsProvider.getCachedToken().isBlank()
            ) throw IllegalArgumentException("The API key, user ID and token cannot be empty!")

            val lifecycle = ProcessLifecycleOwner.get().lifecycle

            val httpModule = HttpModule.getOrCreate(loggingLevel, credentialsProvider)

            val videoModule = VideoModule(
                user = user,
                credentialsProvider = credentialsProvider,
                appContext = appContext,
                lifecycle = lifecycle,
                okHttpClient = httpModule.okHttpClient
            )

            return VideoClient(
                lifecycle = lifecycle,
                applicationContext = appContext,
                credentialsProvider = credentialsProvider,
                scope = videoModule.scope(),
                socket = videoModule.socket(),
                socketStateService = SocketStateService(),
                userState = videoModule.userState(),
                callCoordinatorClient = videoModule.callClient()
            )
        }
    }
}
