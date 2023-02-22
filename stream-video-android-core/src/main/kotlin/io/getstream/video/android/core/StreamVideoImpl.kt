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

package io.getstream.video.android.core

import android.content.Context
import androidx.lifecycle.Lifecycle
import io.getstream.log.taggedLogger
import io.getstream.video.android.core.call.CallClient
import io.getstream.video.android.core.call.builder.CallClientBuilder
import io.getstream.video.android.core.coordinator.CallCoordinatorClient
import io.getstream.video.android.core.coordinator.state.UserState
import io.getstream.video.android.core.engine.StreamCallEngine
import io.getstream.video.android.core.engine.adapter.CoordinatorSocketListenerAdapter
import io.getstream.video.android.core.errors.VideoError
import io.getstream.video.android.core.events.CallCreatedEvent
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.lifecycle.LifecycleHandler
import io.getstream.video.android.core.lifecycle.internal.StreamLifecycleObserver
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.CallEventType
import io.getstream.video.android.core.model.CallMetadata
import io.getstream.video.android.core.model.Device
import io.getstream.video.android.core.model.IceServer
import io.getstream.video.android.core.model.JoinedCall
import io.getstream.video.android.core.model.SfuToken
import io.getstream.video.android.core.model.StartedCall
import io.getstream.video.android.core.model.StreamCallCid
import io.getstream.video.android.core.model.StreamCallGuid
import io.getstream.video.android.core.model.StreamCallKind
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.mapper.toMetadata
import io.getstream.video.android.core.model.mapper.toTypeAndId
import io.getstream.video.android.core.model.state.DropReason
import io.getstream.video.android.core.model.state.StreamCallState
import io.getstream.video.android.core.model.toIceServer
import io.getstream.video.android.core.model.toInfo
import io.getstream.video.android.core.socket.SocketListener
import io.getstream.video.android.core.socket.SocketStateService
import io.getstream.video.android.core.socket.VideoSocket
import io.getstream.video.android.core.socket.internal.SocketState
import io.getstream.video.android.core.user.UserPreferences
import io.getstream.video.android.core.user.UserPreferencesManager
import io.getstream.video.android.core.utils.Failure
import io.getstream.video.android.core.utils.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.utils.Result
import io.getstream.video.android.core.utils.Success
import io.getstream.video.android.core.utils.flatMap
import io.getstream.video.android.core.utils.getLatencyMeasurements
import io.getstream.video.android.core.utils.map
import io.getstream.video.android.core.utils.onError
import io.getstream.video.android.core.utils.onSuccess
import io.getstream.video.android.core.utils.toCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openapitools.client.models.CallRequest
import org.openapitools.client.models.GetCallEdgeServerRequest
import org.openapitools.client.models.GetCallEdgeServerResponse
import org.openapitools.client.models.GetOrCreateCallRequest
import org.openapitools.client.models.MemberRequest
import org.openapitools.client.models.SendEventRequest
import stream.video.coordinator.client_v1_rpc.CreateDeviceRequest
import stream.video.coordinator.client_v1_rpc.DeleteDeviceRequest
import stream.video.coordinator.push_v1.DeviceInput

/**
 * @param lifecycle The lifecycle used to observe changes in the process. // TODO - docs
 */
internal class StreamVideoImpl(
    private val context: Context,
    private val scope: CoroutineScope,
    override val config: StreamVideoConfig,
    private val engine: StreamCallEngine,
    private val lifecycle: Lifecycle,
    private val loggingLevel: LoggingLevel,
    private val callCoordinatorClient: CallCoordinatorClient,
    private val preferences: UserPreferences,
    private val socket: VideoSocket,
    private val socketStateService: SocketStateService,
    private val userState: UserState,
    private val networkStateProvider: NetworkStateProvider,
) : StreamVideo {

    private val logger by taggedLogger("Call:StreamVideo")

    /**
     * Observes the app lifecycle and attempts to reconnect/release the socket connection.
     */
    private val lifecycleObserver =
        StreamLifecycleObserver(
            lifecycle,
            object : LifecycleHandler {
                override fun resume() = reconnectSocket()
                override fun stopped() {
                    socket.releaseConnection()
                }
            }
        )

    private val callClientHolder = MutableStateFlow<CallClient?>(null)

    init {
        observeState()
        addSocketListener(CoordinatorSocketListenerAdapter(engine))
        scope.launch(Dispatchers.Main.immediate) {
            lifecycleObserver.observe()
        }

        socket.connectSocket()
    }

    private fun observeState() {
        scope.launch {
            engine.callState.collect { state ->
                when (state) {
                    is StreamCallState.Drop -> if (config.cancelOnTimeout && state.reason is DropReason.Timeout) {
                        logger.i { "[observeState] call dropped by timeout" }
                        cancelCall(state.callGuid.cid)
                    }
                    is StreamCallState.Idle -> clearCallState()
                    is StreamCallState.Joined -> if (config.createCallClientInternally) {
                        logger.i { "[observeState] caller joins a call: $state" }
                        createCallClient(
                            callGuid = state.callGuid,
                            signalUrl = state.callUrl,
                            sfuToken = state.sfuToken,
                            iceServers = state.iceServers,
                        )
                    }
                    is StreamCallState.Outgoing -> if (config.joinOnAcceptedByCallee && state.acceptedByCallee) {
                        logger.i { "[observeState] caller joins a call: $state" }
                        joinCall(state.toMetadata())
                    }
                    else -> { /* no-op */
                    }
                }
            }
        }
    }

    /**
     * Represents the state of the currently active call.
     */
    override val callState: StateFlow<StreamCallState> = engine.callState

    /**
     * Create a device that will be used to receive push notifications.
     *
     * @param token The Token obtained from the selected push provider.
     * @param pushProvider The selected push provider.
     *
     * @return [Result] containing the [Device].
     */
    override suspend fun createDevice(token: String, pushProvider: String): Result<Device> {
        logger.d { "[createDevice] token: $token, pushProvider: $pushProvider" }
        return callCoordinatorClient.createDevice(
            CreateDeviceRequest(
                DeviceInput(
                    id = token,
                    push_provider_id = pushProvider
                )
            )
        )
            .also { logger.v { "[createDevice] result: $it" } }
            .map {
                Device(
                    token = it.device?.id ?: error("CreateDeviceResponse has no device object "),
                    pushProvider = it.device.push_provider_name
                )
            }.also { storeDevice(it) }
    }

    private fun storeDevice(result: Result<Device>) {
        if (result is Success) {
            logger.d { "[storeDevice] device: ${result.data}" }
            val device = result.data
            val preferences = UserPreferencesManager.initialize(context)

            preferences.storeDevice(device)
        }
    }

    /**
     * Remove a device used to receive push notifications.
     *
     * @param id The ID of the device, previously provided by [createDevice].
     * @return Result if the operation was successful or not.
     */
    override suspend fun deleteDevice(id: String): Result<Unit> {
        logger.d { "[deleteDevice] id: $id" }
        return callCoordinatorClient.deleteDevice(
            DeleteDeviceRequest(id = id)
        ).also { logger.v { "[deleteDevice] result: $it" } }
    }

    override fun removeDevices(devices: List<Device>) {
        scope.launch {
            val operations = devices.map {
                async { deleteDevice(it.token) }
            }

            operations.awaitAll()
        }
    }

    /**
     * Domain - Coordinator.
     */
    // caller: DIAL and wait answer
    override suspend fun getOrCreateCall(
        type: String,
        id: String,
        participantIds: List<String>,
        ring: Boolean
    ): Result<CallMetadata> = withContext(scope.coroutineContext) {
        logger.d { "[getOrCreateCall] type: $type, id: $id, participantIds: $participantIds" }
        engine.onCallStarting(type, id, participantIds, ring, forcedNewCall = false)
        callCoordinatorClient.getOrCreateCall(
            type = type,
            id = id,
            getOrCreateCallRequest = GetOrCreateCallRequest(
                data = CallRequest(
                    members = participantIds.map {
                        MemberRequest(
                            userId = it,
                            role = "admin"
                        )
                    },
                ),
                ring = ring
            )
        )
            .also { logger.v { "[getOrCreateCall] Coordinator result: $it" } }
            .map { response -> StartedCall(call = response.toCall(StreamCallKind.fromRinging(ring))) }
            .onSuccess { engine.onCallStarted(it.call) }
            .onError { engine.onCallFailed(it) }
            .map { it.call }
            .also { logger.v { "[getOrCreateCall] Final result: $it" } }
    }

    // caller: JOIN after accepting incoming call by callee
    override suspend fun joinCall(call: CallMetadata): Result<JoinedCall> =
        withContext(scope.coroutineContext) {
            logger.d { "[joinCallOnly] call: $call" }
            engine.onCallJoining(call)
            joinCallInternal(call)
                .onSuccess { data -> engine.onCallJoined(data) }
                .onError { engine.onCallFailed(it) }
                .also { logger.v { "[joinCallOnly] result: $it" } }
        }

    /**
     * Used to invite new users/members to an existing call.
     *
     * @param users The users to invite.
     * @param cid The channel ID.
     * @return [Result] if the operation was successful or not.
     */
    override suspend fun inviteUsers(users: List<User>, cid: StreamCallCid): Result<Unit> {
        logger.d { "[inviteUsers] users: $users" }

        return callCoordinatorClient.inviteUsers(users, cid)
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
                id = call.id,
                type = call.type,
                connectionId = socket.getConnectionId(),
                request = GetOrCreateCallRequest()
            )
            if (joinResult !is Success) {
                logger.e { "[joinCallInternal] failed joinResult: $joinResult" }
                return joinResult as Failure
            }
            logger.v { "[joinCallInternal] joinResult: $joinResult" }

            val validEdges = joinResult.data.edges.filter {
                it.latencyUrl.isNotBlank() && it.name.isNotBlank()
            }

            val latencyResults = validEdges.associate {
                it.name to measureLatency(it.latencyUrl)
            }
            logger.v { "[joinCallInternal] latencyResults: $latencyResults" }
            val selectEdgeServerResult = selectEdgeServer(
                type = call.type,
                id = call.id,
                request = GetCallEdgeServerRequest(
                    latencyMeasurements = latencyResults
                )
            )
            logger.v { "[joinCallInternal] selectEdgeServerResult: $selectEdgeServerResult" }
            when (selectEdgeServerResult) {
                is Success -> {
                    socket.updateCallState(call)
                    val credentials = selectEdgeServerResult.data.credentials
                    val url = credentials.server.url
                    val iceServers =
                        selectEdgeServerResult
                            .data
                            .credentials
                            .iceServers
                            .map { it.toIceServer() }

                    preferences.storeSfuToken(credentials.token)

                    Success(
                        JoinedCall(
                            call = call,
                            callUrl = url,
                            sfuToken = credentials.token,
                            iceServers = iceServers
                        )
                    )
                }
                is Failure -> Failure(selectEdgeServerResult.error)
            }
        } catch (error: Throwable) {
            logger.e(error) { "[joinCallInternal] failed: $error" }
            Failure(VideoError(error.message, error))
        }
    }

    /**
     * Measures and prepares the latency which describes how much time it takes to ping the server.
     *
     * @param edgeUrl The edge we want to measure.
     *
     * @return [List] of [Float] values which represent measurements from ping connections.
     */
    // TODO - measure latencies in the following way:
    // 5x links/servers in parallel, 3s timeout, 3x retries
    private suspend fun measureLatency(edgeUrl: String): List<Float> = withContext(Dispatchers.IO) {
        getLatencyMeasurements(edgeUrl)
    }

    /**
     * @see CallCoordinatorClient.selectEdgeServer for details.
     */
    private suspend fun selectEdgeServer(
        type: String,
        id: String,
        request: GetCallEdgeServerRequest
    ): Result<GetCallEdgeServerResponse> {
        return callCoordinatorClient.selectEdgeServer(id, type, request)
    }

    // caller/callee: CREATE/JOIN meeting or ACCEPT call with no participants or ringing
    override suspend fun joinCall(
        type: String,
        id: String,
        participantIds: List<String>,
        ring: Boolean
    ): Result<JoinedCall> = withContext(scope.coroutineContext) {
        logger.d { "[getOrCreateAndJoinCall] type: $type, id: $id, participantIds: $participantIds" }
        engine.onCallStarting(type, id, participantIds, ring, forcedNewCall = false)
        callCoordinatorClient.getOrCreateCall(
            type = type,
            id = id,
            getOrCreateCallRequest = GetOrCreateCallRequest(
                data = CallRequest(
                    members = participantIds.map {
                        MemberRequest(
                            userId = it,
                            role = "admin"
                        )
                    },
                ),
                ring = ring
            )
        )
            .also { logger.v { "[getOrCreateCall] Coordinator result: $it" } }
            .map { response -> StartedCall(call = response.toCall(StreamCallKind.fromRinging(ring))) }
            .onSuccess { engine.onCallJoining(it.call) }
            .flatMap { joinCallInternal(it.call) }
            .onSuccess { engine.onCallJoined(it) }
            .onError { engine.onCallFailed(it) }
            .also { logger.v { "[getOrCreateAndJoinCall] result: $it" } }
    }

    // callee: SEND Accepted or Rejected
    override suspend fun sendEvent(
        callCid: String,
        eventType: CallEventType
    ): Result<Boolean> {
        logger.d { "[sendEvent] callCid: $callCid, eventType: $eventType" }
        engine.onCallEventSending(callCid, eventType)
        val (type, id) = callCid.toTypeAndId()

        return callCoordinatorClient.sendUserEvent(
            id = id,
            type = type,
            sendEventRequest = SendEventRequest(type = eventType.eventType)
        )
            .onSuccess { engine.onCallEventSent(callCid, eventType) }
            .also { logger.v { "[sendEvent] result: $it" } }
    }

    override suspend fun sendCustomEvent(
        callCid: String,
        dataJson: Map<String, Any>,
        eventType: String
    ): Result<Boolean> {
        logger.d { "[sendCustomEvent] callCid: $callCid, dataJson: $dataJson, eventType: $eventType" }
        val (type, id) = callCid.toTypeAndId()

        return callCoordinatorClient.sendUserEvent(
            id = id,
            type = type,
            sendEventRequest = SendEventRequest(custom = dataJson, type = eventType)
        ).also { logger.v { "[sendCustomEvent] result: $it" } }
    }

    override fun clearCallState() {
        logger.i { "[clearCallState] no args" }
        preferences.storeSfuToken(null)
        socket.updateCallState(null)
        callClientHolder.value?.clear()
        callClientHolder.value = null
    }

    /**
     * Logs out the user by clearing the credentials preferences, unregistering any push devices
     * and clearing the call state.
     */
    override fun logOut() {
        val preferences = UserPreferencesManager.getPreferences()

        clearCallState()
        removeDevices(preferences.getDevices())
        preferences.clear()
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
     * @param socketListener The listener instance tocon be removed.
     */
    override fun removeSocketListener(socketListener: SocketListener): Unit =
        socket.removeListener(socketListener)

    /**
     * Creates an instance of the [CallClient] for the given call input, which is persisted and
     * used to communicate with the BE.
     *
     * Use it to control the track state, mute/unmute devices and listen to call events.
     *
     * @param callGuid The GUID of the Call, containing the ID and its type.
     * @param signalUrl The URL of the server in which the call is being hosted.
     * @param sfuToken User's ticket to enter the call.
     * @param iceServers Servers required to appropriately connect to the call and receive tracks.
     *
     * @return An instance of [CallClient] ready to connect to a call. Make sure to call
     * [CallClient.connectToCall] when you're ready to fully join a call.
     */
    override fun createCallClient(
        callGuid: StreamCallGuid,
        signalUrl: String,
        sfuToken: SfuToken,
        iceServers: List<IceServer>
    ): CallClient {
        logger.i { "[createCallClient] signalUrl: $signalUrl, sfuToken: $sfuToken, iceServers: $iceServers" }

        return CallClientBuilder(
            context = context,
            coordinatorClient = callCoordinatorClient,
            preferences = preferences,
            networkStateProvider = networkStateProvider,
            callEngine = engine,
            signalUrl = signalUrl,
            iceServers = iceServers,
            callGuid = callGuid
        ).apply {
            loggingLevel(loggingLevel)
        }.build().also {
            callClientHolder.value = it
        }
    }

    /**
     *
     * @return An instance of the [CallClient] if it currently exists (the user is in a call).
     */
    override fun getActiveCallClient(): CallClient? {
        return callClientHolder.value
    }

    /**
     *
     * @return An instance of the [CallClient] if it currently exists (the user is in a call).
     */
    override suspend fun awaitCallClient(): CallClient = withContext(scope.coroutineContext) {
        callClientHolder.first { it != null } ?: error("callClient must not be null")
    }

    override suspend fun acceptCall(cid: StreamCallCid): Result<JoinedCall> =
        withContext(scope.coroutineContext) {
            try {
                logger.d { "[acceptCall] cid: $cid" }
                val (type, id) = cid.toTypeAndId()
                sendEvent(
                    callCid = cid,
                    eventType = CallEventType.ACCEPTED
                ).flatMap {
                    joinCall(type, id)
                }.also {
                    logger.v { "[acceptCall] result: $it" }
                }
            } catch (e: Throwable) {
                logger.e { "[acceptCall] failed: $e" }
                Failure(VideoError(e.message, e))
            }
        }

    override suspend fun rejectCall(cid: StreamCallCid): Result<Boolean> =
        withContext(scope.coroutineContext) {
            logger.d { "[rejectCall] cid: $cid" }
            sendEvent(callCid = cid, CallEventType.REJECTED)
        }

    override suspend fun cancelCall(cid: StreamCallCid): Result<Boolean> =
        withContext(scope.coroutineContext) {
            logger.d { "[cancelCall] cid: $cid" }
            sendEvent(callCid = cid, CallEventType.CANCELLED)
        }

    override suspend fun handlePushMessage(payload: Map<String, Any>): Result<Unit> =
        withContext(scope.coroutineContext) {
            val callCid = payload[INTENT_EXTRA_CALL_CID] as? String
                ?: return@withContext Failure(VideoError("Missing Call CID!"))

            val (type, id) = callCid.toTypeAndId()

            when (val result = getOrCreateCall(type, id, emptyList(), false)) {
                is Success -> {
                    val callMetadata = result.data

                    val event = CallCreatedEvent(
                        callCid = callMetadata.cid,
                        ringing = true,
                        users = callMetadata.users,
                        callInfo = callMetadata.toInfo(),
                        callDetails = callMetadata.callDetails
                    )

                    engine.onCoordinatorEvent(event)
                    Success(Unit)
                }
                is Failure -> result
            }
        }
}
