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
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.model.IceServer
import io.getstream.video.android.core.model.MuteUsersData
import io.getstream.video.android.core.model.SendReactionData
import io.getstream.video.android.core.model.SfuToken
import io.getstream.video.android.core.model.UpdateUserPermissionsData
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.toIceServer
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import org.openapitools.client.models.BlockUserResponse
import org.openapitools.client.models.CallSettingsRequest
import org.openapitools.client.models.GetCallEdgeServerRequest
import org.openapitools.client.models.GetCallResponse
import org.openapitools.client.models.GetOrCreateCallResponse
import org.openapitools.client.models.GoLiveResponse
import org.openapitools.client.models.JoinCallResponse
import org.openapitools.client.models.ListRecordingsResponse
import org.openapitools.client.models.MemberRequest
import org.openapitools.client.models.MuteUsersResponse
import org.openapitools.client.models.SendReactionResponse
import org.openapitools.client.models.StopLiveResponse
import org.openapitools.client.models.UpdateCallMembersRequest
import org.openapitools.client.models.UpdateCallMembersResponse
import org.openapitools.client.models.UpdateCallRequest
import org.openapitools.client.models.UpdateCallResponse
import org.openapitools.client.models.UpdateUserPermissionsResponse
import org.openapitools.client.models.VideoEvent
import org.webrtc.RendererCommon
import stream.video.sfu.models.TrackType


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
    private val logger by taggedLogger("Call")
    /** The call state contains all state such as the participant list, reactions etc */
    val state = CallState(this, user)

    /** Camera gives you access to the local camera */
    val camera by lazy { mediaManager.camera }
    val microphone by lazy { mediaManager.microphone }
    val speaker by lazy { mediaManager.speaker }

    /** The cid is type:id */
    val cid = "$type:$id"

    /** Session handles all real time communication for video and audio */
    internal var session: RtcSession? = null
    private val clientImpl = client as StreamVideoImpl
    internal val mediaManager by lazy { MediaManagerImpl(clientImpl.context) }

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
        settingsOverride: CallSettingsRequest? = null,
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
                settingsOverride = settingsOverride,
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
                settingsOverride = settingsOverride,
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
            // TODO: fix me
//            startsAt = startsAt,
        )
        val response = clientImpl.updateCall(type, id, request)
        response.onSuccess {
            state.updateFromResponse(it)
        }
        return response
    }

    suspend fun join(create: CreateCallOptions? = null): Result<RtcSession> {
        // step 1. call the join endpoint to get a list of SFUs
        val result = joinRequest(create)

        if (result !is Success) {
            return result as Failure
        }

        // step 2. measure latency
        val edgeUrls = result.value.edges.map { it.latencyUrl }
        // measure latency in parallel
        val measurements = clientImpl.measureLatency(edgeUrls)

        // upload our latency measurements to the server
        val selectEdgeServerResult = clientImpl.selectEdgeServer(
            type = type,
            id = id,
            request = GetCallEdgeServerRequest(latencyMeasurements = measurements.associate { it.latencyUrl to it.measurements })
        )
        if (selectEdgeServerResult !is Success) {
            return result as Failure
        }

        val credentials = selectEdgeServerResult.value.credentials
        val url = credentials.server.url
        val iceServers =
            selectEdgeServerResult.value.credentials.iceServers.map { it.toIceServer() }

        session = RtcSession(
            client = client,
            call = this,
            sfuUrl = url,
            sfuToken = credentials.token,
            connectionModule = (client as StreamVideoImpl).connectionModule,
            remoteIceServers = iceServers,
            latencyResults = measurements.associate { it.latencyUrl to it.measurements }
        )

        session?.connect()

        return Success(value = session!!)
    }

    /** Leave the call, but don't end it for other users */
    fun leave() {
        // TODO
    }

    /** ends the call for yourself as well as other users */
    suspend fun end(): Result<Unit> {
        return clientImpl.endCall(type, id)
    }

    suspend fun sendReaction(data: SendReactionData): Result<SendReactionResponse> {
        return clientImpl.sendReaction(type, id, data)
    }


    suspend fun muteAllUsers(
        audio: Boolean = true,
        video: Boolean = false,
        screenShare: Boolean = false
    ): Result<MuteUsersResponse> {
        val request = MuteUsersData(
            muteAllUsers = false,
            audio = audio,
            video = video,
            screenShare = screenShare,
        )
        return clientImpl.muteUsers(type, id, request)
    }

    // TODO: review this
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
                    logger.v { "[initRenderer.onFirstFrameRendered] #sfu; sessionId: $sessionId" }
                    if (trackType != TrackType.TRACK_TYPE_SCREEN_SHARE) {
                        state.updateParticipantTrackSize(
                            sessionId, videoRenderer.measuredWidth, videoRenderer.measuredHeight
                        )
                    }
                    onRender(videoRenderer)
                }

                override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {
                    logger.v { "[initRenderer.onFrameResolutionChanged] #sfu; sessionId: $sessionId" }

                    if (trackType != TrackType.TRACK_TYPE_SCREEN_SHARE) {
                        state.updateParticipantTrackSize(
                            sessionId, videoRenderer.measuredWidth, videoRenderer.measuredHeight
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
    internal suspend fun joinRequest(create: CreateCallOptions? = null): Result<JoinCallResponse> {
        val result = clientImpl.joinCall(
            type, id,
            create = create != null,
            members = create?.memberRequestsFromIds(),
            custom = create?.custom,
            settingsOverride = create?.settingsOverride,
            startsAt = create?.startsAt,
            team = create?.team,
            ring = create?.ring ?: false,
        )
        result.onSuccess {
            state.updateFromResponse(it)
        }
        return result
    }
}

public data class CreateCallOptions(
    val memberIds: List<String>? = null,
    val members: List<MemberRequest>? = null,
    val custom: Map<String, Any>? = null,
    val settingsOverride: CallSettingsRequest? = null,
    val startsAt: org.threeten.bp.OffsetDateTime? = null,
    val team: String? = null,
    val ring: Boolean = false
) {
    fun memberRequestsFromIds(): List<MemberRequest>? {
        return memberIds?.map { MemberRequest(userId = it) } ?: members
    }
}