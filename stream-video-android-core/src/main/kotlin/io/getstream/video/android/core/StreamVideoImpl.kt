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
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import io.getstream.android.push.PushDeviceGenerator
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.getstream.video.android.core.errors.VideoErrorCode
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.internal.module.ConnectionModule
import io.getstream.video.android.core.lifecycle.LifecycleHandler
import io.getstream.video.android.core.lifecycle.internal.StreamLifecycleObserver
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.CallEventType
import io.getstream.video.android.core.model.Device
import io.getstream.video.android.core.model.EdgeData
import io.getstream.video.android.core.model.MuteUsersData
import io.getstream.video.android.core.model.QueryCallsData
import io.getstream.video.android.core.model.SendReactionData
import io.getstream.video.android.core.model.UpdateUserPermissionsData
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.mapper.toTypeAndId
import io.getstream.video.android.core.model.toRequest
import io.getstream.video.android.core.socket.ErrorResponse
import io.getstream.video.android.core.socket.SocketState
import io.getstream.video.android.core.user.UserPreferences
import io.getstream.video.android.core.user.UserPreferencesManager
import io.getstream.video.android.core.utils.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.utils.LatencyResult
import io.getstream.video.android.core.utils.getLatencyMeasurementsOKHttp
import io.getstream.video.android.core.utils.toEdge
import io.getstream.video.android.core.utils.toUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.openapitools.client.models.BlockUserRequest
import org.openapitools.client.models.BlockUserResponse
import org.openapitools.client.models.CallRequest
import org.openapitools.client.models.CallSettingsRequest
import org.openapitools.client.models.ConnectedEvent
import org.openapitools.client.models.CreateGuestRequest
import org.openapitools.client.models.CreateGuestResponse
import org.openapitools.client.models.GetCallEdgeServerRequest
import org.openapitools.client.models.GetCallEdgeServerResponse
import org.openapitools.client.models.GetCallResponse
import org.openapitools.client.models.GetOrCreateCallRequest
import org.openapitools.client.models.GetOrCreateCallResponse
import org.openapitools.client.models.GoLiveResponse
import org.openapitools.client.models.JoinCallRequest
import org.openapitools.client.models.JoinCallResponse
import org.openapitools.client.models.ListRecordingsResponse
import org.openapitools.client.models.MemberRequest
import org.openapitools.client.models.MuteUsersResponse
import org.openapitools.client.models.QueryCallsResponse
import org.openapitools.client.models.QueryMembersRequest
import org.openapitools.client.models.QueryMembersResponse
import org.openapitools.client.models.RequestPermissionRequest
import org.openapitools.client.models.SendEventRequest
import org.openapitools.client.models.SendEventResponse
import org.openapitools.client.models.SendReactionResponse
import org.openapitools.client.models.SortParamRequest
import org.openapitools.client.models.StopLiveResponse
import org.openapitools.client.models.UnblockUserRequest
import org.openapitools.client.models.UpdateCallMembersRequest
import org.openapitools.client.models.UpdateCallMembersResponse
import org.openapitools.client.models.UpdateCallRequest
import org.openapitools.client.models.UpdateCallResponse
import org.openapitools.client.models.UpdateUserPermissionsResponse
import org.openapitools.client.models.UserRequest
import org.openapitools.client.models.VideoEvent
import org.openapitools.client.models.WSCallEvent
import retrofit2.HttpException
import kotlin.coroutines.Continuation

/**
 * @param lifecycle The lifecycle used to observe changes in the process
 */
internal class StreamVideoImpl internal constructor(
    override val context: Context,
    internal val scope: CoroutineScope,
    override val user: User,
    private val lifecycle: Lifecycle,
    private val loggingLevel: LoggingLevel,
    internal val connectionModule: ConnectionModule,
    internal val pushDeviceGenerators: List<PushDeviceGenerator>,
    internal val tokenProvider: ((error: Throwable?) -> String)?,
    internal val preferences: UserPreferences,
) : StreamVideo {

    /** the state for the client, includes the current user */
    override val state = ClientState(this)

    /** if true we fail fast on errors instead of logging them */
    var developmentMode = true

    private var guestUserJob: Deferred<Unit>? = null
    private lateinit var connectContinuation: Continuation<Result<ConnectedEvent>>



    @VisibleForTesting
    public var peerConnectionFactory = StreamPeerConnectionFactory(context)
    public override val userId = user.id


    private val logger by taggedLogger("Call:StreamVideo")
    private var subscriptions = mutableSetOf<EventSubscription>()
    private var calls = mutableMapOf<String, Call>()

    val socketImpl = connectionModule.coordinatorSocket

    /**
     * @see StreamVideo.createDevice
     */
    override suspend fun createDevice(token: String, pushProvider: String): Result<Device> {
        logger.d { "[createDevice] token: $token, pushProvider: $pushProvider" }
        return wrapAPICall {
            // TODO: handle this when backend has it
            error("TODO: not support yet")
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
                val failure = parseError(e)
                val parsedError = failure.value as Error.NetworkError
                if (parsedError.serverErrorCode == VideoErrorCode.TOKEN_EXPIRED.code) {
                    // invalid token
                    // val newToken = tokenProvider.getToken()
                    if (tokenProvider != null) {
                        // TODO - handle this better, error structure is not great right now
                        val newToken = tokenProvider.invoke(null)
                        preferences.storeUserToken(newToken)
                        connectionModule.updateToken(newToken)
                    }
                    // retry the API call once
                    try {
                        Success(apiCall())
                    } catch (e: HttpException) {
                        parseError(e)
                    }

                    // set the token, repeat API call
                    // keep track of retry count
                } else {
                    failure
                }
            }
        }
    }

    /**
     * @see StreamVideo.updateCall
     */
    suspend fun updateCall(
        type: String,
        id: String,
        request: UpdateCallRequest,
    ): Result<UpdateCallResponse> {
        logger.d { "[updateCall] type: $type, id: $id, request: $request" }
        return wrapAPICall {
            connectionModule.videoCallsApi.updateCall(
                type = type,
                id = id,
                updateCallRequest = request
            )
        }
    }

    private fun parseError(e: HttpException): Failure {
        val errorBytes = e.response()?.errorBody()?.bytes()
        val error = errorBytes?.let {
            try {
                val errorBody = String(it, Charsets.UTF_8)
                val format = Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                }
                format.decodeFromString<ErrorResponse>(errorBody)
            } catch (e: Exception) {
                return Failure(
                    Error.NetworkError(
                        "failed to parse error response from server: ${e.message}",
                        VideoErrorCode.PARSER_ERROR.code
                    )
                )
            }
        } ?: return Failure(
            Error.NetworkError("failed to parse error response from server", e.code())
        )
        return Failure(
            Error.NetworkError(
                message = error.message,
                serverErrorCode = error.code,
                statusCode = error.statusCode,
                cause = Throwable(error.moreInfo)
            )
        )
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
                override fun resume() {
                    scope.launch {
                        // We should only connect if we were previously connected
                        if (connectionModule.coordinatorSocket.connectionState.value != SocketState.NotConnected) {
                            connectionModule.coordinatorSocket.connect()
                        }
                    }
                }

                override fun stopped() {
                    // We should only disconnect if we were previously connected
                    if (connectionModule.coordinatorSocket.connectionState.value != SocketState.NotConnected) {
                        connectionModule.coordinatorSocket.disconnect()
                    }
                }
            }
        )

    init {
        scope.launch(Dispatchers.Main.immediate) {
            lifecycleObserver.observe()
        }

        // listen to socket events and errors
        scope.launch {
            connectionModule.coordinatorSocket.events.collect() {
                fireEvent(it)
            }
        }
        scope.launch {
            connectionModule.coordinatorSocket.errors.collect() {
                if (developmentMode) {
                    throw it
                } else {
                    logger.e(it) { "permanent failure on socket connection" }
                }
            }
        }
    }

    suspend fun connectAsync(): Deferred<Unit> {
        return scope.async {
            // wait for the guest user setup if we're using guest users
            guestUserJob?.let { it.await() }
            try {
                val result = socketImpl.connect()
                result
            } catch (e: ErrorResponse) {
                if (e.code == 40) {
                    // refresh the the token
                    if (tokenProvider != null) {
                        val newToken = tokenProvider.invoke(e)
                        preferences.storeUserToken(newToken)
                        connectionModule.updateToken(newToken)
                    }
                    // quickly reconnect with the new token
                    val result = socketImpl.reconnect(0)
                    result
                }
            }
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

        // TODO: handle this when we have push on the backend
        return wrapAPICall {}
    }

    /**
     * @see StreamVideo.removeDevices
     */
    fun removeDevices(devices: List<Device>) {
        scope.launch {
            val operations = devices.map {
                async { deleteDevice(it.token) }
            }

            operations.awaitAll()
        }
    }

    fun setupGuestUser(user: User) {
        guestUserJob = scope.async {
            val response = createGuestUser(
                userRequest = UserRequest(
                    id = user.id,
                    image = user.image,
                    name = user.name,
                    role = user.role,
                    teams = user.teams,
                    custom = user.custom,
                )
            )
            if (response.isFailure) {
                throw IllegalStateException("Failed to create guest user")
            }
            response.onSuccess {
                preferences.storeUserCredentials(it.user.toUser())
                preferences.storeUserToken(it.accessToken)
                connectionModule.updateToken(it.accessToken)
            }
        }
    }

    suspend fun createGuestUser(userRequest: UserRequest): Result<CreateGuestResponse> {
        return wrapAPICall {
            connectionModule.defaultApi.createGuest(
                createGuestRequest = CreateGuestRequest(userRequest)
            )
        }
    }

    override suspend fun registerPushDevice() {
        // first get a push device generator that works for this device
        val generator = pushDeviceGenerators.firstOrNull { it.isValidForThisDevice(context) }

        // if we found one, register it at the server
        if (generator != null) {
            generator.onPushDeviceGeneratorSelected()

            generator.asyncGeneratePushDevice { generatedDevice ->
                scope.launch {
                    createDevice(
                        token = generatedDevice.token,
                        pushProvider = generatedDevice.pushProvider.key
                    )
                }
            }
        }
    }

    /**
     * Domain - Coordinator.
     */

    /**
     * Internal function that fires the event. It starts by updating client state and call state
     * After that it loops over the subscriptions and calls their listener
     */
    internal fun fireEvent(event: VideoEvent, cid: String = "") {

        // update state for the client
        state.handleEvent(event)

        // update state for the calls. calls handle updating participants and members
        val selectedCid = cid.ifEmpty {
            val callEvent = event as? WSCallEvent
            callEvent?.getCallCID()
        } ?: ""

        if (selectedCid.isNotEmpty()) {
            calls[selectedCid]?.let {
                it.state.handleEvent(event)
                it.session?.let {
                    it.handleEvent(event)
                }
            }
        }

        // client level subscriptions
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
        // call level subscriptions
        if (selectedCid.isNotEmpty()) {
            calls[selectedCid]?.let {
                it.fireEvent(event)
            }
        }
    }

    internal suspend fun getCall(type: String, id: String): Result<GetCallResponse> {
        return wrapAPICall {
            connectionModule.videoCallsApi.getCall(
                type,
                id,
                connectionId = connectionModule.coordinatorSocket.connectionId
            )
        }
    }

    // caller: DIAL and wait answer
    /**
     * @see StreamVideo.getOrCreateCall
     */
    internal suspend fun getOrCreateCall(
        type: String,
        id: String,
        memberIds: List<String>? = null,
        custom: Map<String, Any>? = null,
        settingsOverride: CallSettingsRequest? = null,
        startsAt: org.threeten.bp.OffsetDateTime? = null,
        team: String? = null,
        ring: Boolean
    ): Result<GetOrCreateCallResponse> {

        val members = memberIds?.map {
            MemberRequest(
                userId = it
            )
        }

        return getOrCreateCallFullMembers(
            type = type,
            id = id,
            members = members,
            custom = custom,
            settingsOverride = settingsOverride,
            startsAt = startsAt,
            team = team,
            ring = ring
        )
    }

    internal suspend fun getOrCreateCallFullMembers(
        type: String,
        id: String,
        members: List<MemberRequest>? = null,
        custom: Map<String, Any>? = null,
        settingsOverride: CallSettingsRequest? = null,
        startsAt: org.threeten.bp.OffsetDateTime? = null,
        team: String? = null,
        ring: Boolean
    ): Result<GetOrCreateCallResponse> {
        logger.d { "[getOrCreateCall] type: $type, id: $id, members: $members" }

        return wrapAPICall {
            connectionModule.videoCallsApi.getOrCreateCall(
                type = type,
                id = id,
                getOrCreateCallRequest = GetOrCreateCallRequest(
                    data = CallRequest(
                        members = members,
                        custom = custom,
                        settingsOverride = settingsOverride,
                        startsAt = startsAt,
                        team = team,
                    ),
                    ring = ring
                ),
                connectionId = connectionModule.coordinatorSocket.connectionId
            )
        }
    }

    /**
     * @see StreamVideo.inviteUsers
     */
    internal suspend fun inviteUsers(type: String, id: String, users: List<User>): Result<Unit> {
        logger.d { "[inviteUsers] users: $users" }

        return wrapAPICall {
            error("TODO: not support yet")
        }
    }

    /**
     * Measures and prepares the latency which describes how much time it takes to ping the server.
     *
     * @param edgeUrl The edge we want to measure.
     *
     * @return [List] of [Float] values which represent measurements from ping connections.
     */
    internal suspend fun measureLatency(edgeUrls: List<String>): List<LatencyResult> =
        withContext(scope.coroutineContext) {
            val jobs = edgeUrls.map {
                async {
                    getLatencyMeasurementsOKHttp(it)
                }
            }
            val results = jobs.awaitAll().sortedBy { it.average }
            results
        }

    /**
     * @see CallCoordinatorClient.selectEdgeServer for details.
     */
    public suspend fun selectEdgeServer(
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

    suspend fun joinCall(
        type: String,
        id: String,
        create: Boolean = false,
        members: List<MemberRequest>? = null,
        custom: Map<String, Any>? = null,
        settingsOverride: CallSettingsRequest? = null,
        startsAt: org.threeten.bp.OffsetDateTime? = null,
        team: String? = null,
        ring: Boolean = false
    ): Result<JoinCallResponse> {

        val joinCallRequest = JoinCallRequest(
            create = create,
            data = CallRequest(
                members = members,
                custom = custom,
                settingsOverride = settingsOverride,
                startsAt = startsAt,
                team = team
            ),
            ring = ring,
        )

        val result = wrapAPICall {
            connectionModule.videoCallsApi.joinCall(
                type,
                id,
                joinCallRequest,
                connectionModule.coordinatorSocket.connectionId
            )
        }
        return result
    }

    suspend fun updateMembers(
        type: String,
        id: String,
        request: UpdateCallMembersRequest
    ): Result<UpdateCallMembersResponse> {
        return wrapAPICall {
            connectionModule.videoCallsApi.updateCallMembers(type, id, request)
        }
    }

    // callee: SEND Accepted or Rejected
    /**
     * @see StreamVideo.sendEvent
     */
    internal suspend fun sendEvent(
        type: String,
        id: String,
        eventType: CallEventType
    ): Result<SendEventResponse> {
        logger.d { "[sendEvent] callCid: $type:$id, eventType: $eventType" }

        return wrapAPICall {
            connectionModule.eventsApi.sendEvent(
                type,
                id,
                SendEventRequest(type = eventType.eventType)
            )
        }
    }

    /**
     * @see StreamVideo.sendCustomEvent
     */
    internal suspend fun sendCustomEvent(
        type: String,
        id: String,
        dataJson: Map<String, Any>,
        eventType: String
    ): Result<SendEventResponse> {
        val callCid = "$type:$id"
        logger.d { "[sendCustomEvent] callCid: $callCid, dataJson: $dataJson, eventType: $eventType" }
        val (type, id) = callCid.toTypeAndId()

        return wrapAPICall {
            connectionModule.eventsApi.sendEvent(
                type,
                id,
                SendEventRequest(custom = dataJson, type = eventType)
            )
        }
    }

    /**
     * @see StreamVideo.queryMembers
     */
    internal suspend fun queryMembers(
        type: String,
        id: String,
        // TODO: why can't the filter be null
        filter: Map<String, Any>,
        sort: List<SortParamRequest> = mutableListOf(SortParamRequest(-1, "created_at")),
        limit: Int = 100
    ): Result<QueryMembersResponse> {

        return wrapAPICall {
            connectionModule.videoCallsApi.queryMembers(
                QueryMembersRequest(
                    type = type, id = id,
                    filterConditions = filter,
                    sort = sort
                )
            )
        }
    }

    /**
     * @see StreamVideo.blockUser
     */
    suspend fun blockUser(type: String, id: String, userId: String): Result<BlockUserResponse> {
        logger.d { "[blockUser] callCid: $type:$id, userId: $userId" }

        return wrapAPICall {
            connectionModule.moderationApi.blockUser(
                type,
                id,
                BlockUserRequest(userId)
            )
        }
    }

    /**
     * @see StreamVideo.unblockUser
     */
    suspend fun unblockUser(type: String, id: String, userId: String): Result<Unit> {
        logger.d { "[unblockUser] callCid: $type:$id, userId: $userId" }

        return wrapAPICall {
            connectionModule.videoCallsApi.unblockUser(
                type,
                id,
                UnblockUserRequest(userId)
            )
        }
    }

    /**
     * @see StreamVideo.endCall
     */
    suspend fun endCall(type: String, id: String): Result<Unit> {
        return wrapAPICall { connectionModule.videoCallsApi.endCall(type, id) }
    }

    /**
     * @see StreamVideo.goLive
     */
    suspend fun goLive(type: String, id: String): Result<GoLiveResponse> {
        logger.d { "[goLive] callCid: $type:$id" }

        return wrapAPICall { connectionModule.videoCallsApi.goLive(type, id) }
    }

    /**
     * @see StreamVideo.stopLive
     */
    suspend fun stopLive(type: String, id: String): Result<StopLiveResponse> {

        return wrapAPICall { connectionModule.videoCallsApi.stopLive(type, id) }
    }

    /**
     * @see StreamVideo.muteUsers
     */
    suspend fun muteUsers(
        type: String,
        id: String,
        muteUsersData: MuteUsersData
    ): Result<MuteUsersResponse> {

        val request = muteUsersData.toRequest()
        return wrapAPICall {
            connectionModule.moderationApi.muteUsers(type, id, request)
        }
    }

    /**
     * @see StreamVideo.queryCalls
     */
    override suspend fun queryCalls(queryCallsData: QueryCallsData): Result<QueryCallsResponse> {
        logger.d { "[queryCalls] queryCallsData: $queryCallsData" }
        val request = queryCallsData.toRequest()
        val connectionId = connectionModule.coordinatorSocket.connectionId
        val result = wrapAPICall {
            connectionModule.videoCallsApi.queryCalls(request, connectionId)
        }
        if (result.isSuccess) {
            // update state for these calls
            result.onSuccess {
                it.calls.forEach { callData ->
                    val call = this.call(callData.call.type, callData.call.id)
                    call.state.updateFromResponse(callData)
                }
            }
        }

        return result
    }

    /**
     * @see StreamVideo.requestPermissions
     */
    suspend fun requestPermissions(
        type: String,
        id: String,
        permissions: List<String>
    ): Result<Unit> {
        logger.d { "[requestPermissions] callCid: $type:$id, permissions: $permissions" }

        return wrapAPICall {
            connectionModule.moderationApi.requestPermission(
                type,
                id,
                RequestPermissionRequest(permissions)
            )
        }
    }

    /**
     * @see StreamVideo.startBroadcasting
     */
    suspend fun startBroadcasting(type: String, id: String): Result<Unit> {
        logger.d { "[startBroadcasting] callCid: $type $id" }

        return wrapAPICall { connectionModule.livestreamingApi.startBroadcasting(type, id) }
    }

    /**
     * @see StreamVideo.stopBroadcasting
     */
    suspend fun stopBroadcasting(type: String, id: String): Result<Unit> {

        return wrapAPICall { connectionModule.livestreamingApi.stopBroadcasting(type, id) }
    }

    /**
     * @see StreamVideo.startRecording
     */
    suspend fun startRecording(type: String, id: String): Result<Unit> {

        return wrapAPICall { connectionModule.recordingApi.startRecording(type, id) }
    }

    /**
     * @see StreamVideo.stopRecording
     */
    suspend fun stopRecording(type: String, id: String): Result<Unit> {

        return wrapAPICall {
            connectionModule.recordingApi.stopRecording(type, id)
        }
    }

    /**
     * @see StreamVideo.updateUserPermissions
     */
    suspend fun updateUserPermissions(
        type: String,
        id: String,
        updateUserPermissionsData: UpdateUserPermissionsData
    ): Result<UpdateUserPermissionsResponse> {
        return wrapAPICall {
            connectionModule.moderationApi.updateUserPermissions(
                type,
                id,
                updateUserPermissionsData.toRequest()
            )
        }
    }

    /**
     * @see StreamVideo.listRecordings
     */
    suspend fun listRecordings(
        type: String,
        id: String,
        sessionId: String
    ): Result<ListRecordingsResponse> {
        return wrapAPICall {
            val result = connectionModule.recordingApi.listRecordings(type, id, sessionId)
            result
        }
    }

    /**
     * @see StreamVideo.sendReaction
     */
    suspend fun sendReaction(
        type: String,
        id: String,
        sendReactionData: SendReactionData
    ): Result<SendReactionResponse> {
        logger.d { "[sendVideoReaction] callCid: $type:$id, sendReactionData: $sendReactionData" }

        return wrapAPICall {
            connectionModule.videoCallsApi.sendVideoReaction(type, id, sendReactionData.toRequest())
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
     * Creates an instance of the [SFUSession] for the given call input, which is persisted and
     * used to communicate with the BE.
     *
     * Use it to control the track state, mute/unmute devices and listen to call events.
     *
     * @param callGuid The GUID of the Call, containing the ID and its type.
     * @param signalUrl The URL of the server in which the call is being hosted.
     * @param sfuToken User's ticket to enter the call.
     * @param iceServers Servers required to appropriately connect to the call and receive tracks.
     *
     * @return An instance of [SFUSession] ready to connect to a call. Make sure to call
     * [SFUSession.connectToCall] when you're ready to fully join a call.
     */

    suspend fun acceptCall(type: String, id: String) {
        TODO("Not yet implemented")
    }

    /**
     * @see StreamVideo.rejectCall
     */
    suspend fun rejectCall(type: String, id: String): Result<SendEventResponse> {
        logger.d { "[rejectCall] cid: $type:$id" }
        return sendEvent(type, id, CallEventType.REJECTED)
    }

    /**
     * @see StreamVideo.cancelCall
     */
    suspend fun cancelCall(type: String, id: String): Result<SendEventResponse> {
        return sendEvent(type = type, id = id, CallEventType.CANCELLED)
    }

    /**
     * @see StreamVideo.handlePushMessage
     */
    suspend fun handlePushMessage(payload: Map<String, Any>): Result<Unit> =
        withContext(scope.coroutineContext) {
            val callCid = payload[INTENT_EXTRA_CALL_CID] as? String
                ?: return@withContext Failure(Error.GenericError("Missing Call CID!"))

            val (type, id) = callCid.toTypeAndId()

            when (val result = getCall(type, id)) {
                is Success -> {
                    val callMetadata = result.value

//                    val event = CallCreatedEvent(
//                        callCid = callMetadata.cid,
//                        ringing = true,
//                        users = callMetadata.users,
//                        callInfo = callMetadata.toInfo(),
//                        callDetails = callMetadata.callDetails
//                    )

                    Success(Unit)
                }

                is Failure -> result
            }
        }

    override fun call(type: String, id: String): Call {
        val cid = "$type:$id"
        return if (calls.contains(cid)) {
            calls[cid]!!
        } else {
            val call = Call(this, type, id, user)
            calls[cid] = call
            call
        }
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
