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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import org.openapitools.client.models.SendEventResponse
import org.openapitools.client.models.SendReactionResponse
import org.openapitools.client.models.StopLiveResponse
import org.openapitools.client.models.UpdateCallMembersRequest
import org.openapitools.client.models.UpdateCallMembersResponse
import org.openapitools.client.models.UpdateCallRequest
import org.openapitools.client.models.UpdateCallResponse
import org.openapitools.client.models.UpdateUserPermissionsResponse
import org.openapitools.client.models.VideoEvent
import org.webrtc.PeerConnection
import org.webrtc.RendererCommon
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension

/**
 * Monitors
 * - Peer connection states
 * - Network up/down
 *
 * And calls call.reconnectOrSwitchSfu() when needed
 */
public class CallHealthMonitor(val call: Call, val callScope: CoroutineScope) {
    private val logger by taggedLogger("Call:HealthMonitor")

    private val network by lazy { call.clientImpl.connectionModule.networkStateProvider }

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(callScope.coroutineContext + supervisorJob)

    var reconnectInProgress: Boolean = false
    var reconnectionAttempts = 0

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

    private suspend fun reconnect() {
        if (reconnectInProgress) return
        reconnectInProgress = true
        reconnectionAttempts++

        // don't hammer the server
        if (reconnectionAttempts > 1) delay(400L)

        val subscriberState = call.session?.subscriber?.state?.value
        val publisherState = call.session?.publisher?.state?.value
        val healthyPeerConnections = subscriberState in goodStates && publisherState in goodStates

        logger.i { "reconnect attempt $reconnectionAttempts, peers are healthy: $healthyPeerConnections publisher $publisherState subscriber $subscriberState" }

        if (healthyPeerConnections) {
            // don't reconnect if things are healthy
            reconnectionAttempts = 0
            call.state._connection.value = RtcConnectionState.Connected
        } else {
            call.reconnectOrSwitchSfu()
        }
        reconnectInProgress = false
    }

    internal val networkStateListener = object : NetworkStateProvider.NetworkStateListener {
        override fun onConnected() {
            logger.i { "network connected, attempting to reconnect" }
            scope.launch {
                reconnect()
            }
        }

        override fun onDisconnected() {
            val connectionState = call.state._connection.value
            logger.i { "network disconnected. connection is $connectionState marking the connection as reconnecting" }
            if (connectionState is RtcConnectionState.Joined || connectionState == RtcConnectionState.Connected) {
                call.state._connection.value = RtcConnectionState.Reconnecting
            }
        }
    }

    fun unhealthyPeer() {
        scope.launch {
            reconnect()
        }
    }

    fun healthyPeer() {
        call.state._connection.value = RtcConnectionState.Connected
        reconnectionAttempts = 0
    }

    fun monitorPeerConnection() {
        val session = call.session
        scope.launch {
            session?.let {
                // failed and closed indicate we should retry connecting to this or another SFU
                // disconnected is temporary, only if it lasts for a certain duration we should reconnect or switch
                it.subscriber?.state?.collect {
                    logger.w { "subscriber ice connection state changed to $it" }
                    if (it in badStates) {
                        unhealthyPeer()
                    } else if (it in goodStates) {
                        healthyPeer()
                    }
                }
            }
        }

        scope.launch {
            session?.let {
                // failed and closed indicate we should retry connecting to this or another SFU
                // disconnected is temporary, only if it lasts for a certain duration we should reconnect or switch
                it.publisher?.state?.collect {
                    logger.w { "publisher ice connection state changed to $it" }
                    if (it in badStates) {
                        unhealthyPeer()
                    } else if (it in goodStates) {
                        healthyPeer()
                    }
                }
            }
        }
    }

    suspend fun monitorInterval() {
        while (true) {
            delay(2000L)
            val subscriberState = call.session?.subscriber?.state?.value
            // see if we need to reconnect
            if (subscriberState in badStates) {
                logger.i { "ice connection state is $subscriberState, attempting to reconnect" }
                reconnect()
            }
            // the check every 2 seconds handles scenarios where the connect goes away and doesn't reconnect
        }
    }

    fun start() {
        network.subscribe(networkStateListener)
        monitorPeerConnection()

        scope.launch { monitorInterval() }
    }

    fun stop() {
        supervisorJob.cancel()
        network.unsubscribe(networkStateListener)
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
    private lateinit var location: String
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
        startsAt: org.threeten.bp.OffsetDateTime? = null,
        team: String? = null,
        ring: Boolean = false
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
                ring = ring
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
                ring = ring
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
        startsAt: org.threeten.bp.OffsetDateTime? = null,
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
        createOptions: CreateCallOptions? = null
    ): Result<RtcSession> {
        // if we are a guest user, make sure we wait for the token before running the join flow
        clientImpl.guestUserJob?.await()
        // the join flow should retry up to 3 times
        // if the error is not permanent
        // and fail immediately on permanent errors
        state._connection.value = RtcConnectionState.InProgress
        var retryCount = 0

        var result: Result<RtcSession>

        while (retryCount < 3) {
            result = _join(create, createOptions)
            if (result is Success) {
                return result
            }
            if (result is Failure) {
                logger.w { "Join failed with error $result" }
                if (isPermanentError(result.value)) {
                    state._connection.value = RtcConnectionState.Failed(result.value)
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
    ): Result<RtcSession> {

        // step 1. call the join endpoint to get a list of SFUs
        val timer = clientImpl.debugInfo.trackTime("call.join")

        val locationResult = clientImpl.selectLocation()
        if (locationResult !is Success) {
            return locationResult as Failure
        }
        timer.split("location found")

        val options = createOptions
            ?: if (create) {
                CreateCallOptions()
            } else {
                null
            }
        location = locationResult.value
        val result = joinRequest(options, location)

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
            state._connection.value = RtcConnectionState.Joined(it)
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

        client.state.setActiveCall(this)

        timer.finish()

        return Success(value = session!!)
    }

    suspend fun reconnectOrSwitchSfu() {
        // mark us as reconnecting
        val connectionState = state._connection.value

        if (connectionState is RtcConnectionState.Joined || connectionState == RtcConnectionState.Connected) {
            state._connection.value = RtcConnectionState.Reconnecting
        }

        // see if we are online before attempting to reconnect
        val online = network.isConnected()

        if (online) {
            // start by retrying the current connection
            session?.reconnect()

            // ask if we should switch
            val joinResponse = joinRequest(location = location, currentSfu = session?.sfuUrl)
            val shouldSwitch = true

            if (shouldSwitch && joinResponse is Success) {
                // switch to the new SFU
                val cred = joinResponse.value.credentials
                val iceServers = cred.iceServers.map { it.toIceServer() }
                session?.switchSfu(cred.server.url, cred.token, iceServers)
            }
        }
    }

    /** Leave the call, but don't end it for other users */
    fun leave() {
        state._connection.value = RtcConnectionState.Disconnected
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
        onRender: (View) -> Unit = {}
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
                            VideoDimension(videoRenderer.measuredWidth, videoRenderer.measuredHeight)
                        )
                    }
                    onRender(videoRenderer)
                }

                override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {
                    logger.d { "[initRenderer.onFrameResolutionChanged] #sfu; sessionId: $sessionId" }

                    if (trackType != TrackType.TRACK_TYPE_SCREEN_SHARE) {
                        session?.updateTrackDimensions(
                            sessionId,
                            trackType,
                            true,
                            VideoDimension(videoRenderer.measuredWidth, videoRenderer.measuredHeight)
                        )
                    }
                }
            }
        )
    }

    suspend fun goLive(): Result<GoLiveResponse> {
        return clientImpl.goLive(type, id)
    }

    suspend fun stopLive(): Result<StopLiveResponse> {
        return clientImpl.stopLive(type, id)
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
    internal suspend fun joinRequest(create: CreateCallOptions? = null, location: String, currentSfu: String? = null): Result<JoinCallResponse> {
        val result = clientImpl.joinCall(
            type, id,
            create = create != null,
            members = create?.memberRequestsFromIds(),
            custom = create?.custom,
            settingsOverride = create?.settings,
            startsAt = create?.startsAt,
            team = create?.team,
            ring = create?.ring ?: false,
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
    }
}

public data class CreateCallOptions(
    val memberIds: List<String>? = null,
    val members: List<MemberRequest>? = null,
    val custom: Map<String, Any>? = null,
    val settings: CallSettingsRequest? = null,
    val startsAt: org.threeten.bp.OffsetDateTime? = null,
    val team: String? = null,
    val ring: Boolean = false
) {
    fun memberRequestsFromIds(): List<MemberRequest>? {
        return memberIds?.map { MemberRequest(userId = it) } ?: members
    }
}
