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
import io.getstream.video.android.client.coordinator.CallCoordinatorClient
import io.getstream.video.android.client.user.UserState
import io.getstream.video.android.errors.VideoError
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.model.CallMetadata
import io.getstream.video.android.model.JoinedCall
import io.getstream.video.android.model.User
import io.getstream.video.android.module.CallClientModule
import io.getstream.video.android.module.HttpModule
import io.getstream.video.android.socket.VideoSocket
import io.getstream.video.android.token.CredentialsProvider
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Result
import io.getstream.video.android.utils.Success
import io.getstream.video.android.utils.enrichSFUURL
import io.getstream.video.android.utils.getLatencyMeasurements
import io.getstream.video.android.utils.toCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

/**
 * The core client that handles all API and socket communication and acts as a central place to
 * request information from.
 *
 * @property scope CoroutinesScope used for the lifecycle of coroutines.
 * @property socket Handler for socket connection and logic.
 * @property userState State of the user, used to update internal services.
 * @property callCoordinatorClient The client used for API communication.
 */
public class CallClient(
    private val scope: CoroutineScope,
    private val userState: UserState,
    private val callCoordinatorClient: CallCoordinatorClient,
    private val socket: VideoSocket
) {

    /**
     * Start region - API calls.
     */

    public fun registerDevice(device: Device) {
    }

    /**
     * @see CallCoordinatorClient.createCall for details.
     */
    public suspend fun createCall(
        type: String,
        id: String,
        participantIds: List<String>
    ): Result<CreateCallResponse> {
        return callCoordinatorClient.getOrCreateCall(
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
    }

    /**
     * @see CallCoordinatorClient.joinCall for details.
     */
    public suspend fun createAndJoinCall(
        type: String,
        id: String,
        participantIds: List<String> = emptyList()
    ): Result<JoinedCall> {
        return when (val createCallResult = createCall(type, id, participantIds)) {
            is Success -> this.joinCall(createCallResult.data.call?.call?.toCall()!!)
            is Failure -> return Failure(createCallResult.error)
        }
    }

    /**
     * Once the call is set up, we can initiate the Join flow, by analyzing the latency of servers
     * and choosing the correct one.
     *
     * @param call Information about the call.
     * @return [Result] wrapper around [JoinedCall] once the correct server is chosen.
     */
    public suspend fun joinCall(call: CallMetadata): Result<JoinedCall> {
        val callResult = callCoordinatorClient.joinCall(
            JoinCallRequest(
                id = call.id,
                type = call.type,
                datacenter_id = "milan"
            )
        )

        if (callResult is Success) {
            val data = callResult.data

            return try {
                val latencyResults = data.edges.associate {
                    it.name to measureLatency(it.latency_url)
                }

                val selectEdgeServerResult = selectEdgeServer(
                    request = GetCallEdgeServerRequest(
                        call_cid = call.cid,
                        measurements = LatencyMeasurements(measurements = latencyResults)
                    )
                )

                when (selectEdgeServerResult) {
                    is Success -> {
                        socket.updateCallState(call)
                        val credentials = selectEdgeServerResult.data.credentials
                        val url = credentials?.server?.url

                        Success(
                            JoinedCall(
                                call = call,
                                callUrl = enrichSFUURL(url!!), // TODO - once the SFU and coord are fully published, won't need this
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
    private suspend fun selectEdgeServer(request: GetCallEdgeServerRequest): Result<GetCallEdgeServerResponse> {
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
     * Returns the currently logged in user.
     */
    public fun getUser(): User = userState.user.value

    /**
     * Builder for the [CallClient] that sets all the dependencies up.
     *
     * @property appContext Context of the app.
     * @property credentialsProvider Handle that provides the user token.
     */
    public class Builder(
        private val appContext: Context,
        private val credentialsProvider: CredentialsProvider,
        private val loggingLevel: LoggingLevel,
        private val socket: VideoSocket,
        private val userState: UserState,
        private val lifecycle: Lifecycle
    ) {
        /**
         * Builds the [CallClient] and its respective dependencies, used to set up all the business
         * logic of the SDK.
         */
        public fun build(): CallClient {
            val user = credentialsProvider.getUserCredentials()

            if (credentialsProvider.loadApiKey().isBlank() ||
                user.id.isBlank() ||
                credentialsProvider.getCachedToken().isBlank()
            ) throw IllegalArgumentException("The API key, user ID and token cannot be empty!")

            val httpModule =
                HttpModule.getOrCreate(loggingLevel.httpLoggingLevel, credentialsProvider)

            val callClientModule = CallClientModule(
                user = user,
                credentialsProvider = credentialsProvider,
                appContext = appContext,
                lifecycle = lifecycle,
                okHttpClient = httpModule.okHttpClient
            )

            return CallClient(
                scope = callClientModule.scope(),
                userState = userState,
                callCoordinatorClient = callClientModule.callCoordinatorClient(),
                socket = socket
            )
        }
    }
}
