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

package io.getstream.video.android

import android.content.Context
import androidx.lifecycle.Lifecycle
import io.getstream.logging.StreamLog
import io.getstream.video.android.call.CallClient
import io.getstream.video.android.call.builder.CallClientBuilder
import io.getstream.video.android.coordinator.CallCoordinatorClient
import io.getstream.video.android.coordinator.state.UserState
import io.getstream.video.android.engine.StreamCallEngineImpl
import io.getstream.video.android.errors.VideoError
import io.getstream.video.android.logging.LoggingLevel
import io.getstream.video.android.model.CallEventType
import io.getstream.video.android.model.CallMetadata
import io.getstream.video.android.model.IceServer
import io.getstream.video.android.model.JoinedCall
import io.getstream.video.android.model.StartedCall
import io.getstream.video.android.model.User
import io.getstream.video.android.model.state.DropReason
import io.getstream.video.android.model.state.StreamCallState
import io.getstream.video.android.model.toIceServer
import io.getstream.video.android.model.toUserEventType
import io.getstream.video.android.network.NetworkStateProvider
import io.getstream.video.android.socket.SocketListener
import io.getstream.video.android.socket.SocketState
import io.getstream.video.android.socket.SocketStateService
import io.getstream.video.android.socket.VideoSocket
import io.getstream.video.android.token.CredentialsProvider
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Result
import io.getstream.video.android.utils.Success
import io.getstream.video.android.utils.enrichSFUURL
import io.getstream.video.android.utils.flatMap
import io.getstream.video.android.utils.getLatencyMeasurements
import io.getstream.video.android.utils.map
import io.getstream.video.android.utils.onError
import io.getstream.video.android.utils.onSuccess
import io.getstream.video.android.utils.toCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import stream.video.coordinator.client_v1_rpc.CreateCallInput
import stream.video.coordinator.client_v1_rpc.CreateCallRequest
import stream.video.coordinator.client_v1_rpc.GetCallEdgeServerRequest
import stream.video.coordinator.client_v1_rpc.GetCallEdgeServerResponse
import stream.video.coordinator.client_v1_rpc.GetOrCreateCallRequest
import stream.video.coordinator.client_v1_rpc.JoinCallRequest
import stream.video.coordinator.client_v1_rpc.MemberInput
import stream.video.coordinator.client_v1_rpc.SendEventRequest
import stream.video.coordinator.edge_v1.Latency
import stream.video.coordinator.edge_v1.LatencyMeasurements

/**
 * @param lifecycle The lifecycle used to observe changes in the process. // TODO - docs
 */
public class StreamVideoImpl(
    private val context: Context,
    private val scope: CoroutineScope,
    private val config: StreamVideoConfig,
    private val lifecycle: Lifecycle,
    private val loggingLevel: LoggingLevel,
    private val callCoordinatorClient: CallCoordinatorClient,
    private val credentialsProvider: CredentialsProvider,
    private val socket: VideoSocket,
    private val socketStateService: SocketStateService,
    private val userState: UserState,
    private val networkStateProvider: NetworkStateProvider
) : StreamVideo {

    private val logger = StreamLog.getLogger("Call:StreamCalls")

    private val engine = StreamCallEngineImpl(scope, config) {
        credentialsProvider.getUserCredentials()
    }

    init {
        scope.launch {
            engine.callState.collect { state ->
                when (state) {
                    is StreamCallState.Drop -> {
                        if (state.reason is DropReason.Timeout) {
                            logger.i { "[observeState] call dropped by timeout" }
                            cancelCall(state.callGuid.cid)
                        }
                    }
                    else -> { /* no-op */
                    }
                }
            }
        }
    }

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

    private var callClient: CallClient? = null

    init {
        socket.addListener(engine)
        scope.launch(Dispatchers.Main.immediate) {
            lifecycleObserver.observe()
        }

        socket.connectSocket()
    }

    /**
     * Represents the state of the currently active call.
     */
    override val callState: StateFlow<StreamCallState> = engine.callState

    /**
     * Domain - Coordinator.
     */
    override suspend fun createCall(
        type: String,
        id: String,
        participantIds: List<String>,
        ringing: Boolean
    ): Result<CallMetadata> {
        logger.d { "[createCall] type: $type, id: $id, participantIds: $participantIds" }
        engine.onCallStarting(type, id, participantIds, ringing, forcedNewCall = true)
        return callCoordinatorClient.createCall(
            CreateCallRequest(
                type = type,
                id = id,
                input = CreateCallInput(
                    members = participantIds.map {
                        MemberInput(
                            user_id = it,
                            role = "admin"
                        )
                    },
                    ring = ringing
                )
            )
        )
            .also { logger.v { "[getOrCreateCall] result: $it" } }
            .map {
                StartedCall(
                    call = it.call?.toCall() ?: error("CreateCallResponse has no call object")
                )
            }
            .onSuccess { engine.onCallStarted(it.call) }
            .onError { engine.onCallFailed(it) }
            .map { it.call }
            .also { logger.v { "[createCall] result: $it" } }
    }

    // caller: DIAL and wait answer
    override suspend fun getOrCreateCall(
        type: String,
        id: String,
        participantIds: List<String>,
        ringing: Boolean
    ): Result<CallMetadata> {
        logger.d { "[getOrCreateCall] type: $type, id: $id, participantIds: $participantIds" }
        engine.onCallStarting(type, id, participantIds, ringing, forcedNewCall = false)
        return callCoordinatorClient.getOrCreateCall(
            GetOrCreateCallRequest(
                type = type,
                id = id,
                input = CreateCallInput(
                    members = participantIds.map {
                        MemberInput(
                            user_id = it,
                            role = "admin"
                        )
                    },
                    ring = ringing
                )
            )
        )
            .also { logger.v { "[getOrCreateCall] Coordinator result: $it" } }
            .map {
                StartedCall(
                    call = it.call?.toCall() ?: error("CreateCallResponse has no call object")
                )
            }
            .onSuccess { engine.onCallStarted(it.call) }
            .onError { engine.onCallFailed(it) }
            .map { it.call }
            .also { logger.v { "[getOrCreateCall] Final result: $it" } }
    }

    // caller: JOIN after accepting incoming call by callee
    override suspend fun joinCall(call: CallMetadata): Result<JoinedCall> {
        logger.d { "[joinCallOnly] call: $call" }
        engine.onCallJoining(call)
        return joinCallInternal(call)
            .onSuccess { data -> engine.onCallJoined(data) }
            .onError { engine.onCallFailed(it) }
            .also { logger.v { "[joinCallOnly] result: $it" } }
    }

    /**
     * Once the call is set up, we can initiate the Join flow, by analyzing the latency of servers
     * and choosing the correct one.
     *
     * @param call Information about the call.
     * @return [Result] wrapper around [JoinedCall] once the correct server is chosen.
     */
    private suspend fun joinCallInternal(call: CallMetadata): Result<JoinedCall> {
        return try {
            logger.d { "[joinCallInternal] call: $call" }
            val joinResult = callCoordinatorClient.joinCall(
                JoinCallRequest(
                    id = call.id,
                    type = call.type,
                    datacenter_id = ""
                )
            )
            if (joinResult !is Success) {
                logger.e { "[joinCallInternal] failed joinResult: $joinResult" }
                return joinResult as Failure
            }
            logger.v { "[joinCallInternal] joinResult: $joinResult" }

            val latencyResults = joinResult.data.edges.associate {
                it.name to measureLatency(it.latency_url)
            }

            val selectEdgeServerResult = selectEdgeServer(
                request = GetCallEdgeServerRequest(
                    call_cid = call.cid,
                    measurements = LatencyMeasurements(measurements = latencyResults)
                )
            )
            logger.v { "[joinCallInternal] selectEdgeServerResult: $selectEdgeServerResult" }
            when (selectEdgeServerResult) {
                is Success -> {
                    socket.updateCallState(call)
                    val credentials = selectEdgeServerResult.data.credentials
                    val url = credentials?.server?.url
                    val iceServers =
                        selectEdgeServerResult.data.credentials?.ice_servers?.map { it.toIceServer() }
                            ?: emptyList()

                    Success(
                        JoinedCall(
                            call = call,
                            callUrl = enrichSFUURL(url!!),
                            userToken = credentials.token,
                            iceServers = iceServers
                        )
                    )
                }
                is Failure -> Failure(selectEdgeServerResult.error)
            }
        } catch (error: Throwable) {
            logger.e { "[joinCallInternal] failed: $error" }
            Failure(VideoError(error.message, error))
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

    // caller/callee: CREATE/JOIN meeting
    override suspend fun createAndJoinCall(
        type: String,
        id: String,
        participantIds: List<String>,
        ringing: Boolean
    ): Result<JoinedCall> {
        logger.d { "[getOrCreateAndJoinCall] type: $type, id: $id, participantIds: $participantIds" }
        engine.onCallStarting(type, id, participantIds, ringing, forcedNewCall = false)
        return callCoordinatorClient.getOrCreateCall(
            GetOrCreateCallRequest(
                type = type,
                id = id,
                input = CreateCallInput(
                    members = participantIds.map {
                        MemberInput(
                            user_id = it,
                            role = "admin"
                        )
                    },
                    ring = ringing
                )
            )
        )
            .also { logger.v { "[getOrCreateCall] Coordinator result: $it" } }
            .map {
                StartedCall(
                    call = it.call?.toCall() ?: error("CreateCallResponse has no call object")
                )
            }
            .onSuccess { engine.onCallJoining(it.call) }
            .flatMap { joinCallInternal(it.call) }
            .onSuccess { engine.onCallJoined(it) }
            .onError { engine.onCallFailed(it) }
            .also { logger.v { "[getOrCreateAndJoinCall] result: $it" } }
    }

    // callee: ACCEPT incoming call
    override suspend fun joinCall(type: String, id: String): Result<JoinedCall> {
        logger.d { "[joinCall] type: $type, id: $id" }
        return createAndJoinCall(type, id, participantIds = emptyList(), ringing = false)
            .also { logger.v { "[joinCall] result: $it" } }
    }

    // callee: SEND Accepted or Rejected
    override suspend fun sendEvent(
        callCid: String,
        eventType: CallEventType
    ): Result<Boolean> {
        logger.d { "[sendEvent] callCid: $callCid, eventType: $eventType" }
        engine.onCallEventSending(callCid, eventType)
        return callCoordinatorClient.sendUserEvent(
            SendEventRequest(call_cid = callCid, event_type = eventType.toUserEventType())
        )
            .onSuccess { engine.onCallEventSent(callCid, eventType) }
            .also { logger.v { "[sendEvent] result: $it" } }
    }

    override fun clearCallState() {
        logger.i { "[clearCallState] no args" }
        credentialsProvider.setSfuToken(null)
        socket.updateCallState(null)
        callClient?.clear()
        callClient = null
    }

    /**
     * Attempts to reconnect the socket if it's in a disconnected state and the user is available.
     */
    private fun reconnectSocket() {
        val user = userState.user.value

        if (socketStateService.state !is SocketState.Connected && user.id.isNotBlank()) {
            socket.reconnect()
        }
    }

    override fun getUser(): User = userState.user.value

    /**
     * Adds a listener to the socket which allows you to build custom behavior based on each event.
     *
     * @param socketListener The listener instance to add to a pool of event handlers.
     */
    override fun addSocketListener(socketListener: SocketListener): Unit =
        socket.addListener(socketListener)

    /**
     * Removes a listener from the socket allowing you to clean up event handling.
     *
     * @param socketListener The listener instance to be removed.
     */
    override fun removeSocketListener(socketListener: SocketListener): Unit =
        socket.removeListener(socketListener)

    /**
     * Creates an instance of the [CallClient] for the given call input, which is persisted and
     * used to communicate with the BE.
     *
     * Use it to control the track state, mute/unmute devices and listen to call events.
     *
     * @param signalUrl The URL of the server in which the call is being hosted.
     * @param userToken User's ticket to enter the call.
     * @param iceServers Servers required to appropriately connect to the call and receive tracks.
     * @param credentialsProvider Contains information about the user required for the Call state.
     * @return An instance of [CallClient] ready to connect to a call. Make sure to call
     * [CallClient.connectToCall] when you're ready to fully join a call.
     */
    override fun createCallClient(
        signalUrl: String,
        userToken: String,
        iceServers: List<IceServer>,
        credentialsProvider: CredentialsProvider
    ): CallClient {
        credentialsProvider.setSfuToken(userToken)
        val builder = CallClientBuilder(
            context = context,
            credentialsProvider = credentialsProvider,
            networkStateProvider = networkStateProvider,
            signalUrl = signalUrl,
            iceServers = iceServers
        )

        builder.loggingLevel(loggingLevel)

        val client = builder.build()
        this.callClient = client

        return client
    }

    /**
     *
     * @return An instance of the [CallClient] if it currently exists (the user is in a call).
     */
    override fun getActiveCallClient(): CallClient? {
        return this.callClient
    }

    override suspend fun acceptCall(type: String, id: String): Result<JoinedCall> {
        logger.d { "[acceptCall] type: $type, id: $id" }
        return joinCall(type = type, id = id)
            .flatMap { joined ->
                sendEvent(
                    callCid = joined.call.cid,
                    eventType = CallEventType.ACCEPTED
                ).map { joined }
            }.also { logger.v { "[acceptCall] result: $it" } }
    }

    override suspend fun rejectCall(cid: String): Result<Boolean> {
        logger.d { "[rejectCall] cid: $cid" }
        return sendEvent(callCid = cid, CallEventType.REJECTED)
    }

    override suspend fun cancelCall(cid: String): Result<Boolean> {
        logger.d { "[cancelCall] cid: $cid" }
        return sendEvent(callCid = cid, CallEventType.CANCELLED)
    }
}
