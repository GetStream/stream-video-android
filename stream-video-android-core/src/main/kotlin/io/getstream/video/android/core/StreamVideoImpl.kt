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
import io.getstream.android.push.PushDevice
import io.getstream.android.push.PushDeviceGenerator
import io.getstream.android.push.PushProvider
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.result.flatMap
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.getstream.video.android.core.errors.VideoErrorCode
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.internal.module.ConnectionModule
import io.getstream.video.android.core.lifecycle.LifecycleHandler
import io.getstream.video.android.core.lifecycle.internal.StreamLifecycleObserver
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.EdgeData
import io.getstream.video.android.core.model.MuteUsersData
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.model.UpdateUserPermissionsData
import io.getstream.video.android.core.model.toRequest
import io.getstream.video.android.core.socket.ErrorResponse
import io.getstream.video.android.core.socket.SocketState
import io.getstream.video.android.core.utils.DebugInfo
import io.getstream.video.android.core.utils.INTENT_EXTRA_CALL_CID
import io.getstream.video.android.core.utils.LatencyResult
import io.getstream.video.android.core.utils.getLatencyMeasurementsOKHttp
import io.getstream.video.android.core.utils.toEdge
import io.getstream.video.android.core.utils.toUser
import io.getstream.video.android.datastore.delegate.StreamUserDataStore
import io.getstream.video.android.model.Device
import io.getstream.video.android.model.User
import io.getstream.video.android.model.UserDevices
import io.getstream.video.android.model.mapper.toTypeAndId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.openapitools.client.models.AcceptCallResponse
import org.openapitools.client.models.BlockUserRequest
import org.openapitools.client.models.BlockUserResponse
import org.openapitools.client.models.CallRequest
import org.openapitools.client.models.CallSettingsRequest
import org.openapitools.client.models.ConnectedEvent
import org.openapitools.client.models.CreateDeviceRequest
import org.openapitools.client.models.CreateGuestRequest
import org.openapitools.client.models.CreateGuestResponse
import org.openapitools.client.models.GetCallResponse
import org.openapitools.client.models.GetOrCreateCallRequest
import org.openapitools.client.models.GetOrCreateCallResponse
import org.openapitools.client.models.GoLiveResponse
import org.openapitools.client.models.JoinCallRequest
import org.openapitools.client.models.JoinCallResponse
import org.openapitools.client.models.ListRecordingsResponse
import org.openapitools.client.models.MemberRequest
import org.openapitools.client.models.MuteUsersResponse
import org.openapitools.client.models.QueryCallsRequest
import org.openapitools.client.models.QueryCallsResponse
import org.openapitools.client.models.QueryMembersRequest
import org.openapitools.client.models.QueryMembersResponse
import org.openapitools.client.models.RejectCallResponse
import org.openapitools.client.models.RequestPermissionRequest
import org.openapitools.client.models.SendEventRequest
import org.openapitools.client.models.SendEventResponse
import org.openapitools.client.models.SendReactionRequest
import org.openapitools.client.models.SendReactionResponse
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
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resumeWithException

/**
 * @param lifecycle The lifecycle used to observe changes in the process
 */
internal class StreamVideoImpl internal constructor(
    override val context: Context,
    internal val _scope: CoroutineScope,
    override val user: User,
    private val lifecycle: Lifecycle,
    private val loggingLevel: LoggingLevel,
    internal val connectionModule: ConnectionModule,
    internal val pushDeviceGenerators: List<PushDeviceGenerator>,
    internal val tokenProvider: (suspend (error: Throwable?) -> String)?,
    internal val dataStore: StreamUserDataStore,
) : StreamVideo {

    private var locationJob: Deferred<Result<String>>? = null

    /** the state for the client, includes the current user */
    override val state = ClientState(this)

    internal val scope = CoroutineScope(_scope.coroutineContext + SupervisorJob())
    /** if true we fail fast on errors instead of logging them */
    var developmentMode = true
    val debugInfo = DebugInfo(this)

    /** session id is generated client side */
    public val sessionId = UUID.randomUUID().toString()

    internal var guestUserJob: Deferred<Unit>? = null
    private lateinit var connectContinuation: Continuation<Result<ConnectedEvent>>

    @InternalStreamVideoApi
    public var peerConnectionFactory = StreamPeerConnectionFactory(context)
    public override val userId = user.id

    private val logger by taggedLogger("Call:StreamVideo")
    private var subscriptions = mutableSetOf<EventSubscription>()
    private var calls = mutableMapOf<String, Call>()

    val socketImpl = connectionModule.coordinatorSocket

    override fun cleanup() {
        // stop all running coroutines
        scope.cancel()
        // stop the socket
        socketImpl.cleanup()
        // call cleanup on the active call
        val activeCall = state.activeCall.value
        activeCall?.cleanup()
    }

    /**
     * @see StreamVideo.createDevice
     */
    override suspend fun createDevice(pushDevice: PushDevice): Result<Device> {
        logger.d { "[createDevice] pushDevice: $pushDevice" }
        return pushDevice.toCreateDeviceRequest().flatMap { createDeviceRequest ->
            wrapAPICall {
                connectionModule.devicesApi.createDevice(createDeviceRequest)
                Device(
                    id = pushDevice.token,
                    pushProvider = pushDevice.pushProvider.key,
                    pushProviderName = pushDevice.providerName ?: ""
                ).also(::storeDevice)
            }
        }
    }

    private fun PushDevice.toCreateDeviceRequest(): Result<CreateDeviceRequest> =
        when (pushProvider) {
            PushProvider.FIREBASE -> Success(CreateDeviceRequest.PushProvider.firebase)
            PushProvider.HUAWEI -> Success(CreateDeviceRequest.PushProvider.huawei)
            PushProvider.XIAOMI -> Success(CreateDeviceRequest.PushProvider.xiaomi)
            PushProvider.UNKNOWN -> Failure(Error.GenericError("Unsupported PushProvider"))
        }.map {
            CreateDeviceRequest(
                id = token,
                pushProvider = it,
                pushProviderName = providerName
            )
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
                    if (tokenProvider != null) {
                        // TODO - handle this better, error structure is not great right now
                        val newToken = tokenProvider.invoke(null)
                        dataStore.updateUserToken(newToken)
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
            connectionModule.coordinatorSocket.events.collect {
                fireEvent(it)
            }
        }
        scope.launch {
            connectionModule.coordinatorSocket.errors.collect {
                if (developmentMode) {
                    logger.e(it) { "failure on socket connection" }
                } else {
                    logger.e(it) { "failure on socket connection" }
                }
            }
        }
    }

    var location: String? = null

    internal suspend fun getCachedLocation(): Result<String> {
        val job = loadLocationAsync()
        job.join()
        location?.let {
            return Success(it)
        }
        return selectLocation()
    }

    internal fun loadLocationAsync(): Deferred<Result<String>> {
        if (locationJob != null) return locationJob as Deferred<Result<String>>
        locationJob = scope.async {
            selectLocation()
        }
        return locationJob as Deferred<Result<String>>
    }

    internal suspend fun selectLocation(): Result<String> {
        var attempts = 0
        var lastResult: Result<String>? = null
        while (attempts < 3) {
            attempts += 1
            lastResult = _selectLocation()
            if (lastResult is Success) {
                location = lastResult.value
                return lastResult
            }
            delay(100L)
            if (attempts == 3) {
                return lastResult
            }
        }

        return Failure(Error.GenericError("failed to select location"))
    }

    override suspend fun connectAsync(): Deferred<Unit> {
        return scope.async {
            // wait for the guest user setup if we're using guest users
            guestUserJob?.await()
            try {
                val timer = debugInfo.trackTime("coordinator connect")
                socketImpl.connect()
                timer.finish()
            } catch (e: ErrorResponse) {
                if (e.code == VideoErrorCode.TOKEN_EXPIRED.code && tokenProvider != null) {
                    // refresh the the token
                    println("CAUGHT an error")
                    val newToken = tokenProvider.invoke(e)
                    dataStore.updateUserToken(newToken)
                    connectionModule.updateToken(newToken)
                    // quickly reconnect with the new token
                    socketImpl.reconnect(0)
                } else {
                    throw e
                }
            }
        }
    }

    private fun storeDevice(device: Device) {
        logger.d { "[storeDevice] device: device" }
        scope.launch {
            val dataStore = StreamUserDataStore.instance()
            dataStore.updateUserDevices(
                UserDevices(
                    dataStore.userDevices.value?.let { it.devices + device } ?: listOf(device)
                )
            )
        }
    }

    private fun removeStoredDeivce(device: Device) {
        logger.d { "[storeDevice] device: device" }
        scope.launch {
            val dataStore = StreamUserDataStore.instance()
            dataStore.updateUserDevices(
                UserDevices(
                    dataStore.userDevices.value?.let {
                        it.devices.filter { it.id != device.id }
                    } ?: listOf()
                )
            )
        }
    }

    /**
     * @see StreamVideo.deleteDevice
     */
    override suspend fun deleteDevice(device: Device): Result<Unit> {
        logger.d { "[deleteDevice] device: $device" }
        return wrapAPICall {
            connectionModule.devicesApi.deleteDevice(device.id, userId)
            removeStoredDeivce(device)
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
                connectionModule.updateAuthType("jwt")
                scope.launch {
                    dataStore.updateUser(it.user.toUser())
                    dataStore.updateUserToken(it.accessToken)
                }
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
                    createDevice(generatedDevice)
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
        logger.d { "Event received $event" }
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
                it.session?.handleEvent(event)
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
            calls[selectedCid]?.fireEvent(event)
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
    internal suspend fun getOrCreateCall(
        type: String,
        id: String,
        memberIds: List<String>? = null,
        custom: Map<String, Any>? = null,
        settingsOverride: CallSettingsRequest? = null,
        startsAt: org.threeten.bp.OffsetDateTime? = null,
        team: String? = null,
        ring: Boolean,
        notify: Boolean,
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
            ring = ring,
            notify = notify
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
        ring: Boolean,
        notify: Boolean
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
                    ring = ring,
                    notify = notify,
                ),
                connectionId = connectionModule.coordinatorSocket.connectionId
            )
        }
    }

    internal suspend fun inviteUsers(
        type: String,
        id: String,
        users: List<User>
    ): Result<Unit> {
        logger.d { "[inviteUsers] users: $users" }

        return wrapAPICall {
            error("TODO: not support yet")
        }
    }

    /**
     * Measures and prepares the latency which describes how much time it takes to ping the server.
     *
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

    suspend fun joinCall(
        type: String,
        id: String,
        create: Boolean = false,
        members: List<MemberRequest>? = null,
        custom: Map<String, Any>? = null,
        settingsOverride: CallSettingsRequest? = null,
        startsAt: org.threeten.bp.OffsetDateTime? = null,
        team: String? = null,
        ring: Boolean = false,
        notify: Boolean = false,
        location: String
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
            notify = notify,
            location = location,
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

    internal suspend fun sendCustomEvent(
        type: String,
        id: String,
        dataJson: Map<String, Any>,
    ): Result<SendEventResponse> {
        logger.d { "[sendCustomEvent] callCid: $type:$id, dataJson: $dataJson" }

        return wrapAPICall {
            connectionModule.eventsApi.sendEvent(
                type,
                id,
                SendEventRequest(custom = dataJson)
            )
        }
    }

    internal suspend fun queryMembers(
        type: String,
        id: String,
        // TODO: why can't the filter be null
        filter: Map<String, Any>,
        sort: List<SortField> = mutableListOf(SortField.Desc("created_at")),
        limit: Int = 100
    ): Result<QueryMembersResponse> {

        return wrapAPICall {
            connectionModule.videoCallsApi.queryMembers(
                QueryMembersRequest(
                    type = type, id = id,
                    filterConditions = filter,
                    sort = sort.map { it.toRequest() },
                    limit = limit
                )
            )
        }
    }

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

    suspend fun endCall(type: String, id: String): Result<Unit> {
        return wrapAPICall { connectionModule.videoCallsApi.endCall(type, id) }
    }

    suspend fun goLive(type: String, id: String): Result<GoLiveResponse> {
        logger.d { "[goLive] callCid: $type:$id" }

        return wrapAPICall { connectionModule.videoCallsApi.goLive(type, id) }
    }

    suspend fun stopLive(type: String, id: String): Result<StopLiveResponse> {

        return wrapAPICall { connectionModule.videoCallsApi.stopLive(type, id) }
    }

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
    override suspend fun queryCalls(
        filters: Map<String, Any>,
        sort: List<SortField>,
        limit: Int,
        watch: Boolean
    ): Result<QueryCallsResponse> {
        logger.d { "[queryCalls] filters: $filters, sort: $sort, limit: $limit, watch: $watch" }
        val request = QueryCallsRequest(
            filterConditions = filters,
            sort = sort.map { it.toRequest() },
            limit = limit,
            watch = watch
        )
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

    suspend fun startBroadcasting(type: String, id: String): Result<Unit> {
        logger.d { "[startBroadcasting] callCid: $type $id" }

        return wrapAPICall { connectionModule.livestreamingApi.startBroadcasting(type, id) }
    }

    suspend fun stopBroadcasting(type: String, id: String): Result<Unit> {

        return wrapAPICall { connectionModule.livestreamingApi.stopBroadcasting(type, id) }
    }

    suspend fun startRecording(type: String, id: String): Result<Unit> {

        return wrapAPICall { connectionModule.recordingApi.startRecording(type, id) }
    }

    suspend fun stopRecording(type: String, id: String): Result<Unit> {

        return wrapAPICall {
            connectionModule.recordingApi.stopRecording(type, id)
        }
    }

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

    suspend fun listRecordings(
        type: String,
        id: String,
        sessionId: String
    ): Result<ListRecordingsResponse> {
        return wrapAPICall {
            val result = connectionModule.recordingApi.listRecordingsTypeIdSession1(type, id, sessionId)
            result
        }
    }

    suspend fun sendReaction(
        callType: String,
        id: String,
        type: String,
        emoji: String? = null,
        custom: Map<String, Any>? = null
    ): Result<SendReactionResponse> {
        val request = SendReactionRequest(type, custom, emoji)

        logger.d { "[sendVideoReaction] callCid: $type:$id, sendReactionData: $request" }

        return wrapAPICall {
            connectionModule.videoCallsApi.sendVideoReaction(callType, id, request)
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
        val dataStore = StreamUserDataStore.instance()
        scope.launch { dataStore.clear() }
    }

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
        val idOrRandom = id.ifEmpty { UUID.randomUUID().toString() }

        val cid = "$type:$idOrRandom"
        return if (calls.contains(cid)) {
            calls[cid]!!
        } else {
            val call = Call(this, type, idOrRandom, user)
            calls[cid] = call
            call
        }
    }

    suspend fun _selectLocation(): Result<String> {
        return wrapAPICall {
            val url = "https://hint.stream-io-video.com/"
            val request: Request = Request.Builder()
                .url(url)
                .method("HEAD", null)
                .build()
            val call = connectionModule.okHttpClient.newCall(request)
            val response = suspendCancellableCoroutine<Response> { continuation ->
                call.enqueue(object : Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        continuation.resumeWithException(e)
                    }
                    override fun onResponse(call: okhttp3.Call, response: Response) {
                        continuation.resume(response) {
                            call.cancel()
                        }
                    }
                })
            }

            if (!response.isSuccessful) {
                throw Error("Unexpected code $response")
            }
            val locationHeader = response.headers["X-Amz-Cf-Pop"]
            locationHeader?.take(3) ?: "missing-location"
        }
    }

    internal suspend fun accept(type: String, id: String): Result<AcceptCallResponse> {
        return wrapAPICall {
            connectionModule.videoCallsApi.acceptCall(type, id)
        }
    }
    internal suspend fun reject(type: String, id: String): Result<RejectCallResponse> {
        return wrapAPICall {
            connectionModule.videoCallsApi.rejectCall(type, id)
        }
    }

    internal suspend fun notify(type: String, id: String): Result<GetCallResponse> {
        return wrapAPICall {
            connectionModule.videoCallsApi.getCall(type, id, notify = true)
        }
    }
    internal suspend fun ring(type: String, id: String): Result<GetCallResponse> {
        return wrapAPICall {
            connectionModule.videoCallsApi.getCall(type, id, ring = true)
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
