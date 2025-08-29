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
import androidx.collection.LruCache
import androidx.lifecycle.Lifecycle
import io.getstream.android.push.PushDevice
import io.getstream.android.video.generated.models.AcceptCallResponse
import io.getstream.android.video.generated.models.BlockUserRequest
import io.getstream.android.video.generated.models.BlockUserResponse
import io.getstream.android.video.generated.models.CallAcceptedEvent
import io.getstream.android.video.generated.models.CallRequest
import io.getstream.android.video.generated.models.CallSettingsRequest
import io.getstream.android.video.generated.models.CollectUserFeedbackRequest
import io.getstream.android.video.generated.models.ConnectedEvent
import io.getstream.android.video.generated.models.CreateGuestRequest
import io.getstream.android.video.generated.models.CreateGuestResponse
import io.getstream.android.video.generated.models.GetCallResponse
import io.getstream.android.video.generated.models.GetOrCreateCallRequest
import io.getstream.android.video.generated.models.GetOrCreateCallResponse
import io.getstream.android.video.generated.models.GoLiveRequest
import io.getstream.android.video.generated.models.GoLiveResponse
import io.getstream.android.video.generated.models.JoinCallRequest
import io.getstream.android.video.generated.models.JoinCallResponse
import io.getstream.android.video.generated.models.ListRecordingsResponse
import io.getstream.android.video.generated.models.ListTranscriptionsResponse
import io.getstream.android.video.generated.models.MemberRequest
import io.getstream.android.video.generated.models.MuteUsersResponse
import io.getstream.android.video.generated.models.PinRequest
import io.getstream.android.video.generated.models.QueryCallMembersRequest
import io.getstream.android.video.generated.models.QueryCallMembersResponse
import io.getstream.android.video.generated.models.QueryCallsRequest
import io.getstream.android.video.generated.models.RejectCallRequest
import io.getstream.android.video.generated.models.RejectCallResponse
import io.getstream.android.video.generated.models.RequestPermissionRequest
import io.getstream.android.video.generated.models.SendCallEventRequest
import io.getstream.android.video.generated.models.SendCallEventResponse
import io.getstream.android.video.generated.models.SendReactionRequest
import io.getstream.android.video.generated.models.SendReactionResponse
import io.getstream.android.video.generated.models.StartClosedCaptionsRequest
import io.getstream.android.video.generated.models.StartClosedCaptionsResponse
import io.getstream.android.video.generated.models.StartHLSBroadcastingResponse
import io.getstream.android.video.generated.models.StartRecordingRequest
import io.getstream.android.video.generated.models.StartTranscriptionRequest
import io.getstream.android.video.generated.models.StartTranscriptionResponse
import io.getstream.android.video.generated.models.StopClosedCaptionsRequest
import io.getstream.android.video.generated.models.StopClosedCaptionsResponse
import io.getstream.android.video.generated.models.StopLiveRequest
import io.getstream.android.video.generated.models.StopLiveResponse
import io.getstream.android.video.generated.models.StopTranscriptionRequest
import io.getstream.android.video.generated.models.StopTranscriptionResponse
import io.getstream.android.video.generated.models.UnblockUserRequest
import io.getstream.android.video.generated.models.UnpinRequest
import io.getstream.android.video.generated.models.UpdateCallMembersRequest
import io.getstream.android.video.generated.models.UpdateCallMembersResponse
import io.getstream.android.video.generated.models.UpdateCallRequest
import io.getstream.android.video.generated.models.UpdateCallResponse
import io.getstream.android.video.generated.models.UpdateUserPermissionsResponse
import io.getstream.android.video.generated.models.UserRequest
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.android.video.generated.models.WSCallEvent
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.video.android.core.errors.VideoErrorCode
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.filter.Filters
import io.getstream.video.android.core.filter.toMap
import io.getstream.video.android.core.internal.module.CoordinatorConnectionModule
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
import io.getstream.video.android.core.notifications.internal.service.ANY_MARKER
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfigRegistry
import io.getstream.video.android.core.permission.android.DefaultStreamPermissionCheck
import io.getstream.video.android.core.permission.android.StreamPermissionCheck
import io.getstream.video.android.core.socket.ErrorResponse
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.common.token.ConstantTokenProvider
import io.getstream.video.android.core.socket.common.token.TokenProvider
import io.getstream.video.android.core.socket.coordinator.state.VideoSocketState
import io.getstream.video.android.core.sounds.Sounds
import io.getstream.video.android.core.utils.LatencyResult
import io.getstream.video.android.core.utils.getLatencyMeasurementsOKHttp
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.safeSuspendingCall
import io.getstream.video.android.core.utils.safeSuspendingCallWithResult
import io.getstream.video.android.core.utils.toEdge
import io.getstream.video.android.core.utils.toQueriedCalls
import io.getstream.video.android.core.utils.toQueriedMembers
import io.getstream.video.android.model.ApiKey
import io.getstream.video.android.model.Device
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.webrtc.ManagedAudioProcessingFactory
import retrofit2.HttpException
import java.util.*
import kotlin.coroutines.Continuation

internal const val WAIT_FOR_CONNECTION_ID_TIMEOUT = 5000L
internal const val defaultAudioUsage = AudioAttributes.USAGE_VOICE_COMMUNICATION

/**
 * @param lifecycle The lifecycle used to observe changes in the process
 */
internal class StreamVideoClient internal constructor(
    override val context: Context,
    internal val scope: CoroutineScope = ClientScope(),
    override val user: User,
    internal val apiKey: ApiKey,
    internal var token: String,
    private val lifecycle: Lifecycle,
    internal val coordinatorConnectionModule: CoordinatorConnectionModule,
    internal val tokenProvider: TokenProvider = ConstantTokenProvider(token),
    internal val streamNotificationManager: StreamNotificationManager,
    internal val enableCallNotificationUpdates: Boolean,
    internal val callServiceConfigRegistry: CallServiceConfigRegistry = CallServiceConfigRegistry(),
    internal val testSfuAddress: String? = null,
    internal val sounds: Sounds,
    internal val permissionCheck: StreamPermissionCheck = DefaultStreamPermissionCheck(),
    internal val crashOnMissingPermission: Boolean = false,
    internal val appName: String? = null,
    internal val audioProcessing: ManagedAudioProcessingFactory? = null,
    internal val leaveAfterDisconnectSeconds: Long = 30,
    internal val appVersion: String? = null,
    internal val enableCallUpdatesAfterLeave: Boolean = false,
    internal val enableStatsCollection: Boolean = true,
    internal val enableStereoForSubscriber: Boolean = true,
) : StreamVideo, NotificationHandler by streamNotificationManager {

    private var locationJob: Deferred<Result<String>>? = null

    /** the state for the client, includes the current user */
    override val state = ClientState(this)

    /**
     * Can be set from tests to be returned as a session id for the coordinator.
     */
    internal var testSessionId: String? = null

    /** if true we fail fast on errors instead of logging them */

    internal var guestUserJob: Deferred<Unit>? = null
    private lateinit var connectContinuation: Continuation<Result<ConnectedEvent>>

    public override val userId = user.id

    private val logger by taggedLogger("Call:StreamVideo")
    private var subscriptions = mutableSetOf<EventSubscription>()
    private var calls = mutableMapOf<String, Call>()
    private val destroyedCalls = LruCache<Int, Call>(maxSize = 100)

    val socketImpl = coordinatorConnectionModule.socketConnection

    fun onCallCleanUp(call: Call) {
        if (enableCallUpdatesAfterLeave) {
            logger.d { "[cleanup] Call updates are required, preserve the instance: ${call.cid}" }
            destroyedCalls.put(call.hashCode(), call)
        }
        logger.d { "[cleanup] Removing call from cache: ${call.cid}" }
        calls.remove(call.cid)
    }

    override fun cleanup() {
        // remove all cached calls
        calls.clear()
        destroyedCalls.evictAll()
        // stop all running coroutines
        scope.cancel()
        // call cleanup on the active call
        val activeCall = state.activeCall.value
        // Stop the call service if it was running

        val callConfig = callServiceConfigRegistry.get(activeCall?.type ?: ANY_MARKER)
        val runCallServiceInForeground = callConfig.runCallServiceInForeground
        if (runCallServiceInForeground) {
            safeCall {
                val serviceIntent = CallService.buildStopIntent(
                    context = context,
                    callServiceConfiguration = callConfig,
                )
                context.stopService(serviceIntent)
            }
        }
        activeCall?.leave()
    }

    /**
     * @see StreamVideo.createDevice
     */
    override suspend fun createDevice(pushDevice: PushDevice): Result<Device> {
        return streamNotificationManager.createDevice(pushDevice)
    }

    /**
     * @see StreamVideo.getDevice
     */
    override fun getDevice(): Flow<Device?> {
        return streamNotificationManager.getDevice()
    }

    /**
     * @see StreamVideo.updateDevice
     */
    override suspend fun updateDevice(device: Device?) {
        return streamNotificationManager.updateDevice(device)
    }

    /**
     * Ensure that every API call runs on the IO dispatcher and has correct error handling
     */
    internal suspend fun <T : Any> apiCall(
        apiCall: suspend () -> T,
    ): Result<T> = safeSuspendingCallWithResult {
        try {
            apiCall()
        } catch (e: HttpException) {
            // Retry once with a new token if the token is expired
            if (e.isAuthError()) {
                val newToken = tokenProvider.loadToken()
                token = newToken
                coordinatorConnectionModule.updateToken(newToken)
                apiCall()
            } else {
                throw e
            }
        }
    }

    private fun HttpException.isAuthError(): Boolean {
        val failure = parseError(this)
        val parsedError = failure.value as Error.NetworkError
        return when (parsedError.serverErrorCode) {
            VideoErrorCode.AUTHENTICATION_ERROR.code,
            VideoErrorCode.TOKEN_EXPIRED.code,
            VideoErrorCode.TOKEN_NOT_VALID.code,
            VideoErrorCode.TOKEN_DATE_INCORRECT.code,
            VideoErrorCode.TOKEN_SIGNATURE_INCORRECT.code,
                -> true

            else -> false
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
        return apiCall {
            coordinatorConnectionModule.api.updateCall(
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
        coordinatorConnectionModule.socketConnection.connect(user)
    }

    init {
        // listen to socket events and errors
        scope.launch(CoroutineName("init#coordinatorSocket.events.collect")) {
            coordinatorConnectionModule.socketConnection.events().collect {
                fireEvent(it)
            }
        }
        scope.launch {
            coordinatorConnectionModule.socketConnection.state().collect {
                state.handleState(it)
            }
        }

        scope.launch(CoroutineName("init#coordinatorSocket.errors.collect")) {
            coordinatorConnectionModule.socketConnection.errors().collect { error ->
                state.handleError(error.streamError)
            }
        }

        scope.launch(CoroutineName("init#coordinatorSocket.connectionState.collect")) {
            coordinatorConnectionModule.socketConnection.state().collect { it ->
                // If the socket is reconnected then we have a new connection ID.
                // We need to re-watch every watched call with the new connection ID
                // (otherwise the WS events will stop)
                val watchedCalls = calls
                if (it is VideoSocketState.Connected && watchedCalls.isNotEmpty()) {
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
                socketImpl.connect(user)
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
            val newToken = tokenProvider.loadToken()
            coordinatorConnectionModule.updateToken(newToken)

            logger.d { "[refreshToken] Token has been refreshed with: $newToken" }

            socketImpl.reconnect(user, true)
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
                coordinatorConnectionModule.updateAuthType("jwt")
                coordinatorConnectionModule.updateToken(it.accessToken)
            }
        }
    }

    suspend fun createGuestUser(userRequest: UserRequest): Result<CreateGuestResponse> {
        return apiCall {
            coordinatorConnectionModule.api.createGuest(
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
            notifyDestroyedCalls(event)
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
            deliverIntentToDestroyedCalls(event)
        }
    }

    private fun shouldProcessDestroyedCall(event: VideoEvent, callCid: String): Boolean {
        return when (event) {
            is WSCallEvent -> event.getCallCID() == callCid
            else -> true
        }
    }

    private fun deliverIntentToDestroyedCalls(event: VideoEvent) {
        safeCall {
            destroyedCalls.snapshot().forEach { (_, call) ->
                call.let {
                    if (shouldProcessDestroyedCall(event, call.cid)) {
                        it.state.handleEvent(event)
                        it.handleEvent(event)
                    }
                }
            }
        }
    }

    private fun notifyDestroyedCalls(event: VideoEvent) {
        safeCall {
            destroyedCalls.snapshot().forEach { (_, call) ->
                if (shouldProcessDestroyedCall(event, call.cid)) {
                    call.fireEvent(event)
                }
            }
        }
    }

    internal suspend fun getCall(type: String, id: String): Result<GetCallResponse> {
        return apiCall {
            coordinatorConnectionModule.api.getCall(
                type = type,
                id = id,
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
        video: Boolean?,
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
            video = video,
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
        video: Boolean?,
    ): Result<GetOrCreateCallResponse> {
        logger.d { "[getOrCreateCall] type: $type, id: $id, members: $members" }

        return apiCall {
            coordinatorConnectionModule.api.getOrCreateCall(
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
                    video = video,
                ),
                connectionId = waitForConnectionId(),
            )
        }
    }

    private suspend fun waitForConnectionId(): String? {
        // The Coordinator WS connection can take a moment to set up - this can be an issue
        // if we jump right into the call from a deep link and we connect the call quickly.
        // We return null on timeout. The Coordinator WS will update the connectionId later
        // after it reconnects (it will call queryCalls)
        val connectionId = withTimeoutOrNull(timeMillis = WAIT_FOR_CONNECTION_ID_TIMEOUT) {
            val value =
                coordinatorConnectionModule.socketConnection.connectionId().first { it != null }
            value
        }.also {
            logger.d { "[waitForConnectionId]: $it" }
        }
        return connectionId ?: testSessionId
    }

    internal suspend fun inviteUsers(
        type: String,
        id: String,
        users: List<User>,
    ): Result<Unit> {
        logger.d { "[inviteUsers] users: $users" }

        return apiCall {
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

        val result = apiCall {
            coordinatorConnectionModule.api.joinCall(
                type = type,
                id = id,
                joinCallRequest = joinCallRequest,
                connectionId = waitForConnectionId(),
            )
        }
        return result
    }

    suspend fun updateMembers(
        type: String,
        id: String,
        request: UpdateCallMembersRequest,
    ): Result<UpdateCallMembersResponse> {
        return apiCall {
            coordinatorConnectionModule.api.updateCallMembers(type, id, request)
        }
    }

    internal suspend fun sendCustomEvent(
        type: String,
        id: String,
        dataJson: Map<String, Any>,
    ): Result<SendCallEventResponse> {
        logger.d { "[sendCustomEvent] callCid: $type:$id, dataJson: $dataJson" }

        return apiCall {
            coordinatorConnectionModule.api.sendCallEvent(
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
        return apiCall {
            coordinatorConnectionModule.api.queryCallMembers(
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

        return apiCall {
            coordinatorConnectionModule.api.blockUser(
                type,
                id,
                BlockUserRequest(userId),
            )
        }
    }

    suspend fun unblockUser(type: String, id: String, userId: String): Result<Unit> {
        logger.d { "[unblockUser] callCid: $type:$id, userId: $userId" }

        return apiCall {
            coordinatorConnectionModule.api.unblockUser(
                type,
                id,
                UnblockUserRequest(userId),
            )
        }
    }

    suspend fun pinForEveryone(type: String, callId: String, sessionId: String, userId: String) =
        apiCall {
            coordinatorConnectionModule.api.videoPin(
                type,
                callId,
                PinRequest(
                    sessionId,
                    userId,
                ),
            )
        }

    suspend fun unpinForEveryone(type: String, callId: String, sessionId: String, userId: String) =
        apiCall {
            coordinatorConnectionModule.api.videoUnpin(
                type,
                callId,
                UnpinRequest(
                    sessionId,
                    userId,
                ),
            )
        }

    suspend fun endCall(type: String, id: String): Result<Unit> {
        return apiCall { coordinatorConnectionModule.api.endCall(type, id) }
    }

    suspend fun goLive(
        type: String,
        id: String,
        startHls: Boolean,
        startRecording: Boolean,
        startTranscription: Boolean,
    ): Result<GoLiveResponse> {
        logger.d { "[goLive] callCid: $type:$id" }

        return apiCall {
            coordinatorConnectionModule.api.goLive(
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
        return apiCall { coordinatorConnectionModule.api.stopLive(type, id, StopLiveRequest()) }
    }

    suspend fun muteUsers(
        type: String,
        id: String,
        muteUsersData: MuteUsersData,
    ): Result<MuteUsersResponse> {
        val request = muteUsersData.toRequest()
        return apiCall {
            coordinatorConnectionModule.api.muteUsers(type, id, request)
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
        val result = apiCall {
            coordinatorConnectionModule.api.queryCalls(
                queryCallsRequest = request,
                connectionId = waitForConnectionId(),
            )
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

        return apiCall {
            coordinatorConnectionModule.api.requestPermission(
                type,
                id,
                RequestPermissionRequest(permissions),
            )
        }
    }

    suspend fun startBroadcasting(type: String, id: String): Result<StartHLSBroadcastingResponse> {
        logger.d { "[startBroadcasting] callCid: $type $id" }

        return apiCall { coordinatorConnectionModule.api.startHLSBroadcasting(type, id) }
    }

    suspend fun stopBroadcasting(type: String, id: String): Result<Unit> {
        return apiCall { coordinatorConnectionModule.api.stopHLSBroadcasting(type, id) }
    }

    suspend fun startRecording(
        type: String,
        id: String,
        externalStorage: String? = null,
    ): Result<Unit> {
        return apiCall {
            val req = StartRecordingRequest(externalStorage)
            coordinatorConnectionModule.api.startRecording(type, id, req)
        }
    }

    suspend fun stopRecording(type: String, id: String): Result<Unit> {
        return apiCall {
            coordinatorConnectionModule.api.stopRecording(type, id)
        }
    }

    suspend fun updateUserPermissions(
        type: String,
        id: String,
        updateUserPermissionsData: UpdateUserPermissionsData,
    ): Result<UpdateUserPermissionsResponse> {
        return apiCall {
            coordinatorConnectionModule.api.updateUserPermissions(
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
        return apiCall {
            coordinatorConnectionModule.api.listRecordings(type, id)
        }
    }

    suspend fun sendReaction(
        callType: String,
        id: String,
        type: String,
        emoji: String? = null,
        custom: Map<String, Any>? = null,
    ): Result<SendReactionResponse> {
        val request = SendReactionRequest(type, custom = custom, emojiCode = emoji)

        logger.d { "[sendVideoReaction] callCid: $type:$id, sendReactionData: $request" }

        return apiCall {
            coordinatorConnectionModule.api.sendVideoReaction(callType, id, request)
        }
    }

    internal suspend fun collectFeedback(
        callType: String,
        id: String,
        sessionId: String,
        rating: Int,
        reason: String?,
        custom: Map<String, Any>?,
    ) = apiCall {
        coordinatorConnectionModule.api.collectUserFeedback(
            type = callType,
            id = id,
            collectUserFeedbackRequest = CollectUserFeedbackRequest(
                rating = rating,
                sdk = "stream-video-android",
                userSessionId = sessionId,
                sdkVersion = BuildConfig.STREAM_VIDEO_VERSION,
                reason = reason,
                custom = custom,
            ),
        )
    }

    /**
     * @see StreamVideo.getEdges
     */
    override suspend fun getEdges(): Result<List<EdgeData>> {
        logger.d { "[getEdges] no params" }

        return apiCall {
            val result = coordinatorConnectionModule.api.getEdges()

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
        return apiCall {
            val url = "https://hint.stream-io-video.com/"
            val request: Request = Request.Builder().url(url).method("HEAD", null).build()
            val call = coordinatorConnectionModule.http.newCall(request)
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
        return apiCall {
            coordinatorConnectionModule.api.acceptCall(type, id)
        }
    }

    internal suspend fun reject(
        type: String,
        id: String,
        reason: RejectReason? = null,
    ): Result<RejectCallResponse> {
        return apiCall {
            coordinatorConnectionModule.api.rejectCall(type, id, RejectCallRequest(reason?.alias))
        }
    }

    internal suspend fun notify(type: String, id: String): Result<GetCallResponse> {
        return apiCall {
            coordinatorConnectionModule.api.getCall(type = type, id = id, notify = true)
        }
    }

    internal suspend fun ring(type: String, id: String): Result<GetCallResponse> {
        return apiCall {
            coordinatorConnectionModule.api.getCall(type = type, id = id, ring = true)
        }
    }

    suspend fun startTranscription(
        type: String,
        id: String,
        externalStorage: String? = null,
    ): Result<StartTranscriptionResponse> {
        return apiCall {
            val startTranscriptionRequest =
                StartTranscriptionRequest(transcriptionExternalStorage = externalStorage)
            coordinatorConnectionModule.api.startTranscription(type, id, startTranscriptionRequest)
        }
    }

    suspend fun stopTranscription(type: String, id: String): Result<StopTranscriptionResponse> {
        return apiCall {
            coordinatorConnectionModule.api.stopTranscription(type, id, StopTranscriptionRequest())
        }
    }

    suspend fun listTranscription(type: String, id: String): Result<ListTranscriptionsResponse> {
        return apiCall {
            coordinatorConnectionModule.api.listTranscriptions(type, id)
        }
    }

    suspend fun startClosedCaptions(type: String, id: String): Result<StartClosedCaptionsResponse> {
        return apiCall {
            coordinatorConnectionModule.api.startClosedCaptions(
                type,
                id,
                StartClosedCaptionsRequest(),
            )
        }
    }

    suspend fun stopClosedCaptions(type: String, id: String): Result<StopClosedCaptionsResponse> {
        return apiCall {
            coordinatorConnectionModule.api.stopClosedCaptions(
                type,
                id,
                StopClosedCaptionsRequest(),
            )
        }
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