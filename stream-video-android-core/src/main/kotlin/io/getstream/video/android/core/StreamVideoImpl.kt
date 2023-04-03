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
import io.getstream.video.android.core.errors.VideoBackendError
import io.getstream.video.android.core.errors.VideoError
import io.getstream.video.android.core.events.CallCreatedEvent
import io.getstream.video.android.core.events.VideoEvent
import io.getstream.video.android.core.internal.module.ConnectionModule
import io.getstream.video.android.core.lifecycle.LifecycleHandler
import io.getstream.video.android.core.lifecycle.internal.StreamLifecycleObserver
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.CallEventType
import io.getstream.video.android.core.model.CallMetadata
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.model.Device
import io.getstream.video.android.core.model.EdgeData
import io.getstream.video.android.core.model.JoinedCall
import io.getstream.video.android.core.model.MuteUsersData
import io.getstream.video.android.core.model.QueriedCalls
import io.getstream.video.android.core.model.QueryCallsData
import io.getstream.video.android.core.model.QueryMembersData
import io.getstream.video.android.core.model.SendReactionData
import io.getstream.video.android.core.model.StartedCall
import io.getstream.video.android.core.model.StreamCallKind
import io.getstream.video.android.core.model.UpdateUserPermissionsData
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.mapper.toTypeAndId
import io.getstream.video.android.core.model.toIceServer
import io.getstream.video.android.core.model.toInfo
import io.getstream.video.android.core.model.toRequest
import io.getstream.video.android.core.socket.SocketListener
import io.getstream.video.android.core.socket.internal.SocketState
import io.getstream.video.android.core.socket.internal.VideoSocketImpl
import io.getstream.video.android.core.user.UserPreferencesManager
import io.getstream.video.android.core.utils.*
import io.getstream.video.android.core.utils.toCall
import io.getstream.video.android.core.utils.toCallUser
import io.getstream.video.android.core.utils.toEdge
import io.getstream.video.android.core.utils.toQueriedCalls
import io.getstream.video.android.core.utils.toRecording
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.openapitools.client.models.*
import retrofit2.HttpException
import stream.video.coordinator.client_v1_rpc.CreateDeviceRequest
import stream.video.coordinator.client_v1_rpc.DeleteDeviceRequest
import stream.video.coordinator.client_v1_rpc.MemberInput
import stream.video.coordinator.client_v1_rpc.UpsertCallMembersRequest
import stream.video.coordinator.push_v1.DeviceInput
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class EventSubscription(
    public val listener: VideoEventListener<VideoEvent>,
    public val filter: ((VideoEvent) -> Boolean)? = null,
) {
    var isDisposed: Boolean = false

    fun dispose() {
        isDisposed = true
    }
}

/**
 * @param lifecycle The lifecycle used to observe changes in the process. // TODO - docs
 */
internal class StreamVideoImpl internal constructor(
    override val context: Context,
    internal val scope: CoroutineScope,
    override val user: User,
    private val lifecycle: Lifecycle,
    private val loggingLevel: LoggingLevel,
    internal val connectionModule: ConnectionModule,
) : StreamVideo, SocketListener {

    override fun onEvent(event: VideoEvent) {
        // TODO: maybe merge fire event into this?
        println("engine eventlistener received an event: $event")
        fireEvent(event)
        nextEventContinuation?.let { continuation ->
            if (!nextEventCompleted) {
                continuation.resume(value=event)
            }
            nextEventCompleted = true
        }
    }

    override val state = ClientState()
    private val logger by taggedLogger("Call:StreamVideo")
    private var subscriptions = mutableSetOf<EventSubscription>()
    private var calls = mutableMapOf<String, Call2>()

    // caller: JOIN after accepting incoming call by callee
    /**
     * @see StreamVideo.joinCall
     */
    override suspend fun joinCall(call: CallMetadata): Result<JoinedCall> =
        withContext(scope.coroutineContext) {
            logger.d { "[joinCallOnly] call: $call" }
            // TODO: engine.onCallJoining(call)
            joinCallInternal(call)
                // .onSuccess { data -> engine.onCallJoined(data) }
                // .onError { engine.onCallFailed(it) }
                .also { logger.v { "[joinCallOnly] result: $it" } }
        }

    /**
     * @see StreamVideo.createDevice
     */
    override suspend fun createDevice(token: String, pushProvider: String): Result<Device> {
        logger.d { "[createDevice] token: $token, pushProvider: $pushProvider" }
        return wrapAPICall {
            val deviceResponse = connectionModule.oldService.createDevice(
                CreateDeviceRequest(
                    DeviceInput(
                        id = token,
                        push_provider_id = pushProvider
                    )
                )
            )
            val device = Device(
                token = deviceResponse.device?.id ?: error("CreateDeviceResponse has no device object "),
                pushProvider = deviceResponse.device.push_provider_name
            )
            storeDevice(device)
            device
        }
    }

    /**
     * Ensure that every API call runs on the IO dispatcher and has correct error handling
     */
    internal suspend fun <T : Any> wrapAPICall(apiCall: suspend () -> T): Result<T> {
        return withContext(scope.coroutineContext) {
            try {
                Success(apiCall())
            } catch (e: HttpException) {
                parseError(e)
            }
        }
    }

    /**
     * @see StreamVideo.updateCall
     */
    override suspend fun updateCall(
        type: String,
        id: String,
        custom: Map<String, Any>,
    ): Result<UpdateCallResponse> {
        logger.d { "[updateCall] type: $type, id: $id, participantIds: $custom" }
        return wrapAPICall {
            connectionModule.videoCallsApi.updateCall(
                type = type,
                id = id,
                updateCallRequest = UpdateCallRequest(
                    custom = custom,
                    settingsOverride = CallSettingsRequest()
                )
            )
        }
    }

    internal fun parseError(e: HttpException): Failure {
        val errorBytes = e.response()?.errorBody()?.bytes()
        val error = errorBytes?.let {
            val errorBody = String(it, Charsets.UTF_8)
            val format = Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }
            format.decodeFromString<VideoBackendError>(errorBody)
        }
        return Failure(VideoError())
    }

    public override fun subscribeFor(
        vararg eventTypes: Class<out VideoEvent>,
        listener: VideoEventListener<VideoEvent>,
    ): EventSubscription {
        val filter = { event: VideoEvent ->
            eventTypes.any { type -> type.isInstance(event) }
        }
        val sub = EventSubscription(listener, filter)
        subscriptions.add(sub)
        return sub
    }

    public override fun subscribe(
        listener: VideoEventListener<VideoEvent>
    ): EventSubscription {
        val sub = EventSubscription(listener)
        subscriptions.add(sub)
        return sub
    }

    /**
     * Observes the app lifecycle and attempts to reconnect/release the socket connection.
     */
    private val lifecycleObserver =
        StreamLifecycleObserver(
            lifecycle,
            object : LifecycleHandler {
                override fun resume() = reconnectSocket()
                override fun stopped() {
                    connectionModule.coordinatorSocket.releaseConnection()
                }
            }
        )

    private val callClientHolder = MutableStateFlow<CallClient?>(null)

    init {
        observeState()
        // addSocketListener(CoordinatorSocketListenerAdapter(engine))
        scope.launch(Dispatchers.Main.immediate) {
            lifecycleObserver.observe()
        }

        // listen to socket events
        connectionModule.coordinatorSocket.addListener(this)

        // TODO: Find the event listener


        // TODO: Make this optional
        runBlocking(scope.coroutineContext) {
            val socketImpl = connectionModule.coordinatorSocket as VideoSocketImpl
            val result = socketImpl.connect()
            println("Runblocking socket connect, $result")
        }
    }

    private fun observeState() {
        scope.launch {
//            engine.callState.collect { state ->
//                when (state) {
//                    is StreamCallState.Drop -> if (config.cancelOnTimeout && state.reason is DropReason.Timeout) {
//                        logger.i { "[observeState] call dropped by timeout" }
//                        cancelCall(state.callGuid.type, state.callGuid.id)
//                    }
//                    is StreamCallState.Idle -> clearCallState()
//                    is StreamCallState.Joined -> {
//                        logger.i { "[observeState] caller joins a call: $state" }
//                        createCallClient(
//                            callGuid = state.callGuid,
//                            signalUrl = state.callUrl,
//                            sfuToken = state.sfuToken,
//                            iceServers = state.iceServers,
//                        )
//                    }
//                    is StreamCallState.Outgoing -> if (config.joinOnAcceptedByCallee && state.acceptedByCallee) {
//                        logger.i { "[observeState] caller joins a call: $state" }
//                        joinCall(state.toMetadata())
//                    }
//                    else -> { /* no-op */
//                    }
//                }
//            }
        }
    }

    private fun storeDevice(device: Device) {
        logger.d { "[storeDevice] device: device" }
        val preferences = UserPreferencesManager.initialize(context)

        preferences.storeDevice(device)
    }

    /**
     * @see StreamVideo.deleteDevice
     */
    override suspend fun deleteDevice(id: String): Result<Unit> {
        logger.d { "[deleteDevice] id: $id" }

        val request = DeleteDeviceRequest(id = id)
        return wrapAPICall { connectionModule.oldService.deleteDevice(request) }
    }

    /**
     * @see StreamVideo.removeDevices
     */
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

    /**
     * Internal function that fires the event. It starts by updating client state and call state
     * After that it loops over the subscriptions and calls their listener
     */
    internal fun fireEvent(event: VideoEvent, cid: String="") {

        // update state for the client
        state.handleEvent(event)

        // update state for the calls. calls handle updating participants and members
        val selectedCid = cid.ifEmpty { event.callCid }
        if (selectedCid.isNotEmpty()) {
            calls[selectedCid]?.let {
                it.state.handleEvent(event)
            }
        }

        // fire event handlers
        subscriptions.forEach { sub ->
            if (!sub.isDisposed) {
                // subs without filters should always fire
                if (sub.filter == null) {
                    sub.listener.onEvent(event)
                }

                // if there is a filter, check it and fire if it matches
                sub.filter?.let {
                    if (it.invoke(event)) {
                        sub.listener.onEvent(event)
                    }
                }
            }
        }
    }

    // caller: DIAL and wait answer
    /**
     * @see StreamVideo.getOrCreateCall
     */
    override suspend fun getOrCreateCall(
        type: String,
        id: String,
        participantIds: List<String>,
        ring: Boolean
    ): Result<CallMetadata> = withContext(scope.coroutineContext) {
        logger.d { "[getOrCreateCall] type: $type, id: $id, participantIds: $participantIds" }
        // engine.onCallStarting(type, id, participantIds, ring, forcedNewCall = false)

        try {
            Success(
                connectionModule.videoCallsApi.getOrCreateCall(
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
            )
                .also { logger.v { "[getOrCreateCall] Coordinator result: $it" } }
                .map { response -> StartedCall(call = response.toCall(StreamCallKind.fromRinging(ring))) }
//                .onSuccess { engine.onCallStarted(it.call) }
//                .onError { engine.onCallFailed(it) }
                .map { it.call }
                .also { logger.v { "[getOrCreateCall] Final result: $it" } }
        } catch (e: HttpException) {
            parseError(e)
        }
    }

    /**
     * @see StreamVideo.inviteUsers
     */
    override suspend fun inviteUsers(type: String, id: String, users: List<User>): Result<Unit> {
        logger.d { "[inviteUsers] users: $users" }

        return wrapAPICall {
            connectionModule.oldService.upsertCallMembers(
                UpsertCallMembersRequest(
                    call_cid = "$type:$id",
                    members = users.map { user ->
                        MemberInput(
                            user_id = user.id, role = user.role
                        )
                    }
                )
            )
        }
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

            val joinResult = wrapAPICall {
                connectionModule.videoCallsApi.joinCallTypeId0(
                    id = call.id,
                    type = call.type,
                    connectionId = connectionModule.coordinatorSocket.getConnectionId(),
                    joinCallRequest = JoinCallRequest()
                )
            }
            if (joinResult !is Success) {
                logger.e { "[joinCallInternal] failed joinResult: $joinResult" }
                return joinResult as Failure
            }
            logger.v { "[joinCallInternal] joinResult: $joinResult" }

            val validEdges = joinResult.data.edges.filter {
                it.latencyUrl.isNotBlank() && it.name.isNotBlank()
            }

            val latencyResults = measureLatency(validEdges.map { it.latencyUrl })
            logger.v { "[joinCallInternal] latencyResults: $latencyResults" }
            val selectEdgeServerResult = selectEdgeServer(
                type = call.type,
                id = call.id,
                request = GetCallEdgeServerRequest(
                    latencyMeasurements = latencyResults.associate { it.latencyUrl to it.measurements }
                )
            )
            logger.v { "[joinCallInternal] selectEdgeServerResult: $selectEdgeServerResult" }
            when (selectEdgeServerResult) {
                is Success -> {
                    connectionModule.coordinatorSocket.updateCallState(call)
                    val credentials = selectEdgeServerResult.data.credentials
                    val url = credentials.server.url
                    val iceServers =
                        selectEdgeServerResult
                            .data
                            .credentials
                            .iceServers
                            .map { it.toIceServer() }

                    connectionModule.preferences.storeSfuToken(credentials.token)

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

    internal suspend fun measureLatency(edgeUrls: List<String>): List<LatencyResult> = withContext(scope.coroutineContext) {
        val jobs = edgeUrls.map { async {
            getLatencyMeasurementsOKHttp(it)
        } }
        val results = jobs.awaitAll().sortedBy { it.average }
        results
    }

    /**
     * @see CallCoordinatorClient.selectEdgeServer for details.
     */
    public override suspend fun selectEdgeServer(
        type: String,
        id: String,
        request: GetCallEdgeServerRequest
    ): Result<GetCallEdgeServerResponse> {
        return wrapAPICall {
            connectionModule.videoCallsApi.getCallEdgeServer(
                type = type,
                id = id,
                getCallEdgeServerRequest = request
            )
        }
    }
    override suspend fun joinCall(type: String, id: String): Result<JoinCallResponse> {
        val joinCallRequest = JoinCallRequest()
        return wrapAPICall { connectionModule.videoCallsApi.joinCallTypeId0(type, id, joinCallRequest, connectionModule.coordinatorSocket.getConnectionId()) }
    }

    suspend fun joinCallOld(
        type: String,
        id: String,
        participantIds: List<String>,
        ring: Boolean
    ): Result<JoinedCall> {
        logger.d { "[getOrCreateAndJoinCall] type: $type, id: $id, participantIds: $participantIds" }

        // TODO: engine.onCallStarting(type, id, participantIds, ring, forcedNewCall = false)
        // TODO: remove this, join call automatically gets the call, no need to do it twice
//        getOrCreateCall(
//            type = type,
//            id = id,
//            request = GetOrCreateCallRequest(
//                data = CallRequest(
//                    members = participantIds.map {
//                        MemberRequest(
//                            userId = it,
//                            role = "admin"
//                        )
//                    },
//                ),
//                ring = ring
//            )
//        )
//            .also { logger.v { "[getOrCreateCall] Coordinator result: $it" } }
//            .map { response -> StartedCall(call = response.toCall(StreamCallKind.fromRinging(ring))) }
//            .onSuccess { engine.onCallJoining(it.call) }
//            .flatMap { joinCallInternal(it.call) }
//            .onSuccess { engine.onCallJoined(it) }
//            .onError { engine.onCallFailed(it) }
//            .also { logger.v { "[getOrCreateAndJoinCall] result: $it" } }
        return wrapAPICall {
            // TODO: FiXME
            JoinedCall(CallMetadata.empty(), "", "", emptyList())
        }
    }

    // callee: SEND Accepted or Rejected
    /**
     * @see StreamVideo.sendEvent
     */
    override suspend fun sendEvent(
        type: String,
        id: String,
        eventType: CallEventType
    ): Result<SendEventResponse> {
        logger.d { "[sendEvent] callCid: $type:$id, eventType: $eventType" }
        val callCid = "$type:$id"

        return wrapAPICall {
            connectionModule.eventsApi.sendEvent(type, id, SendEventRequest(type = eventType.eventType)).also {
                // engine.onCallEventSent(callCid, eventType)
            }
        }
    }

    /**
     * @see StreamVideo.sendCustomEvent
     */
    override suspend fun sendCustomEvent(
        type: String,
        id: String,
        dataJson: Map<String, Any>,
        eventType: String
    ): Result<SendEventResponse> {
        val callCid = "$type:$id"
        logger.d { "[sendCustomEvent] callCid: $callCid, dataJson: $dataJson, eventType: $eventType" }
        val (type, id) = callCid.toTypeAndId()

        return wrapAPICall {
            connectionModule.eventsApi.sendEvent(type, id, SendEventRequest(custom = dataJson, type = eventType))
        }
    }

    /**
     * @see StreamVideo.queryMembers
     */
    override suspend fun queryMembers(
        type: String,
        id: String,
        queryMembersData: QueryMembersData
    ): Result<List<CallUser>> {
        logger.d { "[queryMembers] callCid: $type:$id, queryMembersData: $queryMembersData" }

        return wrapAPICall { connectionModule.videoCallsApi.queryMembers(queryMembersData.toRequest(id, type)).members.map { it.toCallUser() } }
    }

    /**
     * @see StreamVideo.blockUser
     */
    override suspend fun blockUser(type: String, id: String, userId: String): Result<Unit> {
        logger.d { "[blockUser] callCid: $type:$id, userId: $userId" }

        return wrapAPICall { connectionModule.videoCallsApi.blockUser(type, id, BlockUserRequest(userId)) }
    }

    /**
     * @see StreamVideo.unblockUser
     */
    override suspend fun unblockUser(type: String, id: String, userId: String): Result<Unit> {
        logger.d { "[unblockUser] callCid: $type:$id, userId: $userId" }

        return wrapAPICall { connectionModule.videoCallsApi.unblockUser(type, id, UnblockUserRequest(userId)) }
    }

    /**
     * @see StreamVideo.endCall
     */
    override suspend fun endCall(type: String, id: String): Result<Unit> {
        return wrapAPICall { connectionModule.videoCallsApi.endCall(type, id) }
    }

    /**
     * @see StreamVideo.goLive
     */
    override suspend fun goLive(type: String, id: String): Result<GoLiveResponse> {
        logger.d { "[goLive] callCid: $type:$id" }

        return wrapAPICall { connectionModule.videoCallsApi.goLive(type, id) }
    }

    /**
     * @see StreamVideo.stopLive
     */
    override suspend fun stopLive(type: String, id: String): Result<StopLiveResponse> {

        return wrapAPICall { connectionModule.videoCallsApi.stopLive(type, id) }
    }

    /**
     * @see StreamVideo.muteUsers
     */
    override suspend fun muteUsers(
        type: String,
        id: String,
        muteUsersData: MuteUsersData
    ): Result<Unit> {

        val request = muteUsersData.toRequest()
        return wrapAPICall {
            connectionModule.videoCallsApi.muteUsers(type, id, request)
        }
    }

    /**
     * @see StreamVideo.queryCalls
     */
    override suspend fun queryCalls(queryCallsData: QueryCallsData): Result<QueriedCalls> {
        logger.d { "[queryCalls] queryCallsData: $queryCallsData" }
        val request = queryCallsData.toRequest()
        val connectionId = connectionModule.coordinatorSocket.getConnectionId()
        return wrapAPICall { connectionModule.defaultApi.queryCalls(request, connectionId).toQueriedCalls() }
    }

    /**
     * @see StreamVideo.requestPermissions
     */
    override suspend fun requestPermissions(
        type: String,
        id: String,
        permissions: List<String>
    ): Result<Unit> {
        logger.d { "[requestPermissions] callCid: $type:$id, permissions: $permissions" }

        return wrapAPICall { connectionModule.defaultApi.requestPermission(type, id, RequestPermissionRequest(permissions)) }
    }

    /**
     * @see StreamVideo.startBroadcasting
     */
    override suspend fun startBroadcasting(type: String, id: String): Result<Unit> {
        logger.d { "[startBroadcasting] callCid: $type $id" }

        return wrapAPICall { connectionModule.defaultApi.startBroadcasting(type, id) }
    }

    /**
     * @see StreamVideo.stopBroadcasting
     */
    override suspend fun stopBroadcasting(type: String, id: String): Result<Unit> {

        return wrapAPICall { connectionModule.defaultApi.stopBroadcasting(type, id) }
    }

    /**
     * @see StreamVideo.startRecording
     */
    override suspend fun startRecording(type: String, id: String): Result<Unit> {

        return wrapAPICall { connectionModule.defaultApi.startRecording(type, id) }
    }

    /**
     * @see StreamVideo.stopRecording
     */
    override suspend fun stopRecording(type: String, id: String): Result<Unit> {

        return wrapAPICall {
            connectionModule.defaultApi.stopRecording(type, id)
        }
    }

    /**
     * @see StreamVideo.updateUserPermissions
     */
    override suspend fun updateUserPermissions(
        type: String,
        id: String,
        updateUserPermissionsData: UpdateUserPermissionsData
    ): Result<Unit> {
        return wrapAPICall {
            connectionModule.defaultApi.updateUserPermissions(type, id, updateUserPermissionsData.toRequest())
        }
    }

    /**
     * @see StreamVideo.listRecordings
     */
    override suspend fun listRecordings(
        type: String,
        id: String,
        sessionId: String
    ): Result<ListRecordingsResponse> {
        // TODO: Result structure isn't flexible
        return wrapAPICall {
            val result = connectionModule.defaultApi.listRecordings(type, id, sessionId)

            result.recordings.map { it.toRecording() }
            result
        }
    }

    /**
     * @see StreamVideo.sendReaction
     */
    override suspend fun sendReaction(
        type: String,
        id: String,
        sendReactionData: SendReactionData
    ): Result<SendReactionResponse> {
        logger.d { "[sendVideoReaction] callCid: $type:$id, sendReactionData: $sendReactionData" }

        return wrapAPICall {
            connectionModule.defaultApi.sendVideoReaction(type, id, sendReactionData.toRequest())
        }
    }

    /**
     * @see StreamVideo.getEdges
     */
    override suspend fun getEdges(): Result<List<EdgeData>> {
        logger.d { "[getEdges] no params" }

        return wrapAPICall {
            val result = connectionModule.videoCallsApi.getEdges()

            result.edges.map { it.toEdge() }
        }
    }

    /**
     * @see StreamVideo.logOut
     */
    override fun logOut() {
        val preferences = UserPreferencesManager.getPreferences()

        removeDevices(preferences.getDevices())
        preferences.clear()
    }

    /**
     * Attempts to reconnect the socket if it's in a disconnected state and the user is available.
     */
    private fun reconnectSocket() {

        if (connectionModule.coordinatorStateService.state !is SocketState.Connected && user.id.isNotBlank()) {
            connectionModule.coordinatorSocket.reconnect()
        }
    }

    /**
     * @see StreamVideo.addSocketListener
     */
    override fun addSocketListener(socketListener: SocketListener): Unit =
        connectionModule.coordinatorSocket.addListener(socketListener)

    /**
     * @see StreamVideo.removeSocketListener
     */
    override fun removeSocketListener(socketListener: SocketListener): Unit =
        connectionModule.coordinatorSocket.removeListener(socketListener)

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

    /**
     * @see StreamVideo.getActiveCallClient
     */
    override fun getActiveCallClient(): CallClient? {
        return callClientHolder.value
    }

    /**
     * @see StreamVideo.awaitCallClient
     */
    override suspend fun awaitCallClient(): CallClient = withContext(scope.coroutineContext) {
        callClientHolder.first { it != null } ?: error("callClient must not be null")
    }

    override suspend fun acceptCall(type: String, id: String) {
        TODO("Not yet implemented")
    }

    /**
     * @see StreamVideo.acceptCall
     */
//    override suspend fun acceptCall(type: String, id: String): Result<JoinedCall> =
//        withContext(scope.coroutineContext) {
//            Result<JoinedCall>(JoinedCall())
////            try {
////
////                sendEvent(
////                    type, id,
////                    eventType = CallEventType.ACCEPTED
////                ).flatMap {
////                    joinCall(type, id)
////                }.also {
////                    logger.v { "[acceptCall] result: $it" }
////                }
////            } catch (e: Throwable) {
////                logger.e { "[acceptCall] failed: $e" }
////                Failure(VideoError(e.message, e))
////            }
//        }

    /**
     * @see StreamVideo.rejectCall
     */
    override suspend fun rejectCall(type: String, id: String): Result<SendEventResponse> {
        logger.d { "[rejectCall] cid: $type:$id" }
        return sendEvent(type, id, CallEventType.REJECTED)
    }

    /**
     * @see StreamVideo.cancelCall
     */
    override suspend fun cancelCall(type: String, id: String): Result<SendEventResponse> {
        return sendEvent(type = type, id = id, CallEventType.CANCELLED)
    }

    /**
     * @see StreamVideo.handlePushMessage
     */
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

                    // TODO engine.onCoordinatorEvent(event)
                    Success(Unit)
                }
                is Failure -> result
            }
        }

    override fun call(type: String, id: String, token: String): Call2 {
        val cid = "$type:$id"
        return if (calls.contains(cid)) {
            calls[cid]!!
        } else {
            val call = Call2(this, type, id, token, user)
            calls[cid] = call
            call
        }
    }

    public var nextEventContinuation: Continuation<VideoEvent>? = null
    public var nextEventCompleted: Boolean = false

    suspend fun waitForNextEvent(): VideoEvent = suspendCoroutine { continuation ->
        nextEventContinuation = continuation
        nextEventCompleted = false
    }
}

/** Extension function that makes it easy to use on kotlin, but keeps Java usable as well */
public inline fun <reified T : VideoEvent> StreamVideo.subscribeFor(
    listener: VideoEventListener<T>
): EventSubscription {
    return this.subscribeFor(
        T::class.java,
        listener = { event ->
            listener.onEvent(event as T)
        }
    )
}
