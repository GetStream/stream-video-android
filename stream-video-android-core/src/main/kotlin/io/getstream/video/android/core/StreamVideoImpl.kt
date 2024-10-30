/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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
import android.media.AudioAttributes
import androidx.lifecycle.Lifecycle
import io.getstream.android.push.PushDevice
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.video.android.core.call.connection.StreamPeerConnectionFactory
import io.getstream.video.android.core.errors.VideoErrorCode
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.filter.Filters
import io.getstream.video.android.core.filter.toMap
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.internal.module.ConnectionModule
import io.getstream.video.android.core.lifecycle.LifecycleHandler
import io.getstream.video.android.core.lifecycle.internal.StreamLifecycleObserver
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.EdgeData
import io.getstream.video.android.core.model.MuteUsersData
import io.getstream.video.android.core.model.QueriedCalls
import io.getstream.video.android.core.model.QueriedMembers
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.model.UpdateUserPermissionsData
import io.getstream.video.android.core.model.toRequest
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.core.notifications.internal.StreamNotificationManager
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.callServiceConfig
import io.getstream.video.android.core.permission.android.DefaultStreamPermissionCheck
import io.getstream.video.android.core.permission.android.StreamPermissionCheck
import io.getstream.video.android.core.socket.ErrorResponse
import io.getstream.video.android.core.socket.PersistentSocket
import io.getstream.video.android.core.socket.SocketState
import io.getstream.video.android.core.sounds.Sounds
import io.getstream.video.android.core.utils.LatencyResult
import io.getstream.video.android.core.utils.getLatencyMeasurementsOKHttp
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.safeSuspendingCall
import io.getstream.video.android.core.utils.toEdge
import io.getstream.video.android.core.utils.toQueriedCalls
import io.getstream.video.android.core.utils.toQueriedMembers
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.Device
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.openapitools.client.models.AcceptCallResponse
import org.openapitools.client.models.BlockUserRequest
import org.openapitools.client.models.BlockUserResponse
import org.openapitools.client.models.CallAcceptedEvent
import org.openapitools.client.models.CallRequest
import org.openapitools.client.models.CallSettingsRequest
import org.openapitools.client.models.ConnectedEvent
import org.openapitools.client.models.CreateGuestRequest
import org.openapitools.client.models.CreateGuestResponse
import org.openapitools.client.models.GetCallResponse
import org.openapitools.client.models.GetOrCreateCallRequest
import org.openapitools.client.models.GetOrCreateCallResponse
import org.openapitools.client.models.GoLiveRequest
import org.openapitools.client.models.GoLiveResponse
import org.openapitools.client.models.JoinCallRequest
import org.openapitools.client.models.JoinCallResponse
import org.openapitools.client.models.ListRecordingsResponse
import org.openapitools.client.models.MemberRequest
import org.openapitools.client.models.MuteUsersResponse
import org.openapitools.client.models.PinRequest
import org.openapitools.client.models.QueryCallMembersRequest
import org.openapitools.client.models.QueryCallMembersResponse
import org.openapitools.client.models.QueryCallsRequest
import org.openapitools.client.models.RejectCallRequest
import org.openapitools.client.models.RejectCallResponse
import org.openapitools.client.models.RequestPermissionRequest
import org.openapitools.client.models.SendCallEventRequest
import org.openapitools.client.models.SendCallEventResponse
import org.openapitools.client.models.SendReactionRequest
import org.openapitools.client.models.SendReactionResponse
import org.openapitools.client.models.StartHLSBroadcastingResponse
import org.openapitools.client.models.StartRecordingRequest
import org.openapitools.client.models.StopLiveResponse
import org.openapitools.client.models.UnblockUserRequest
import org.openapitools.client.models.UnpinRequest
import org.openapitools.client.models.UpdateCallMembersRequest
import org.openapitools.client.models.UpdateCallMembersResponse
import org.openapitools.client.models.UpdateCallRequest
import org.openapitools.client.models.UpdateCallResponse
import org.openapitools.client.models.UpdateUserPermissionsResponse
import org.openapitools.client.models.UserRequest
import org.openapitools.client.models.VideoEvent
import org.openapitools.client.models.WSCallEvent
import org.webrtc.ManagedAudioProcessingFactory
import retrofit2.HttpException
import java.net.ConnectException
import java.util.*
import kotlin.coroutines.Continuation

internal const val WAIT_FOR_CONNECTION_ID_TIMEOUT = 5000L
internal const val defaultAudioUsage = AudioAttributes.USAGE_VOICE_COMMUNICATION

/**
 * @param lifecycle The lifecycle used to observe changes in the process
 */
internal class StreamVideoImpl internal constructor(
    override val context: Context,
    internal val _scope: CoroutineScope,
    override val user: User,
    internal val apiKey: ApiKey,
    internal var token: String,
    private val lifecycle: Lifecycle,
    private val loggingLevel: LoggingLevel,
    internal val connectionModule: ConnectionModule,
    internal val tokenProvider: (suspend (error: Throwable?) -> String)?,
    internal val streamNotificationManager: StreamNotificationManager,
    internal val callServiceConfig: CallServiceConfig = callServiceConfig(),
    internal val testSfuAddress: String? = null,
    internal val sounds: Sounds,
    internal val permissionCheck: StreamPermissionCheck = DefaultStreamPermissionCheck(),
    internal val crashOnMissingPermission: Boolean = false,
    internal val audioUsage: Int = defaultAudioUsage,
    internal val appName: String? = null,
    internal val audioProcessing: ManagedAudioProcessingFactory? = null,
) : StreamVideo, NotificationHandler by streamNotificationManager {

    companion object {
        var hintDomain = "https://hint.stream-io-video.com/"
    }

    private var locationJob: Deferred<Result<String>>? = null

    /** the state for the client, includes the current user */
    override val state = ClientState(this)

    private val coroutineExceptionHandler =
        CoroutineExceptionHandler { coroutineContext, exception ->
            val coroutineName = coroutineContext[CoroutineName]?.name ?: "unknown"
            logger.e(exception) {
                "[StreamVideo#Scope] Uncaught exception in coroutine $coroutineName: $exception"
            }
        }
    internal val scope =
        CoroutineScope(_scope.coroutineContext + SupervisorJob() + coroutineExceptionHandler)

    /** if true we fail fast on errors instead of logging them */

    /** session id is generated client side */
    public val sessionId = UUID.randomUUID().toString()

    internal var guestUserJob: Deferred<Unit>? = null
    private lateinit var connectContinuation: Continuation<Result<ConnectedEvent>>

    @InternalStreamVideoApi
    public var peerConnectionFactory =
        StreamPeerConnectionFactory(context, audioUsage, audioProcessing)

    public override val userId = user.id

    private val logger by taggedLogger("Call:StreamVideo")
    private var subscriptions = mutableSetOf<EventSubscription>()
    private var calls = mutableMapOf<String, Call>()

    val socketImpl = connectionModule.coordinatorSocket

    fun onCallCleanUp(call: Call) {
        calls.remove(call.cid)
    }

    override fun cleanup() {
        // remove all cached calls
        calls.clear()
        // stop all running coroutines
        scope.cancel()
        // stop the socket
        socketImpl.cleanup()
        // call cleanup on the active call
        val activeCall = state.activeCall.value
        activeCall?.leave()
        // Stop the call service if it was running
        if (callServiceConfig.runCallServiceInForeground) {
            safeCall {
                val serviceIntent = CallService.buildStopIntent(context, callServiceConfig)
                context.stopService(serviceIntent)
            }
        }
    }

    /**
     * @see StreamVideo.createDevice
     */
    override suspend fun createDevice(pushDevice: PushDevice): Result<Device> {
        return streamNotificationManager.createDevice(pushDevice)
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
                        token = newToken
                        connectionModule.updateToken(newToken)
                    }
                    // retry the API call once
                    try {
                        Success(apiCall())
                    } catch (e: HttpException) {
                        parseError(e)
                    } catch (e: Throwable) {
                        // rethrow exception (will be handled by outer-catch)
                        throw e
                    }

                    // set the token, repeat API call
                    // keep track of retry count
                } else {
                    failure
                }
            } catch (e: Throwable) {
                // Other issues. For example UnknownHostException.
                Failure(Error.ThrowableError(e.message ?: "", e))
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
            connectionModule.api.updateCall(
                type = type,
                id = id,
                updateCallRequest = request,
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
                        VideoErrorCode.PARSER_ERROR.code,
                    ),
                )
            }
        } ?: return Failure(
            Error.NetworkError("failed to parse error response from server", e.code()),
        )
        return Failure(
            Error.NetworkError(
                message = error.message,
                serverErrorCode = error.code,
                statusCode = error.statusCode,
                cause = Throwable(error.moreInfo),
            ),
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
        listener: VideoEventListener<VideoEvent>,
    ): EventSubscription {
        val sub = EventSubscription(listener)
        subscriptions.add(sub)
        return sub
    }

    override suspend fun connectIfNotAlreadyConnected() = safeSuspendingCall {
        if (connectionModule.coordinatorSocket.canConnect()) {
            connectionModule.coordinatorSocket.connect()
        }
    }

    /**
     * Observes the app lifecycle and attempts to reconnect/release the socket connection.
     */
    private val lifecycleObserver = StreamLifecycleObserver(
        lifecycle,
        object : LifecycleHandler {
            override fun started() {
                scope.launch(CoroutineName("lifecycleObserver.started")) {
                    connectIfNotAlreadyConnected()
                }
            }

            override fun stopped() {
                safeCall {
                    // We should only disconnect if we were previously connected
                    // Also don't disconnect the socket if we are in an active call
                    val socketDecision = connectionModule.coordinatorSocket.canDisconnect()
                    val activeCallDecision = state.hasActiveOrRingingCall()
                    val decision = socketDecision && !activeCallDecision
                    logger.d {
                        "[lifecycle#stopped] Decision to disconnect: $decision, caused by socket state: $socketDecision, active or ringing call: $activeCallDecision"
                    }
                    if (decision) {
                        connectionModule.coordinatorSocket.disconnect(
                            PersistentSocket.DisconnectReason.ByRequest,
                        )
                    }
                }
            }
        },
    )

    init {

        scope.launch(Dispatchers.Main.immediate + CoroutineName("init#lifecycleObserver.observe")) {
            lifecycleObserver.observe()
        }

        // listen to socket events and errors
        scope.launch(CoroutineName("init#coordinatorSocket.events.collect")) {
            connectionModule.coordinatorSocket.events.collect {
                fireEvent(it)
            }
        }
        scope.launch(CoroutineName("init#coordinatorSocket.errors.collect")) {
            connectionModule.coordinatorSocket.errors.collect { throwable ->
                if (throwable is ConnectException) {
                    state.handleError(throwable)
                } else {
                    (throwable as? ErrorResponse)?.let {
                        if (it.code == VideoErrorCode.TOKEN_EXPIRED.code) refreshToken(it)
                    }
                }
            }
        }

        scope.launch(CoroutineName("init#coordinatorSocket.connectionState.collect")) {
            connectionModule.coordinatorSocket.connectionState.collect { it ->
                // If the socket is reconnected then we have a new connection ID.
                // We need to re-watch every watched call with the new connection ID
                // (otherwise the WS events will stop)
                val watchedCalls = calls
                if (it is SocketState.Connected && watchedCalls.isNotEmpty()) {
                    val filter = Filters.`in`("cid", watchedCalls.values.map { it.cid }).toMap()
                    queryCalls(filters = filter, watch = true).also {
                        if (it is Failure) {
                            logger.e { "Failed to re-watch calls (${it.value}" }
                        }
                    }
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
        locationJob = scope.async { selectLocation() }
        return locationJob as Deferred<Result<String>>
    }

    internal suspend fun selectLocation(): Result<String> {
        var attempts = 0
        var lastResult: Result<String>?
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

    override suspend fun connectAsync(): Deferred<Result<Long>> {
        return scope.async {
            // wait for the guest user setup if we're using guest users
            guestUserJob?.await()
            try {
                val startTime = System.currentTimeMillis()
                socketImpl.connect()
                val duration = System.currentTimeMillis() - startTime
                Success(duration)
            } catch (e: ErrorResponse) {
                if (e.code == VideoErrorCode.TOKEN_EXPIRED.code) {
                    refreshToken(e)
                    Failure(Error.GenericError("Initialize error. Token expired."))
                } else {
                    throw e
                }
            }
        }
    }

    private suspend fun refreshToken(error: Throwable) {
        tokenProvider?.let {
            val newToken = tokenProvider.invoke(error)
            connectionModule.updateToken(newToken)

            logger.d { "[refreshToken] Token has been refreshed with: $newToken" }

            socketImpl.reconnect(0)
        }
    }

    /**
     * @see StreamVideo.deleteDevice
     */
    override suspend fun deleteDevice(device: Device): Result<Unit> {
        return streamNotificationManager.deleteDevice(device)
    }

    fun setupGuestUser(user: User) {
        guestUserJob = scope.async {
            val response = createGuestUser(
                userRequest = UserRequest(
                    id = user.id,
                    image = user.image,
                    name = user.name,
                    custom = user.custom,
                ),
            )
            if (response.isFailure) {
                throw IllegalStateException("Failed to create guest user")
            }
            response.onSuccess {
                connectionModule.updateAuthType("jwt")
                connectionModule.updateToken(it.accessToken)
            }
        }
    }

    suspend fun createGuestUser(userRequest: UserRequest): Result<CreateGuestResponse> {
        return wrapAPICall {
            connectionModule.api.createGuest(
                createGuestRequest = CreateGuestRequest(userRequest),
            )
        }
    }

    internal suspend fun registerPushDevice() {
        streamNotificationManager.registerPushDevice()
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

        if (selectedCid.isNotEmpty()) {
            // Special handling  for accepted events
            if (event is CallAcceptedEvent) {
                // Skip accepted events not meant for the current outgoing call.
                val currentRingingCall = state.ringingCall.value
                val state = currentRingingCall?.state?.ringingState?.value
                if (currentRingingCall != null &&
                    (state is RingingState.Outgoing || state == RingingState.Idle) &&
                    currentRingingCall.cid != event.callCid
                ) {
                    // Skip this event
                    return
                }
            }

            // Update calls as usual
            calls[selectedCid]?.let {
                it.state.handleEvent(event)
                it.session?.handleEvent(event)
                it.handleEvent(event)
            }
        }
    }

    internal suspend fun getCall(type: String, id: String): Result<GetCallResponse> {
        return wrapAPICall {
            connectionModule.api.getCall(
                type,
                id,
                connectionId = waitForConnectionId(),
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
                userId = it,
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
            notify = notify,
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
        notify: Boolean,
    ): Result<GetOrCreateCallResponse> {
        logger.d { "[getOrCreateCall] type: $type, id: $id, members: $members" }

        return wrapAPICall {
            connectionModule.api.getOrCreateCall(
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
                connectionId = waitForConnectionId(),
            )
        }
    }

    private suspend fun waitForConnectionId(): String? =
    // The Coordinator WS connection can take a moment to set up - this can be an issue
    // if we jump right into the call from a deep link and we connect the call quickly.
    // We return null on timeout. The Coordinator WS will update the connectionId later
        // after it reconnects (it will call queryCalls)
        withTimeoutOrNull(timeMillis = WAIT_FOR_CONNECTION_ID_TIMEOUT) {
            val value = connectionModule.coordinatorSocket.connectionId.first { it != null }
            value
        }.also {
            if (it == null) {
                logger.w { "[waitForConnectionId] connectionId timeout - returning null" }
            }
        }

    internal suspend fun inviteUsers(
        type: String,
        id: String,
        users: List<User>,
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
        location: String,
        migratingFrom: String?,
    ): Result<JoinCallResponse> {
        val joinCallRequest = JoinCallRequest(
            create = create,
            data = CallRequest(
                members = members,
                custom = custom,
                settingsOverride = settingsOverride,
                startsAt = startsAt,
                team = team,
            ),
            ring = ring,
            notify = notify,
            location = location,
            migratingFrom = migratingFrom,
        )

        val result = wrapAPICall {
            connectionModule.api.joinCall(
                type,
                id,
                joinCallRequest,
                waitForConnectionId(),
            )
        }
        return result
    }

    suspend fun updateMembers(
        type: String,
        id: String,
        request: UpdateCallMembersRequest,
    ): Result<UpdateCallMembersResponse> {
        return wrapAPICall {
            connectionModule.api.updateCallMembers(type, id, request)
        }
    }

    internal suspend fun sendCustomEvent(
        type: String,
        id: String,
        dataJson: Map<String, Any>,
    ): Result<SendCallEventResponse> {
        logger.d { "[sendCustomEvent] callCid: $type:$id, dataJson: $dataJson" }

        return wrapAPICall {
            connectionModule.api.sendCallEvent(
                type,
                id,
                SendCallEventRequest(custom = dataJson),
            )
        }
    }

    internal suspend fun queryMembersInternal(
        type: String,
        id: String,
        filter: Map<String, Any>?,
        sort: List<SortField>,
        prev: String?,
        next: String?,
        limit: Int,
    ): Result<QueryCallMembersResponse> {
        return wrapAPICall {
            connectionModule.api.queryCallMembers(
                QueryCallMembersRequest(
                    type = type,
                    id = id,
                    filterConditions = filter,
                    sort = sort.map { it.toRequest() },
                    prev = prev,
                    next = next,
                    limit = limit,
                ),
            )
        }
    }

    override suspend fun queryMembers(
        type: String,
        id: String,
        filter: Map<String, Any>?,
        sort: List<SortField>,
        prev: String?,
        next: String?,
        limit: Int,
    ): Result<QueriedMembers> {
        return queryMembersInternal(
            type = type,
            id = id,
            filter = filter,
            sort = sort,
            prev = prev,
            next = next,
            limit = limit,
        ).map { it.toQueriedMembers() }
    }

    suspend fun blockUser(type: String, id: String, userId: String): Result<BlockUserResponse> {
        logger.d { "[blockUser] callCid: $type:$id, userId: $userId" }

        return wrapAPICall {
            connectionModule.api.blockUser(
                type,
                id,
                BlockUserRequest(userId),
            )
        }
    }

    suspend fun unblockUser(type: String, id: String, userId: String): Result<Unit> {
        logger.d { "[unblockUser] callCid: $type:$id, userId: $userId" }

        return wrapAPICall {
            connectionModule.api.unblockUser(
                type,
                id,
                UnblockUserRequest(userId),
            )
        }
    }

    suspend fun pinForEveryone(type: String, callId: String, sessionId: String, userId: String) =
        wrapAPICall {
            connectionModule.api.videoPin(
                type,
                callId,
                PinRequest(
                    sessionId,
                    userId,
                ),
            )
        }

    suspend fun unpinForEveryone(type: String, callId: String, sessionId: String, userId: String) =
        wrapAPICall {
            connectionModule.api.videoUnpin(
                type,
                callId,
                UnpinRequest(
                    sessionId,
                    userId,
                ),
            )
        }

    suspend fun endCall(type: String, id: String): Result<Unit> {
        return wrapAPICall { connectionModule.api.endCall(type, id) }
    }

    suspend fun goLive(
        type: String,
        id: String,
        startHls: Boolean,
        startRecording: Boolean,
        startTranscription: Boolean,
    ): Result<GoLiveResponse> {
        logger.d { "[goLive] callCid: $type:$id" }

        return wrapAPICall {
            connectionModule.api.goLive(
                type = type,
                id = id,
                goLiveRequest = GoLiveRequest(
                    startHls = startHls,
                    startRecording = startRecording,
                    startTranscription = startTranscription,
                ),
            )
        }
    }

    suspend fun stopLive(type: String, id: String): Result<StopLiveResponse> {
        return wrapAPICall { connectionModule.api.stopLive(type, id) }
    }

    suspend fun muteUsers(
        type: String,
        id: String,
        muteUsersData: MuteUsersData,
    ): Result<MuteUsersResponse> {
        val request = muteUsersData.toRequest()
        return wrapAPICall {
            connectionModule.api.muteUsers(type, id, request)
        }
    }

    /**
     * @see StreamVideo.queryCalls
     */
    override suspend fun queryCalls(
        filters: Map<String, Any>,
        sort: List<SortField>,
        limit: Int,
        prev: String?,
        next: String?,
        watch: Boolean,
    ): Result<QueriedCalls> {
        logger.d { "[queryCalls] filters: $filters, sort: $sort, limit: $limit, watch: $watch" }
        val request = QueryCallsRequest(
            filterConditions = filters,
            sort = sort.map { it.toRequest() },
            limit = limit,
            prev = prev,
            next = next,
            watch = watch,
        )
        val result = wrapAPICall {
            connectionModule.api.queryCalls(request, waitForConnectionId())
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

        return result.map { it.toQueriedCalls() }
    }

    suspend fun requestPermissions(
        type: String,
        id: String,
        permissions: List<String>,
    ): Result<Unit> {
        logger.d { "[requestPermissions] callCid: $type:$id, permissions: $permissions" }

        return wrapAPICall {
            connectionModule.api.requestPermission(
                type,
                id,
                RequestPermissionRequest(permissions),
            )
        }
    }

    suspend fun startBroadcasting(type: String, id: String): Result<StartHLSBroadcastingResponse> {
        logger.d { "[startBroadcasting] callCid: $type $id" }

        return wrapAPICall { connectionModule.api.startHLSBroadcasting(type, id) }
    }

    suspend fun stopBroadcasting(type: String, id: String): Result<Unit> {
        return wrapAPICall { connectionModule.api.stopHLSBroadcasting(type, id) }
    }

    suspend fun startRecording(
        type: String,
        id: String,
        externalStorage: String? = null,
    ): Result<Unit> {
        return wrapAPICall {
            val req = StartRecordingRequest(externalStorage)
            connectionModule.api.startRecording(type, id, req)
        }
    }

    suspend fun stopRecording(type: String, id: String): Result<Unit> {
        return wrapAPICall {
            connectionModule.api.stopRecording(type, id)
        }
    }

    suspend fun updateUserPermissions(
        type: String,
        id: String,
        updateUserPermissionsData: UpdateUserPermissionsData,
    ): Result<UpdateUserPermissionsResponse> {
        return wrapAPICall {
            connectionModule.api.updateUserPermissions(
                type,
                id,
                updateUserPermissionsData.toRequest(),
            )
        }
    }

    suspend fun listRecordings(
        type: String,
        id: String,
        sessionId: String?,
    ): Result<ListRecordingsResponse> {
        return wrapAPICall {
            connectionModule.api.listRecordings(type, id)
        }
    }

    suspend fun sendReaction(
        callType: String,
        id: String,
        type: String,
        emoji: String? = null,
        custom: Map<String, Any>? = null,
    ): Result<SendReactionResponse> {
        val request = SendReactionRequest(type, custom, emoji)

        logger.d { "[sendVideoReaction] callCid: $type:$id, sendReactionData: $request" }

        return wrapAPICall {
            connectionModule.api.sendVideoReaction(callType, id, request)
        }
    }

    /**
     * @see StreamVideo.getEdges
     */
    override suspend fun getEdges(): Result<List<EdgeData>> {
        logger.d { "[getEdges] no params" }

        return wrapAPICall {
            val result = connectionModule.api.getEdges()

            result.edges.map { it.toEdge() }
        }
    }

    /**
     * @see StreamVideo.logOut
     */
    override fun logOut() {
        scope.launch(
            CoroutineName("logOut"),
        ) { streamNotificationManager.deviceTokenStorage.clear() }
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

    @OptIn(InternalCoroutinesApi::class)
    suspend fun _selectLocation(): Result<String> {
        return wrapAPICall {
            val url = hintDomain
            val request: Request = Request.Builder().url(url).method("HEAD", null).build()
            val call = connectionModule.okHttpClient.newCall(request)
            val response = suspendCancellableCoroutine { continuation ->
                call.enqueue(object : Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        continuation.tryResumeWithException(e)?.let {
                            continuation.completeResume(it)
                        }
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
            connectionModule.api.acceptCall(type, id)
        }
    }

    internal suspend fun reject(
        type: String,
        id: String,
        reason: RejectReason? = null,
    ): Result<RejectCallResponse> {
        return wrapAPICall {
            connectionModule.api.rejectCall(type, id, RejectCallRequest(reason?.alias))
        }
    }

    internal suspend fun notify(type: String, id: String): Result<GetCallResponse> {
        return wrapAPICall {
            connectionModule.api.getCall(type, id, notify = true)
        }
    }

    internal suspend fun ring(type: String, id: String): Result<GetCallResponse> {
        return wrapAPICall {
            connectionModule.api.getCall(type, id, ring = true)
        }
    }

    internal fun isAudioProcessingEnabled(): Boolean {
        return peerConnectionFactory.isAudioProcessingEnabled()
    }

    internal fun setAudioProcessingEnabled(enabled: Boolean) {
        return peerConnectionFactory.setAudioProcessingEnabled(enabled)
    }

    internal fun toggleAudioProcessing(): Boolean {
        return peerConnectionFactory.toggleAudioProcessing()
    }
}

/** Extension function that makes it easy to use on kotlin, but keeps Java usable as well */
public inline fun <reified T : VideoEvent> StreamVideo.subscribeFor(
    listener: VideoEventListener<T>,
): EventSubscription {
    return this.subscribeFor(
        T::class.java,
        listener = { event ->
            listener.onEvent(event as T)
        },
    )
}
