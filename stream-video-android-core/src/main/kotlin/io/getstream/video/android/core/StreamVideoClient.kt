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
import io.getstream.android.video.generated.models.CallAcceptedEvent
import io.getstream.android.video.generated.models.CreateGuestRequest
import io.getstream.android.video.generated.models.CreateGuestResponse
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
import io.getstream.video.android.core.internal.RtcSessionFactory
import io.getstream.video.android.core.internal.VideoApi
import io.getstream.video.android.core.internal.module.CoordinatorConnectionModule
import io.getstream.video.android.core.internal.network.AuthTypeProvider
import io.getstream.video.android.core.model.EdgeData
import io.getstream.video.android.core.model.QueriedCalls
import io.getstream.video.android.core.model.QueriedMembers
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.notifications.NotificationHandler
import io.getstream.video.android.core.notifications.internal.StreamNotificationManager
import io.getstream.video.android.core.notifications.internal.service.ANY_MARKER
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfigRegistry
import io.getstream.video.android.core.permission.android.DefaultStreamPermissionCheck
import io.getstream.video.android.core.permission.android.StreamPermissionCheck
import io.getstream.video.android.core.socket.ErrorResponse
import io.getstream.video.android.core.socket.common.scope.ClientScope
import io.getstream.video.android.core.socket.coordinator.state.VideoSocketState
import io.getstream.video.android.core.sounds.Sounds
import io.getstream.video.android.core.utils.LatencyResult
import io.getstream.video.android.core.utils.getLatencyMeasurementsOKHttp
import io.getstream.video.android.core.utils.safeCall
import io.getstream.video.android.core.utils.safeSuspendingCall
import io.getstream.video.android.core.utils.safeSuspendingCallWithResult
import io.getstream.video.android.core.utils.toQueriedCalls
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.webrtc.ManagedAudioProcessingFactory
import retrofit2.HttpException
import java.util.*

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
    internal val videoApi: VideoApi,
    internal val streamNotificationManager: StreamNotificationManager,
    internal val enableCallNotificationUpdates: Boolean,
    internal val callServiceConfigRegistry: CallServiceConfigRegistry = CallServiceConfigRegistry(),
    internal val sounds: Sounds,
    internal val permissionCheck: StreamPermissionCheck = DefaultStreamPermissionCheck(),
    internal val crashOnMissingPermission: Boolean = false,
    internal val appName: String? = null,
    internal val audioProcessing: ManagedAudioProcessingFactory? = null,
    internal val leaveAfterDisconnectSeconds: Long = 30,
    internal val appVersion: String? = null,
    internal val enableCallUpdatesAfterLeave: Boolean = false,
    internal val rtcSessionFactory: RtcSessionFactory,
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
                coordinatorConnectionModule.updateToken(null)
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
                    Failure(Error.GenericError("Initialize error. Token expired."))
                } else {
                    Failure(Error.ThrowableError(e.message, e))
                }
            }
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
                coordinatorConnectionModule.updateAuthType(AuthTypeProvider.AuthType.JWT)
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
            safeCall {
                destroyedCalls.snapshot().forEach { (_, call) ->
                    call.fireEvent(event)
                }
            }
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
            safeCall {
                destroyedCalls.snapshot().forEach { (_, call) ->
                    call.let {
                        // No session here
                        it.state.handleEvent(event)
                        it.handleEvent(event)
                    }
                }
            }
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

    override suspend fun queryMembers(
        type: String,
        id: String,
        filter: Map<String, Any>?,
        sort: List<SortField>,
        prev: String?,
        next: String?,
        limit: Int,
    ): Result<QueriedMembers> {
        return videoApi.queryCallMembers(
            type = type,
            id = id,
            filter = filter,
            sort = sort,
            prev = prev,
            next = next,
            limit = limit,
        ).await()
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
        val result = videoApi.oldQueryCalls(
            filters,
            sort,
            limit,
            prev,
            next,
            watch,
        ).await()
            .onSuccess {
                it.calls.forEach { callData ->
                    val call = this.call(callData.call.type, callData.call.id)
                    call.state.updateFromResponse(callData)
                }
            }

        return result.map { it.toQueriedCalls() }
    }

    /**
     * @see StreamVideo.getEdges
     */
    override suspend fun getEdges(): Result<List<EdgeData>> {
        logger.d { "[getEdges] no params" }
        return videoApi.getEdges().await()
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
            val call = Call(this, videoApi, rtcSessionFactory, type, idOrRandom, user)
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
