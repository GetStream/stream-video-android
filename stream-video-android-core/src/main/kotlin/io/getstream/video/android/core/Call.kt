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
import io.getstream.log.taggedLogger
import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.events.VideoEventListener
import io.getstream.video.android.core.model.*
import io.getstream.video.android.core.model.toIceServer
import org.openapitools.client.models.*
import io.getstream.webrtc.android.ui.VideoTextureViewRenderer
import org.openapitools.client.models.*
import org.webrtc.RendererCommon
import stream.video.sfu.models.TrackType

public data class SFUConnection(
    internal val callUrl: String,
    internal val sfuToken: SfuToken,
    internal val iceServers: List<IceServer>
)

public class Call(
    internal val client: StreamVideo,
    val type: String,
    val id: String,
    private val token: String = "",
    val user: User,
) {
    private val clientImpl = client as StreamVideoImpl
    var session: RtcSession? = null
    val cid = "$type:$id"
    val state = CallState(this, user)

    val mediaManager by lazy { MediaManagerImpl(client.context) }
    val camera by lazy { mediaManager.camera }
    val microphone by lazy { mediaManager.microphone }
    val speaker by lazy { mediaManager.speaker }

    // should be a stateflow
    private var sfuConnection: SFUConnection? = null

    suspend fun muteAllUsers(audio: Boolean = true, video: Boolean=false, screenShare: Boolean=false): Result<MuteUsersResponse> {
        val request = MuteUsersData(
            muteAllUsers=false,
            audio=audio,
            video=video,
            screenShare = screenShare,
        )
        return client.muteUsers(type, id, request)
    }

    suspend fun join(): Result<RtcSession> {

        /**
         * Alright, how to make this solid
         *
         * - There are 2 methods.
         * -- Client.JoinCall which makes the API call and gets a response
         * -- The whole join process. Which measures latency, uploads it etc
         *
         * Latency measurement needs to be changed
         *
         */

        // step 1. call the join endpoint to get a list of SFUs
        val result = client.joinCall(type, id)
        if (result !is Success) {
            return result as Failure
        }

        // step 2. measure latency
        // TODO: setup the initial call state based on this
        println(result.value.call.settings)

        val edgeUrls = result.value.edges.map { it.latencyUrl }
        // measure latency in parallel
        val measurements = clientImpl.measureLatency(edgeUrls)

        // upload our latency measurements to the server
        val selectEdgeServerResult = client.selectEdgeServer(
            type = type,
            id = id,
            request = GetCallEdgeServerRequest(
                latencyMeasurements = measurements.associate { it.latencyUrl to it.measurements }
            )
        )
        if (selectEdgeServerResult !is Success) {
            return result as Failure
        }

        val credentials = selectEdgeServerResult.value.credentials
        val url = credentials.server.url
        val iceServers =
            selectEdgeServerResult
                .value
                .credentials
                .iceServers
                .map { it.toIceServer() }

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

    suspend fun sendReaction(data: SendReactionData): Result<SendReactionResponse> {
        return client.sendReaction(type, id, data)
    }

    private val logger by taggedLogger("Call")

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
                            sessionId,
                            videoRenderer.measuredWidth,
                            videoRenderer.measuredHeight
                        )
                    }
                    onRender(videoRenderer)
                }

                override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {
                    logger.v { "[initRenderer.onFrameResolutionChanged] #sfu; sessionId: $sessionId" }

                    if (trackType != TrackType.TRACK_TYPE_SCREEN_SHARE) {
                        state.updateParticipantTrackSize(
                            sessionId,
                            videoRenderer.measuredWidth,
                            videoRenderer.measuredHeight
                        )
                    }
                }
            }
        )
    }

    suspend fun goLive(): Result<GoLiveResponse> {
        return client.goLive(type, id)
    }

    suspend fun stopLive(): Result<StopLiveResponse> {
        return client.stopLive(type, id)
    }

    fun leave() {
        TODO()
    }

    suspend fun end(): Result<Unit> {
        return client.endCall(type, id)
    }

    /** Basic crud operations */
    suspend fun get(): Result<GetCallResponse> {
        val response = clientImpl.getCall(type, id)
        response.onSuccess {
            state.updateFromResponse(it)
        }
        return response
    }

    suspend fun create(memberIds: List<String>? = null,
                       members: List<MemberRequest>? = null,
                       custom: Map<String, Any>? = null,
                       settingsOverride: CallSettingsRequest? = null,
                       startsAt: org.threeten.bp.OffsetDateTime? = null,
                       team: String? = null,
                       ring: Boolean = false): Result<GetOrCreateCallResponse> {

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
        }
        return response
    }

    suspend fun update(custom: Map<String, Any>? = null,
                       settingsOverride: CallSettingsRequest? = null,
                       startsAt: org.threeten.bp.OffsetDateTime? = null,
                       team: String? = null): Result<UpdateCallResponse> {

        val request = UpdateCallRequest(
            custom = custom,
            settingsOverride = settingsOverride,
            // TODO: fix me
//            startsAt = startsAt,
//            team = team
        )
        val response = clientImpl.updateCall(type, id, request)
        response.onSuccess {
            state.updateFromResponse(it)
        }
        return response
    }

    /** Permissions */
    suspend fun requestPermissions(vararg permission: String): Result<Unit> {
        return client.requestPermissions(type, id, permission.toList())
    }

    suspend fun startRecording(): Result<Any> {
        return client.startRecording(type, id)
    }

    suspend fun stopRecording(): Result<Any> {
        return client.stopRecording(type, id)
    }

    suspend fun startBroadcasting(): Result<Any> {
        return client.startBroadcasting(type, id)
    }

    suspend fun stopBroadcasting(): Result<Any> {
        return client.stopBroadcasting(type, id)
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

    public suspend fun grantPermissions(userId: String, permissions: List<String>): Result<UpdateUserPermissionsResponse> {
        val request = UpdateUserPermissionsData(
            userId = userId,
            grantedPermissions = permissions
        )
        return clientImpl.updateUserPermissions(type, id, request)
    }

    public suspend fun revokePermissions(userId: String, permissions: List<String>): Result<UpdateUserPermissionsResponse> {
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

    suspend fun muteUser(userId: String, audio: Boolean = true, video: Boolean=false, screenShare: Boolean=false): Result<MuteUsersResponse> {
        val request = MuteUsersData(
            users = listOf(userId),
            muteAllUsers=false,
            audio=audio,
            video=video,
            screenShare = screenShare,
        )
        return client.muteUsers(type, id, request)
    }

    suspend fun muteUsers(userIds: List<String>, audio: Boolean = true, video: Boolean=false, screenShare: Boolean=false): Result<MuteUsersResponse> {
        val request = MuteUsersData(
            users = userIds,
            muteAllUsers=false,
            audio=audio,
            video=video,
            screenShare = screenShare,
        )
        return client.muteUsers(type, id, request)
    }
}
