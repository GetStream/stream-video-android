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
import io.getstream.video.android.core.call.utils.DecibelThresholdDetection
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.internal.InternalStreamVideoApi
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
import org.webrtc.RendererCommon
import org.webrtc.audio.JavaAudioDeviceModule.AudioSamples
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension

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

    private var subscriptions = mutableSetOf<EventSubscription>()

    internal val clientImpl = client as StreamVideoImpl
    private val logger by taggedLogger("Call")

    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(clientImpl.scope.coroutineContext + supervisorJob)

    /** The call state contains all state such as the participant list, reactions etc */
    val state = CallState(client, this, user, scope)

    val sessionId by lazy { clientImpl.sessionId }
    private val network by lazy { clientImpl.connectionModule.networkStateProvider }

    /** Camera gives you access to the local camera */
    val camera by lazy { mediaManager.camera }
    val microphone by lazy { mediaManager.microphone }
    val speaker by lazy { mediaManager.speaker }

    /** The cid is type:id */
    val cid = "$type:$id"

    val monitor = CallHealthMonitor(this, scope)

    private val decibelThresholdDetection = DecibelThresholdDetection(thresholdCrossedCallback = {
        if (!microphone.isEnabled.value) {
            state.markSpeakingAsMuted()
        }
    })

    /** Session handles all real time communication for video and audio */
    internal var session: RtcSession? = null
    internal val mediaManager by lazy {
        if (testInstanceProvider.mediaManagerCreator != null) {
            testInstanceProvider.mediaManagerCreator!!.invoke()
        } else {
            MediaManagerImpl(
                clientImpl.context,
                this,
                scope,
                clientImpl.peerConnectionFactory.eglBase.eglBaseContext,
            )
        }
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
        notify: Boolean = false,

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
                notify = notify,
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
                notify = notify,
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
            startsAt = startsAt,
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
                logger.e { "Join failed with error $result" }
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
            throw IllegalStateException(
                "Call $cid has already been joined. Please use call.leave before joining it again",
            )
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

        session = if (testInstanceProvider.rtcSessionCreator != null) {
            testInstanceProvider.rtcSessionCreator!!.invoke()
        } else {
            RtcSession(
                client = client,
                call = this,
                sfuUrl = sfuUrl,
                sfuToken = sfuToken,
                connectionModule = (client as StreamVideoImpl).connectionModule,
                remoteIceServers = iceServers,
            )
        }

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
            // wait a bit before we capture stats
            delay(statsGatheringInterval)

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

                state.stats.updateLocalStats()
            }
        }

        client.state.setActiveCall(this)

        timer.finish()

        return Success(value = session!!)
    }

    suspend fun sendStats(data: Map<String, Any>) {
        return clientImpl.sendStats(type, id, data)
    }

    suspend fun switchSfu(forceSwitch: Boolean = false) {
        location?.let {
            val joinResponse = joinRequest(location = it, currentSfu = session?.sfuUrl)
            val shouldSwitch = false

            if ((shouldSwitch || forceSwitch) && joinResponse is Success) {
                // switch to the new SFU
                val cred = joinResponse.value.credentials
                logger.i { "Switching SFU from ${session?.sfuUrl} to ${cred.server.url}" }
                val iceServers = cred.iceServers.map { it.toIceServer() }
                session?.switchSfu(cred.server.url, cred.token, iceServers)
            }
        }
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
            // start by restarting ice connections
            session?.reconnect()

            // ask the coordinator if we should switch
            // TODO: disabled since switching SFUs isn't 100% stable yet server side
            // switchSfu()
        }
    }

    /** Leave the call, but don't end it for other users */
    fun leave() {
        state._connection.value = RealtimeConnection.Disconnected
        client.state.removeActiveCall()
        client.state.removeRingingCall()
        (client as StreamVideoImpl).onCallCleanUp(this)
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
        custom: Map<String, Any>? = null,
    ): Result<SendReactionResponse> {
        return clientImpl.sendReaction(this.type, id, type, emoji, custom)
    }

    suspend fun queryMembers(
        filter: Map<String, Any>,
        sort: List<SortField> = mutableListOf(SortField.Desc("created_at")),
        limit: Int = 100,
    ): Result<QueryMembersResponse> {
        return clientImpl.queryMembers(
            type = type,
            id = id,
            filter = filter,
            sort = sort,
            limit = limit,
        ).onSuccess { state.updateFromResponse(it) }
    }

    suspend fun muteAllUsers(
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false,
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
        onRendered: (View) -> Unit = {},
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
                                videoRenderer.measuredHeight,
                            ),
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
                                videoRenderer.measuredHeight,
                            ),
                        )
                    }
                }
            },
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

    suspend fun startHLS(): Result<Any> {
        return clientImpl.startBroadcasting(type, id)
    }

    suspend fun stopHLS(): Result<Any> {
        return clientImpl.stopBroadcasting(type, id)
    }

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
        listener: VideoEventListener<VideoEvent>,
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
        permissions: List<String>,
    ): Result<UpdateUserPermissionsResponse> {
        val request = UpdateUserPermissionsData(
            userId = userId,
            grantedPermissions = permissions,
        )
        return clientImpl.updateUserPermissions(type, id, request)
    }

    public suspend fun revokePermissions(
        userId: String,
        permissions: List<String>,
    ): Result<UpdateUserPermissionsResponse> {
        val request = UpdateUserPermissionsData(
            userId = userId,
            revokedPermissions = permissions,
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
        screenShare: Boolean = false,
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
        screenShare: Boolean = false,
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
        notify: Boolean = false,
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
            location = location,
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

    fun processAudioSample(audioSample: AudioSamples) {
        // do not unncessarily process the audio sample if we are not muted
        if (!microphone.isEnabled.value) {
            decibelThresholdDetection.processSoundInput(audioSample.data)
        }
    }

    @InternalStreamVideoApi
    public val debug = Debug(this)

    @InternalStreamVideoApi
    public class Debug(val call: Call) {

        public fun restartSubscriberIce() {
            call.session?.subscriber?.connection?.restartIce()
        }

        public fun restartPublisherIce() {
            call.session?.publisher?.connection?.restartIce()
        }

        public fun switchSfu() {
            call.scope.launch {
                call.switchSfu(true)
            }
        }
    }

    companion object {

        internal var testInstanceProvider = TestInstanceProvider()

        internal class TestInstanceProvider {
            var mediaManagerCreator: (() -> MediaManagerImpl)? = null
            var rtcSessionCreator: (() -> RtcSession)? = null
        }
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
