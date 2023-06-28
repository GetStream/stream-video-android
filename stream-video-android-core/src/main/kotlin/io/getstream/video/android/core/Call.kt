/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

import android.os.Build
import android.view.View
import androidx.annotation.VisibleForTesting
import io.getstream.log.taggedLogger
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.core.model.MuteUsersData
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.model.UpdateUserPermissionsData
import io.getstream.video.android.core.model.toIceServer
import io.getstream.video.android.model.User
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.openapitools.client.models.AcceptCallResponse
import org.openapitools.client.models.BlockUserResponse
import org.openapitools.client.models.CallSettingsRequest
import org.openapitools.client.models.GetCallResponse
import org.openapitools.client.models.GetOrCreateCallResponse
import org.openapitools.client.models.GoLiveResponse
import org.openapitools.client.models.JoinCallResponse
import org.openapitools.client.models.ListRecordingsResponse
import org.openapitools.client.models.MemberRequest
import org.openapitools.client.models.MuteUsersResponse
import org.openapitools.client.models.QueryMembersResponse
import org.openapitools.client.models.RejectCallResponse
import org.openapitools.client.models.SendEventResponse
import org.openapitools.client.models.SendReactionResponse
import org.openapitools.client.models.StopLiveResponse
import org.openapitools.client.models.UpdateCallMembersRequest
import org.openapitools.client.models.UpdateCallMembersResponse
import org.openapitools.client.models.UpdateCallRequest
import org.openapitools.client.models.UpdateCallResponse
import org.openapitools.client.models.UpdateUserPermissionsResponse
import org.openapitools.client.models.VideoEvent
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.ChronoUnit
import org.webrtc.PeerConnection
import org.webrtc.RendererCommon
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension

/**
 * Monitors
 * - Publisher and subscriber Peer connection states -> immediately reconnect
 * - Network up/down -> mark down instantly when down. reconnect when up
 * - Interval every 2 seconds. check and decide what to do
 *
 * Calls call.reconnectOrSwitchSfu() when needed
 *
 * Notes
 * - There is a delay after a restart till connections show healthy again
 * - So we shouldn't immediately try to reconnect if we're already reconnecting
 *
 */
public class CallHealthMonitor(val call: Call, val callScope: CoroutineScope) {
    private val logger by taggedLogger("Call:HealthMonitor")

    private val network by lazy { call.clientImpl.connectionModule.networkStateProvider }

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(callScope.coroutineContext + supervisorJob)

    // ensures we don't attempt to reconnect, if we attempted to reconnect less than 700ms ago
    var reconnectInProgress: Boolean = false
    var reconnectionAttempts = 0
    val checkInterval = 5000L
    var lastReconnectAt: OffsetDateTime? = null
    val reconnectDebounceMs = 700L

    val badStates = listOf(
        PeerConnection.PeerConnectionState.DISCONNECTED,
        PeerConnection.PeerConnectionState.FAILED,
        PeerConnection.PeerConnectionState.CLOSED
    )
    val goodStates = listOf(
        PeerConnection.PeerConnectionState.NEW, // New is good, means we're not using it yet
        PeerConnection.PeerConnectionState.CONNECTED,
        PeerConnection.PeerConnectionState.CONNECTING,
    )

    fun start() {
        logger.i { "starting call health monitor" }
        network.subscribe(networkStateListener)
        monitorPeerConnection()
        monitorInterval()
    }

    fun stop() {
        supervisorJob.cancel()
        network.unsubscribe(networkStateListener)
    }

    /**
     * Checks the peer connection states.
     * Launches reconnect() if not healthy
     */
    @Synchronized
    fun check() {
        val subscriberState = call.session?.subscriber?.state?.value
        val publisherState = call.session?.publisher?.state?.value
        val healthyPeerConnections = subscriberState in goodStates && publisherState in goodStates

        logger.d { "checking call health: peers are healthy: $healthyPeerConnections publisher $publisherState subscriber $subscriberState" }

        if (healthyPeerConnections) {
            // don't reconnect if things are healthy
            reconnectionAttempts = 0
            lastReconnectAt = null
            if (call.state._connection.value != RealtimeConnection.Connected) {
                logger.i { "call health check passed, marking connection as healthy" }
                call.state._connection.value = RealtimeConnection.Connected
            }
        } else {
            logger.w { "call health check failed, reconnecting. publisher $publisherState subscriber $subscriberState" }
            scope.launch { reconnect() }
        }
    }

    /**
     * Only 1 reconnect attempt runs at the same time
     * Will skip if we already tried to reconnect less than reconnectDebounceMs ms ago
     */
    suspend fun reconnect() {
        if (reconnectInProgress) return

        logger.i { "attempted to reconnect, but reconnects are disabled at the moment" }
        return

        reconnectInProgress = true
        reconnectionAttempts++

        val now = OffsetDateTime.now()

        val timeDifference = if (lastReconnectAt != null) {
            ChronoUnit.MILLIS.between(lastReconnectAt?.toInstant(), now.toInstant())
        } else {
            10000L
        }

        logger.i { "reconnect called, reconnect attempt: $reconnectionAttempts, time since last reconnect $timeDifference" }

        // ensure we don't run the reconnect too often
        if (timeDifference < reconnectDebounceMs) {
            logger.d { "reconnect skip" }
        } else {
            lastReconnectAt = now

            call.reconnectOrSwitchSfu()
        }

        reconnectInProgress = false
    }

    // monitor the network state since it's faster to detect recovered network sometimes
    internal val networkStateListener = object : NetworkStateProvider.NetworkStateListener {
        override fun onConnected() {
            logger.i { "network connected, running check to see if we should reconnect" }
            scope.launch {
                check()
            }
        }

        override fun onDisconnected() {
            val connectionState = call.state._connection.value
            logger.i { "network disconnected. connection is $connectionState marking the connection as reconnecting" }
            if (connectionState is RealtimeConnection.Joined || connectionState == RealtimeConnection.Connected) {
                call.state._connection.value = RealtimeConnection.Reconnecting
            }
        }
    }

    // monitor the peer connection since it's most likely to break
    fun monitorPeerConnection() {
        val session = call.session

        scope.launch {
            session?.let {
                // failed and closed indicate we should retry connecting to this or another SFU
                // disconnected is temporary, only if it lasts for a certain duration we should reconnect or switch
                it.subscriber?.state?.collect {
                    logger.d { "subscriber ice connection state changed to $it" }
                    check()
                }
            }
        }

        scope.launch {
            session?.let {
                // failed and closed indicate we should retry connecting to this or another SFU
                // disconnected is temporary, only if it lasts for a certain duration we should reconnect or switch
                it.publisher?.state?.collect {
                    logger.d { "publisher ice connection state changed to $it " }
                    check()
                }
            }
        }
    }

    // and for all other scenarios recheck every checkInterval ms
    fun monitorInterval() {
        scope.launch {
            while (true) {
                delay(checkInterval)
                check()
            }
        }
    }
}

/**
 * The call class gives you access to all call level API calls
 *
 * @sample
 *
 * val call = client.call("default", "123")
 * val result = call.create() // update, get etc.
 * // join the call and get audio/video
 * val result = call.join()
 *
 */
public class Call(
    internal val client: StreamVideo,
    val type: String,
    val id: String,
    val user: User,
) {
    private var statsGatheringJob: Job? = null
    internal var location: String? = null

    internal val clientImpl = client as StreamVideoImpl
    private val logger by taggedLogger("Call")

    /** The call state contains all state such as the participant list, reactions etc */
    val state = CallState(this, user)

    val sessionId by lazy { session?.sessionId }
    private val network by lazy { clientImpl.connectionModule.networkStateProvider }

    /** Camera gives you access to the local camera */
    val camera by lazy { mediaManager.camera }
    val microphone by lazy { mediaManager.microphone }
    val speaker by lazy { mediaManager.speaker }

    /** The cid is type:id */
    val cid = "$type:$id"

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(clientImpl.scope.coroutineContext + supervisorJob)

    val monitor = CallHealthMonitor(this, scope)

    /** Session handles all real time communication for video and audio */
    internal var session: RtcSession? = null
    internal val mediaManager by lazy {
        MediaManagerImpl(
            clientImpl.context,
            this,
            scope,
            clientImpl.peerConnectionFactory.eglBase.eglBaseContext
        )
    }

    /** Basic crud operations */
    suspend fun get(): Result<GetCallResponse> {
        val response = clientImpl.getCall(type, id)
        response.onSuccess {
            state.updateFromResponse(it)
        }
        return response
    }

    /** Create a call. You can create a call client side, many apps prefer to do this server side though */
    suspend fun create(
        memberIds: List<String>? = null,
        members: List<MemberRequest>? = null,
        custom: Map<String, Any>? = null,
        settings: CallSettingsRequest? = null,
        startsAt: OffsetDateTime? = null,
        team: String? = null,
        ring: Boolean = false,
        notify: Boolean = false

    ): Result<GetOrCreateCallResponse> {

        val response = if (members != null) {
            clientImpl.getOrCreateCallFullMembers(
                type = type,
                id = id,
                members = members,
                custom = custom,
                settingsOverride = settings,
                startsAt = startsAt,
                team = team,
                ring = ring,
                notify = notify
            )
        } else {
            clientImpl.getOrCreateCall(
                type = type,
                id = id,
                memberIds = memberIds,
                custom = custom,
                settingsOverride = settings,
                startsAt = startsAt,
                team = team,
                ring = ring,
                notify = notify
            )
        }

        response.onSuccess {
            state.updateFromResponse(it)
            if (ring) {
                client.state.addRingingCall(this)
            }
        }
        return response
    }

    /** Update a call */
    suspend fun update(
        custom: Map<String, Any>? = null,
        settingsOverride: CallSettingsRequest? = null,
        startsAt: OffsetDateTime? = null,
    ): Result<UpdateCallResponse> {

        val request = UpdateCallRequest(
            custom = custom,
            settingsOverride = settingsOverride,
            startsAt = startsAt
        )
        val response = clientImpl.updateCall(type, id, request)
        response.onSuccess {
            state.updateFromResponse(it)
        }
        return response
    }

    suspend fun join(
        create: Boolean = false,
        createOptions: CreateCallOptions? = null,
        ring: Boolean = false,
        notify: Boolean = false,
    ): Result<RtcSession> {
        // if we are a guest user, make sure we wait for the token before running the join flow
        clientImpl.guestUserJob?.await()
        // the join flow should retry up to 3 times
        // if the error is not permanent
        // and fail immediately on permanent errors
        state._connection.value = RealtimeConnection.InProgress
        var retryCount = 0

        var result: Result<RtcSession>

        while (retryCount < 3) {
            result = _join(create, createOptions, ring, notify)
            if (result is Success) {
                return result
            }
            if (result is Failure) {
                session = null
                logger.w { "Join failed with error $result" }
                if (isPermanentError(result.value)) {
                    state._connection.value = RealtimeConnection.Failed(result.value)
                    return result
                } else {
                    retryCount += 1
                }
            }
            delay(retryCount - 1 * 1000L)
        }
        return Failure(value = Error.GenericError("Join failed after 3 retries"))
    }

    internal fun isPermanentError(error: Any): Boolean {
        return true
    }

    internal suspend fun _join(
        create: Boolean = false,
        createOptions: CreateCallOptions? = null,
        ring: Boolean = false,
        notify: Boolean = false,
    ): Result<RtcSession> {
        if (session != null) {
            throw IllegalStateException("Call $cid has already been joined. Please use call.leave before joining it again")
        }

        // step 1. call the join endpoint to get a list of SFUs
        val timer = clientImpl.debugInfo.trackTime("call.join")

        val locationResult = clientImpl.getCachedLocation()
        if (locationResult !is Success) {
            return locationResult as Failure
        }
        location = locationResult.value
        timer.split("location found")

        val options = createOptions
            ?: if (create) {
                CreateCallOptions()
            } else {
                null
            }
        val result = joinRequest(options, locationResult.value, ring = ring, notify = notify)

        if (result !is Success) {
            return result as Failure
        }
        val sfuToken = result.value.credentials.token
        val sfuUrl = result.value.credentials.server.url
        val iceServers = result.value.credentials.iceServers.map { it.toIceServer() }
        timer.split("join request completed")

        session = RtcSession(
            client = client,
            call = this,
            sfuUrl = sfuUrl,
            sfuToken = sfuToken,
            connectionModule = (client as StreamVideoImpl).connectionModule,
            remoteIceServers = iceServers,
        )

        session?.let {
            state._connection.value = RealtimeConnection.Joined(it)
        }

        timer.split("rtc session init")

        session?.connect()

        timer.split("rtc connect completed")

        scope.launch {
            // wait for the first stream to be added
            session?.let { rtcSession ->
                val mainRtcSession = rtcSession.lastVideoStreamAdded.filter { it != null }.first()
                timer.finish("stream added, rtc completed, ready to display video $mainRtcSession")
            }
        }

        monitor.start()

        val statsGatheringInterval = 5000L

        statsGatheringJob = scope.launch {
            while (true) {
                delay(statsGatheringInterval)
                session?.publisher?.let {
                    val stats = it.getStats().value
                    state.stats.updateFromRTCStats(stats, isPublisher = true)
                }
                session?.subscriber?.let {
                    val stats = it.getStats().value
                    state.stats.updateFromRTCStats(stats, isPublisher = false)
                }
                updateLocalStats()

            }
        }

        client.state.setActiveCall(this)

        timer.finish()

        return Success(value = session!!)
    }


    fun updateLocalStats() {
        val resolution = camera?.resolution?.value
        val availableResolutions = camera?.availableResolutions?.value
        val maxResolution = availableResolutions?.maxByOrNull { it.width * it.height }

        val displayingAt = session?.trackDimensions?.value

        val sfu = session?.sfuUrl

        val sdk = "android"
        // TODO: How do we get this? val version = Configuration.versionName
        val osVersion = Build.VERSION.RELEASE ?: ""

        val vendor = Build.MANUFACTURER ?: ""
        val model = Build.MODEL ?: ""
        val deviceModel = ("$vendor $model").trim()

        val local = LocalStats(
            resolution = resolution,
            availableResolutions = availableResolutions,
            maxResolution = maxResolution,
            sfu = sfu ?: "",
            os = osVersion,
            sdkVersion = "0.1",
            deviceModel = deviceModel,
        )
        state.stats._local.value = local


    }


    suspend fun reconnectOrSwitchSfu() {
        // mark us as reconnecting
        val connectionState = state._connection.value

        if (connectionState is RealtimeConnection.Joined || connectionState == RealtimeConnection.Connected) {
            state._connection.value = RealtimeConnection.Reconnecting
        }

        // see if we are online before attempting to reconnect
        val online = network.isConnected()

        if (online) {
            // start by retrying the current connection
            session?.reconnect()

            // ask if we should switch
            location?.let {
                val joinResponse = joinRequest(location = it, currentSfu = session?.sfuUrl)
                val shouldSwitch = false

                if (shouldSwitch && joinResponse is Success) {
                    // switch to the new SFU
                    val cred = joinResponse.value.credentials
                    val iceServers = cred.iceServers.map { it.toIceServer() }
                    session?.switchSfu(cred.server.url, cred.token, iceServers)
                }
            }
        }
    }

    /** Leave the call, but don't end it for other users */
    fun leave() {
        state._connection.value = RealtimeConnection.Disconnected
        client.state.removeActiveCall()
        camera.disable()
        microphone.disable()
        cleanup()
    }

    /** ends the call for yourself as well as other users */
    suspend fun end(): Result<Unit> {
        // end the call for everyone
        val result = clientImpl.endCall(type, id)
        // cleanup
        leave()
        return result
    }

    suspend fun sendReaction(
        type: String,
        emoji: String? = null,
        custom: Map<String, Any>? = null
    ): Result<SendReactionResponse> {
        return clientImpl.sendReaction(this.type, id, type, emoji, custom)
    }

    suspend fun queryMembers(
        filter: Map<String, Any>,
        sort: List<SortField> = mutableListOf(SortField.Desc("created_at")),
        limit: Int = 100
    ): Result<QueryMembersResponse> {
        return clientImpl.queryMembers(
            type = type,
            id = id,
            filter = filter,
            sort = sort,
            limit = limit
        ).onSuccess { state.updateFromResponse(it) }
    }

    suspend fun muteAllUsers(
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false
    ): Result<MuteUsersResponse> {
        val request = MuteUsersData(
            muteAllUsers = true,
            audio = audio,
            video = video,
            screenShare = screenShare,
        )
        return clientImpl.muteUsers(type, id, request)
    }

    fun setVisibility(sessionId: String, trackType: TrackType, visible: Boolean) {
        session?.updateTrackDimensions(sessionId, trackType, visible)
    }

    // TODO: review this
    /**
     * Perhaps it would be nicer to have an interface. Any UI elements that renders video should implement it
     *
     * And call a callback for
     * - visible/hidden
     * - resolution changes
     */
    public fun initRenderer(
        videoRenderer: VideoTextureViewRenderer,
        sessionId: String,
        trackType: TrackType,
        onRendered: (View) -> Unit = {}
    ) {
        logger.d { "[initRenderer] #sfu; sessionId: $sessionId" }

        // Note this comes from peerConnectionFactory.eglBase
        videoRenderer.init(
            clientImpl.peerConnectionFactory.eglBase.eglBaseContext,
            object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() {
                    logger.d { "[initRenderer.onFirstFrameRendered] #sfu; sessionId: $sessionId" }
                    if (trackType != TrackType.TRACK_TYPE_SCREEN_SHARE) {
                        session?.updateTrackDimensions(
                            sessionId,
                            trackType,
                            true,
                            VideoDimension(
                                videoRenderer.measuredWidth,
                                videoRenderer.measuredHeight
                            )
                        )
                    }
                    onRendered(videoRenderer)
                }

                override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {
                    logger.d { "[initRenderer.onFrameResolutionChanged] #sfu; sessionId: $sessionId" }

                    if (trackType != TrackType.TRACK_TYPE_SCREEN_SHARE) {
                        session?.updateTrackDimensions(
                            sessionId,
                            trackType,
                            true,
                            VideoDimension(
                                videoRenderer.measuredWidth,
                                videoRenderer.measuredHeight
                            )
                        )
                    }
                }
            }
        )
    }

    suspend fun goLive(): Result<GoLiveResponse> {
        val result = clientImpl.goLive(type, id)
        result.onSuccess { state.updateFromResponse(it) }

        return result
    }

    suspend fun stopLive(): Result<StopLiveResponse> {
        val result = clientImpl.stopLive(type, id)
        result.onSuccess { state.updateFromResponse(it) }
        return result
    }

    suspend fun sendCustomEvent(data: Map<String, Any>): Result<SendEventResponse> {
        return clientImpl.sendCustomEvent(this.type, this.id, data)
    }

    /** Permissions */
    suspend fun requestPermissions(vararg permission: String): Result<Unit> {
        return clientImpl.requestPermissions(type, id, permission.toList())
    }

    suspend fun startRecording(): Result<Any> {
        return clientImpl.startRecording(type, id)
    }

    suspend fun stopRecording(): Result<Any> {
        return clientImpl.stopRecording(type, id)
    }

    suspend fun startBroadcasting(): Result<Any> {
        return clientImpl.startBroadcasting(type, id)
    }

    suspend fun stopBroadcasting(): Result<Any> {
        return clientImpl.stopBroadcasting(type, id)
    }

    private var subscriptions = mutableSetOf<EventSubscription>()

    public fun subscribeFor(
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

    public fun subscribe(
        listener: VideoEventListener<VideoEvent>
    ): EventSubscription {
        val sub = EventSubscription(listener)
        subscriptions.add(sub)
        return sub
    }

    public suspend fun blockUser(userId: String): Result<BlockUserResponse> {
        return clientImpl.blockUser(type, id, userId)
    }

    // TODO: add removeMember (single)

    public suspend fun removeMembers(userIds: List<String>): Result<UpdateCallMembersResponse> {
        val request = UpdateCallMembersRequest(removeMembers = userIds)
        return clientImpl.updateMembers(type, id, request)
    }

    public suspend fun grantPermissions(
        userId: String,
        permissions: List<String>
    ): Result<UpdateUserPermissionsResponse> {
        val request = UpdateUserPermissionsData(
            userId = userId,
            grantedPermissions = permissions
        )
        return clientImpl.updateUserPermissions(type, id, request)
    }

    public suspend fun revokePermissions(
        userId: String,
        permissions: List<String>
    ): Result<UpdateUserPermissionsResponse> {
        val request = UpdateUserPermissionsData(
            userId = userId,
            revokedPermissions = permissions
        )
        return clientImpl.updateUserPermissions(type, id, request)
    }

    public suspend fun updateMembers(memberRequests: List<MemberRequest>): Result<UpdateCallMembersResponse> {
        val request = UpdateCallMembersRequest(updateMembers = memberRequests)
        return clientImpl.updateMembers(type, id, request)
    }

    fun fireEvent(event: VideoEvent) {
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

    suspend fun listRecordings(): Result<ListRecordingsResponse> {
        return clientImpl.listRecordings(type, id, "what")
    }

    suspend fun muteUser(
        userId: String,
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false
    ): Result<MuteUsersResponse> {
        val request = MuteUsersData(
            users = listOf(userId),
            muteAllUsers = false,
            audio = audio,
            video = video,
            screenShare = screenShare,
        )
        return clientImpl.muteUsers(type, id, request)
    }

    suspend fun muteUsers(
        userIds: List<String>,
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false
    ): Result<MuteUsersResponse> {
        val request = MuteUsersData(
            users = userIds,
            muteAllUsers = false,
            audio = audio,
            video = video,
            screenShare = screenShare,
        )
        return clientImpl.muteUsers(type, id, request)
    }

    @VisibleForTesting
    internal suspend fun joinRequest(
        create: CreateCallOptions? = null,
        location: String,
        currentSfu: String? = null,
        ring: Boolean = false,
        notify: Boolean = false
    ): Result<JoinCallResponse> {
        val result = clientImpl.joinCall(
            type, id,
            create = create != null,
            members = create?.memberRequestsFromIds(),
            custom = create?.custom,
            settingsOverride = create?.settings,
            startsAt = create?.startsAt,
            team = create?.team,
            ring = ring,
            notify = notify,
            location = location
        )
        result.onSuccess {
            state.updateFromResponse(it)
        }
        return result
    }

    fun cleanup() {
        monitor.stop()
        session?.cleanup()
        supervisorJob.cancel()
        statsGatheringJob?.cancel()
        session = null
    }

    suspend fun ring(): Result<GetCallResponse> {
        return clientImpl.ring(type, id)
    }

    suspend fun notify(): Result<GetCallResponse> {
        return clientImpl.notify(type, id)
    }

    suspend fun accept(): Result<AcceptCallResponse> {
        return clientImpl.accept(type, id)
    }

    suspend fun reject(): Result<RejectCallResponse> {
        return clientImpl.reject(type, id)
    }
}

public data class CreateCallOptions(
    val memberIds: List<String>? = null,
    val members: List<MemberRequest>? = null,
    val custom: Map<String, Any>? = null,
    val settings: CallSettingsRequest? = null,
    val startsAt: OffsetDateTime? = null,
    val team: String? = null,
) {
    fun memberRequestsFromIds(): List<MemberRequest>? {
        return memberIds?.map { MemberRequest(userId = it) } ?: members
    }
}
