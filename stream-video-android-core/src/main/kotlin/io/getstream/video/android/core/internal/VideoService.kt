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

package io.getstream.video.android.core.internal

import io.getstream.android.push.PushDevice
import io.getstream.android.video.generated.apis.ProductvideoApi
import io.getstream.android.video.generated.models.AcceptCallResponse
import io.getstream.android.video.generated.models.BlockUserRequest
import io.getstream.android.video.generated.models.BlockUserResponse
import io.getstream.android.video.generated.models.CallRequest
import io.getstream.android.video.generated.models.CollectUserFeedbackRequest
import io.getstream.android.video.generated.models.CreateGuestRequest
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
import io.getstream.android.video.generated.models.PinResponse
import io.getstream.android.video.generated.models.QueryCallMembersRequest
import io.getstream.android.video.generated.models.QueryCallMembersResponse
import io.getstream.android.video.generated.models.QueryCallsRequest
import io.getstream.android.video.generated.models.QueryCallsResponse
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
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.call.Call
import io.getstream.result.call.CoroutineCall
import io.getstream.result.call.doOnResult
import io.getstream.result.call.map
import io.getstream.result.call.toUnitCall
import io.getstream.video.android.core.BuildConfig
import io.getstream.video.android.core.internal.network.AuthTypeProvider
import io.getstream.video.android.core.model.CallData
import io.getstream.video.android.core.model.CallInfo
import io.getstream.video.android.core.model.CallRecordingData
import io.getstream.video.android.core.model.CallTranscription
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.model.EdgeData
import io.getstream.video.android.core.model.MuteUsersData
import io.getstream.video.android.core.model.QueriedCalls
import io.getstream.video.android.core.model.QueriedMembers
import io.getstream.video.android.core.model.ReactionData
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.model.UpdateMemberData
import io.getstream.video.android.core.model.UpdateUserPermissionsData
import io.getstream.video.android.core.model.toCallInfo
import io.getstream.video.android.core.model.toCreateDeviceRequest
import io.getstream.video.android.core.model.toRequest
import io.getstream.video.android.core.socket.common.token.CacheableTokenProvider
import io.getstream.video.android.core.socket.common.token.ConstantTokenProvider
import io.getstream.video.android.core.socket.common.token.TokenManager
import io.getstream.video.android.core.utils.toCallData
import io.getstream.video.android.core.utils.toCallInfo
import io.getstream.video.android.core.utils.toCallUser
import io.getstream.video.android.core.utils.toEdge
import io.getstream.video.android.core.utils.toQueriedCalls
import io.getstream.video.android.core.utils.toQueriedMembers
import io.getstream.video.android.core.utils.toReaction
import io.getstream.video.android.core.utils.toRecordings
import io.getstream.video.android.core.utils.toTranscriptions
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope
import org.threeten.bp.OffsetDateTime

internal class VideoService(
    private val scope: CoroutineScope,
    private val tokenManager: TokenManager,
    private val authTypeProvider: AuthTypeProvider,
    private val getConnectionId: suspend () -> String?,
    private val api: ProductvideoApi,
) : VideoApi {

    @Deprecated(
        message = "This is an internal API that will be removed in the future. It is exposing the " +
            "raw GetCallResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("getCall(type, id, ring, notify)"),
    )
    override fun oldQueryCallMembers(
        type: String,
        id: String,
        filter: Map<String, Any>?,
        sort: List<SortField>,
        prev: String?,
        next: String?,
        limit: Int,
    ): Call<QueryCallMembersResponse> =
        apiQueryCallMembers(type, id, filter, sort, prev, next, limit)

    override fun queryCallMembers(
        type: String,
        id: String,
        filter: Map<String, Any>?,
        sort: List<SortField>,
        prev: String?,
        next: String?,
        limit: Int,
    ): Call<QueriedMembers> =
        apiQueryCallMembers(type, id, filter, sort, prev, next, limit)
            .map { it.toQueriedMembers() }

    private fun apiQueryCallMembers(
        type: String,
        id: String,
        filter: Map<String, Any>?,
        sort: List<SortField>,
        prev: String?,
        next: String?,
        limit: Int,
    ): Call<QueryCallMembersResponse> = createCall {
        api.queryCallMembers(
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

    @Deprecated(
        message = "This is an internal API that will be removed in the future. It is exposing the " +
            "raw GetCallResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("getCall(type, id, ring, notify)"),
    )
    override fun oldGetCall(
        type: String,
        id: String,
        ring: Boolean?,
        notify: Boolean?,
    ): Call<GetCallResponse> = apiGetCall(
        type = type,
        id = id,
        ring = ring,
        notify = notify,
    )

    override fun getCall(
        type: String,
        id: String,
        ring: Boolean?,
        notify: Boolean?,
    ): Call<CallData> {
        return apiGetCall(
            type = type,
            id = id,
            ring = ring,
            notify = notify,
        ).map { it.toCallData() }
    }

    private fun apiGetCall(
        type: String,
        id: String,
        ring: Boolean?,
        notify: Boolean?,
    ): Call<GetCallResponse> = createCall {
        api.getCall(
            type = type,
            id = id,
            connectionId = getConnectionId(),
            ring = ring,
            notify = notify,
        )
    }

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw UpdateCallResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("updateCall(type, id, custom, startsAt)"),
    )
    override fun oldUpdateCall(
        type: String,
        id: String,
        custom: Map<String, Any>?,
        startsAt: OffsetDateTime?,
    ): Call<UpdateCallResponse> = apiUpdateCall(
        type = type,
        id = id,
        custom = custom,
        startsAt = startsAt,
    )

    override fun updateCall(
        type: String,
        id: String,
        custom: Map<String, Any>?,
        startsAt: OffsetDateTime?,
    ): Call<CallData> = apiUpdateCall(
        type = type,
        id = id,
        custom = custom,
        startsAt = startsAt,
    ).map { it.toCallData() }

    private fun apiUpdateCall(
        type: String,
        id: String,
        custom: Map<String, Any>?,
        startsAt: OffsetDateTime?,
    ): Call<UpdateCallResponse> = createCall {
        api.updateCall(
            type = type,
            id = id,
            updateCallRequest = UpdateCallRequest(
                custom = custom,
                startsAt = startsAt,
            ),
        )
    }

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw GetOrCreateCallResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith(
            "getOrCreateCall(type, id, members, custom, settingsOverride, startsAt, team, ring, notify)",
        ),
    )
    override fun oldGetOrCreateCall(
        type: String,
        id: String,
        members: List<MemberRequest>?,
        custom: Map<String, Any>?,
        startsAt: OffsetDateTime?,
        team: String?,
        ring: Boolean,
        notify: Boolean,
    ): Call<GetOrCreateCallResponse> = apiGetOrCreateCall(
        type = type,
        id = id,
        members = members,
        custom = custom,
        startsAt = startsAt,
        team = team,
        ring = ring,
        notify = notify,
    )

    override fun getOrCreateCall(
        type: String,
        id: String,
        members: List<MemberRequest>?,
        custom: Map<String, Any>?,
        startsAt: OffsetDateTime?,
        team: String?,
        ring: Boolean,
        notify: Boolean,
    ): Call<CallData> = apiGetOrCreateCall(
        type = type,
        id = id,
        members = members,
        custom = custom,
        startsAt = startsAt,
        team = team,
        ring = ring,
        notify = notify,
    ).map { it.toCallData() }

    private fun apiGetOrCreateCall(
        type: String,
        id: String,
        members: List<MemberRequest>?,
        custom: Map<String, Any>?,
        startsAt: OffsetDateTime?,
        team: String?,
        ring: Boolean,
        notify: Boolean,
    ): Call<GetOrCreateCallResponse> = createCall {
        api.getOrCreateCall(
            type = type,
            id = id,
            getOrCreateCallRequest = GetOrCreateCallRequest(
                data = CallRequest(
                    members = members,
                    custom = custom,
                    startsAt = startsAt,
                    team = team,
                ),
                ring = ring,
                notify = notify,
            ),
            connectionId = getConnectionId(),
        )
    }

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw AcceptCallResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("acceptCall(type, id)"),
    )
    override fun oldAcceptCall(
        type: String,
        id: String,
    ): Call<AcceptCallResponse> = apiAcceptCall(type, id)

    override fun acceptCall(
        type: String,
        id: String,
    ): Call<Unit> = apiAcceptCall(type, id).toUnitCall()

    private fun apiAcceptCall(
        type: String,
        id: String,
    ): Call<AcceptCallResponse> = createCall {
        api.acceptCall(type, id)
    }

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw BlockUserResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("blockUser(type, id, userId)"),
    )
    override fun oldBlockUser(
        type: String,
        id: String,
        userId: String,
    ): Call<BlockUserResponse> = apiBlockUser(type, id, userId)

    override fun blockUser(
        type: String,
        id: String,
        userId: String,
    ): Call<Unit> = apiBlockUser(type, id, userId).toUnitCall()

    private fun apiBlockUser(
        type: String,
        id: String,
        userId: String,
    ): Call<BlockUserResponse> = createCall {
        api.blockUser(
            type,
            id,
            BlockUserRequest(userId),
        )
    }

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw SendCallEventResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("sendCallEvent(type, id, dataJson)"),
    )
    override fun oldSendCallEvent(
        type: String,
        id: String,
        dataJson: Map<String, Any>,
    ): Call<SendCallEventResponse> = apiSendCallEvent(type, id, dataJson)

    override fun sendCallEvent(
        type: String,
        id: String,
        dataJson: Map<String, Any>,
    ): Call<Unit> = apiSendCallEvent(type, id, dataJson).toUnitCall()

    private fun apiSendCallEvent(
        type: String,
        id: String,
        dataJson: Map<String, Any>,
    ): Call<SendCallEventResponse> = createCall {
        api.sendCallEvent(
            type,
            id,
            SendCallEventRequest(custom = dataJson),
        )
    }

    override fun collectUserFeedback(
        callType: String,
        id: String,
        sessionId: String,
        rating: Int,
        reason: String?,
        custom: Map<String, Any>?,
    ): Call<Unit> = createCall {
        api.collectUserFeedback(
            type = callType,
            id = id,
            session = sessionId,
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

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw GoLiveResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("goLive(type, id, startHls, startRecording, startTranscription)"),
    )
    override fun oldGoLive(
        type: String,
        id: String,
        startHls: Boolean,
        startRecording: Boolean,
        startTranscription: Boolean,
    ): Call<GoLiveResponse> = apiGoLive(type, id, startHls, startRecording, startTranscription)

    override fun goLive(
        type: String,
        id: String,
        startHls: Boolean,
        startRecording: Boolean,
        startTranscription: Boolean,
    ): Call<CallInfo> = apiGoLive(type, id, startHls, startRecording, startTranscription)
        .map { it.toCallInfo() }

    private fun apiGoLive(
        type: String,
        id: String,
        startHls: Boolean,
        startRecording: Boolean,
        startTranscription: Boolean,
    ): Call<GoLiveResponse> = createCall {
        api.goLive(
            type = type,
            id = id,
            goLiveRequest = GoLiveRequest(
                startHls = startHls,
                startRecording = startRecording,
                startTranscription = startTranscription,
            ),
        )
    }

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw JoinCallResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith(
            "joinCall(type, id, create, members, custom, settingsOverride, startsAt, team, ring, notify, location, migratingFrom)",
        ),
    )
    override fun oldJoinCall(
        type: String,
        id: String,
        create: Boolean,
        membersId: List<String>?,
        custom: Map<String, Any>?,
        startsAt: OffsetDateTime?,
        team: String?,
        ring: Boolean,
        notify: Boolean,
        location: String,
        migratingFrom: String?,
    ): Call<JoinCallResponse> = apiJoinCall(
        create,
        membersId,
        custom,
        startsAt,
        team,
        ring,
        notify,
        location,
        migratingFrom,
        type,
        id,
    )

    override fun joinCall(
        type: String,
        id: String,
        create: Boolean,
        membersId: List<String>?,
        custom: Map<String, Any>?,
        startsAt: OffsetDateTime?,
        team: String?,
        ring: Boolean,
        notify: Boolean,
        location: String,
        migratingFrom: String?,
    ): Call<CallData> = apiJoinCall(
        create,
        membersId,
        custom,
        startsAt,
        team,
        ring,
        notify,
        location,
        migratingFrom,
        type,
        id,
    ).map { it.toCallData() }

    private fun apiJoinCall(
        create: Boolean,
        membersId: List<String>?,
        custom: Map<String, Any>?,
        startsAt: OffsetDateTime?,
        team: String?,
        ring: Boolean,
        notify: Boolean,
        location: String,
        migratingFrom: String?,
        type: String,
        id: String,
    ): Call<JoinCallResponse> = createCall {
        val joinCallRequest = JoinCallRequest(
            create = create,
            data = CallRequest(
                members = membersId?.map { MemberRequest(it) },
                custom = custom,
                startsAt = startsAt,
                team = team,
            ),
            ring = ring,
            notify = notify,
            location = location,
            migratingFrom = migratingFrom,
        )

        api.joinCall(
            type = type,
            id = id,
            joinCallRequest = joinCallRequest,
            connectionId = getConnectionId(),
        )
    }

    override fun endCall(
        type: String,
        id: String,
    ): Call<Unit> = createCall {
        api.endCall(type, id)
    }

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw UpdateCallMembersResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("updateCallMembers(type, id, request)"),
    )
    override fun oldUpdateCallMembers(
        type: String,
        id: String,
        request: UpdateCallMembersRequest,
    ): Call<UpdateCallMembersResponse> = apiOldUpdateCallMembers(type, id, request)

    override fun updateCallMembers(
        type: String,
        id: String,
        removeMemberIds: List<String>?,
        updateMembers: List<UpdateMemberData>?,
    ): Call<List<CallUser>> = apiOldUpdateCallMembers(
        type = type,
        id = id,
        request = UpdateCallMembersRequest(
            removeMembers = removeMemberIds,
            updateMembers = updateMembers?.map {
                MemberRequest(
                    userId = it.userId,
                    custom = it.custom,
                )
            },
        ),
    ).map { response ->
        response.members.map { it.toCallUser() }
    }

    private fun apiOldUpdateCallMembers(
        type: String,
        id: String,
        request: UpdateCallMembersRequest,
    ) = createCall {
        api.updateCallMembers(type, id, request)
    }

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw MuteUsersResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("muteUsers(type, id, muteUsersData)"),
    )
    override fun oldMuteUsers(
        type: String,
        id: String,
        muteUsersData: MuteUsersData,
    ): Call<MuteUsersResponse> = apiMuteUsers(muteUsersData, type, id)

    override fun muteUsers(
        type: String,
        id: String,
        muteUsersData: MuteUsersData,
    ): Call<Unit> = apiMuteUsers(muteUsersData, type, id).toUnitCall()

    private fun apiMuteUsers(
        muteUsersData: MuteUsersData,
        type: String,
        id: String,
    ): Call<MuteUsersResponse> = createCall {
        val request = muteUsersData.toRequest()
        api.muteUsers(type, id, request)
    }

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw PinResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("videoPin(type, callId, sessionId, userId)"),
    )
    override fun oldVideoPin(
        type: String,
        callId: String,
        sessionId: String,
        userId: String,
    ): Call<PinResponse> = apiVideoPin(type, callId, sessionId, userId)

    override fun videoPin(
        type: String,
        callId: String,
        sessionId: String,
        userId: String,
    ): Call<Unit> = apiVideoPin(type, callId, sessionId, userId).toUnitCall()

    private fun apiVideoPin(
        type: String,
        callId: String,
        sessionId: String,
        userId: String,
    ) = createCall {
        api.videoPin(
            type,
            callId,
            PinRequest(
                sessionId,
                userId,
            ),
        )
    }

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw SendReactionResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("sendVideoReaction(callType, id, type, emoji, custom)"),
    )
    override fun oldSendVideoReaction(
        callType: String,
        id: String,
        type: String,
        emoji: String?,
        custom: Map<String, Any>?,
    ): Call<SendReactionResponse> = apiSendVideoReaction(type, custom, emoji, callType, id)

    override fun sendVideoReaction(
        callType: String,
        id: String,
        type: String,
        emoji: String?,
        custom: Map<String, Any>?,
    ): Call<ReactionData> = apiSendVideoReaction(type, custom, emoji, callType, id)
        .map { it.toReaction() }

    private fun apiSendVideoReaction(
        type: String,
        custom: Map<String, Any>?,
        emoji: String?,
        callType: String,
        id: String,
    ): Call<SendReactionResponse> = createCall {
        val request = SendReactionRequest(type, custom = custom, emojiCode = emoji)
        api.sendVideoReaction(callType, id, request)
    }

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw ListRecordingsResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("listRecordings(type, id)"),
    )
    override fun oldListRecordings(
        type: String,
        id: String,
    ): Call<ListRecordingsResponse> = apiOldListRecordings(type, id)

    override fun listRecordings(
        type: String,
        id: String,
    ): Call<List<CallRecordingData>> = apiOldListRecordings(type, id)
        .map { it.toRecordings() }

    private fun apiOldListRecordings(
        type: String,
        id: String,
    ) = createCall {
        api.listRecordings(type, id)
    }

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw RejectCallResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("rejectCall(type, id, reason)"),
    )
    override fun oldRejectCall(
        type: String,
        id: String,
        reason: RejectReason?,
    ): Call<RejectCallResponse> = apiRejectCall(type, id, reason)

    override fun rejectCall(
        type: String,
        id: String,
        reason: RejectReason?,
    ): Call<Unit> = apiRejectCall(type, id, reason).toUnitCall()

    private fun apiRejectCall(
        type: String,
        id: String,
        reason: RejectReason?,
    ): Call<RejectCallResponse> = createCall {
        api.rejectCall(type, id, RejectCallRequest(reason?.alias))
    }

    override fun requestPermission(
        type: String,
        id: String,
        permissions: List<String>,
    ): Call<Unit> = createCall {
        api.requestPermission(
            type,
            id,
            RequestPermissionRequest(permissions),
        )
    }

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw StartHLSBroadcastingResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("startHLSBroadcasting(type, id)"),
    )
    override fun oldStartHLSBroadcasting(
        type: String,
        id: String,
    ): Call<StartHLSBroadcastingResponse> = apiStartHLSBroadcasting(type, id)

    override fun startHLSBroadcasting(
        type: String,
        id: String,
    ): Call<String> = apiStartHLSBroadcasting(type, id)
        .map { it.playlistUrl }

    private fun apiStartHLSBroadcasting(
        type: String,
        id: String,
    ): Call<StartHLSBroadcastingResponse> = createCall { api.startHLSBroadcasting(type, id) }

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw StartClosedCaptionsResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("startClosedCaptions(type, id)"),
    )
    override fun oldStartClosedCaptions(
        type: String,
        id: String,
    ): Call<StartClosedCaptionsResponse> = apiStartClosedCaptions(type, id)

    override fun startClosedCaptions(
        type: String,
        id: String,
    ): Call<Unit> = apiStartClosedCaptions(type, id).toUnitCall()

    private fun apiStartClosedCaptions(
        type: String,
        id: String,
    ): Call<StartClosedCaptionsResponse> = createCall {
        api.startClosedCaptions(
            type,
            id,
            StartClosedCaptionsRequest(),
        )
    }

    override fun startRecording(
        type: String,
        id: String,
        externalStorage: String?,
    ): Call<Unit> = createCall {
        val req = StartRecordingRequest(externalStorage)
        api.startRecording(type, id, req)
    }

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw StartTranscriptionResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("startTranscription(type, id, externalStorage)"),
    )
    override fun oldStartTranscription(
        type: String,
        id: String,
        externalStorage: String?,
    ): Call<StartTranscriptionResponse> = apiStartTranscription(externalStorage, type, id)

    override fun startTranscription(
        type: String,
        id: String,
        externalStorage: String?,
    ): Call<Unit> = apiStartTranscription(externalStorage, type, id).toUnitCall()

    private fun apiStartTranscription(
        externalStorage: String?,
        type: String,
        id: String,
    ): Call<StartTranscriptionResponse> = createCall {
        val startTranscriptionRequest =
            StartTranscriptionRequest(transcriptionExternalStorage = externalStorage)
        api.startTranscription(type, id, startTranscriptionRequest)
    }

    override fun stopHLSBroadcasting(
        type: String,
        id: String,
    ): Call<Unit> = createCall {
        api.stopHLSBroadcasting(type, id)
    }

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw StopClosedCaptionsResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("stopClosedCaptions(type, id)"),
    )
    override fun oldStopClosedCaptions(
        type: String,
        id: String,
    ): Call<StopClosedCaptionsResponse> = apiStopClosedCaptions(type, id)

    override fun stopClosedCaptions(
        type: String,
        id: String,
    ): Call<Unit> = apiStopClosedCaptions(type, id)
        .toUnitCall()

    private fun apiStopClosedCaptions(
        type: String,
        id: String,
    ) = createCall {
        api.stopClosedCaptions(
            type,
            id,
            StopClosedCaptionsRequest(),
        )
    }

    override fun oldStopLive(
        type: String,
        id: String,
    ): Call<StopLiveResponse> = apiStopLive(type, id)

    override fun stopLive(
        type: String,
        id: String,
    ): Call<CallInfo> = apiStopLive(type, id).map { it.call.toCallInfo() }

    private fun apiStopLive(
        type: String,
        id: String,
    ): Call<StopLiveResponse> = createCall {
        api.stopLive(type, id, StopLiveRequest())
    }

    override fun stopRecording(
        type: String,
        id: String,
    ): Call<Unit> = createCall {
        api.stopRecording(type, id)
    }

    override fun oldStopTranscription(type: String, id: String): Call<StopTranscriptionResponse> {
        TODO("Not yet implemented")
    }

    override fun stopTranscription(
        type: String,
        id: String,
    ): Call<Unit> = createCall {
        api.stopTranscription(type, id, StopTranscriptionRequest())
    }

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw ListTranscriptionsResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("listTranscriptions(type, id)"),
    )
    override fun oldListTranscriptions(
        type: String,
        id: String,
    ): Call<ListTranscriptionsResponse> = apiListTranscriptions(type, id)

    override fun listTranscriptions(
        type: String,
        id: String,
    ): Call<List<CallTranscription>> = apiListTranscriptions(type, id).map { it.toTranscriptions() }

    private fun apiListTranscriptions(
        type: String,
        id: String,
    ) = createCall {
        api.listTranscriptions(type, id)
    }

    override fun unblockUser(
        type: String,
        id: String,
        userId: String,
    ): Call<Unit> = createCall {
        api.unblockUser(
            type,
            id,
            UnblockUserRequest(userId),
        )
    }

    override fun videoUnpin(
        type: String,
        callId: String,
        sessionId: String,
        userId: String,
    ): Call<Unit> = createCall {
        api.videoUnpin(
            type,
            callId,
            UnpinRequest(
                sessionId,
                userId,
            ),
        )
    }

    override fun oldUpdateUserPermissions(
        type: String,
        id: String,
        updateUserPermissionsData: UpdateUserPermissionsData,
    ): Call<UpdateUserPermissionsResponse> {
        TODO("Not yet implemented")
    }

    override fun updateUserPermissions(
        type: String,
        id: String,
        updateUserPermissionsData: UpdateUserPermissionsData,
    ): Call<Unit> = createCall {
        api.updateUserPermissions(
            type,
            id,
            updateUserPermissionsData.toRequest(),
        )
    }

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw QueryCallsResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("oldQueryCalls(filters, sort, limit, prev, next, watch)"),
    )
    override fun oldQueryCalls(
        filters: Map<String, Any>,
        sort: List<SortField>,
        limit: Int,
        prev: String?,
        next: String?,
        watch: Boolean,
    ): Call<QueryCallsResponse> = apiQueryCalls(filters, sort, limit, prev, next, watch)

    override fun queryCalls(
        filters: Map<String, Any>,
        sort: List<SortField>,
        limit: Int,
        prev: String?,
        next: String?,
        watch: Boolean,
    ): Call<QueriedCalls> = apiQueryCalls(filters, sort, limit, prev, next, watch)
        .map { it.toQueriedCalls() }

    private fun apiQueryCalls(
        filters: Map<String, Any>,
        sort: List<SortField>,
        limit: Int,
        prev: String?,
        next: String?,
        watch: Boolean,
    ) = createCall {
        val request = QueryCallsRequest(
            filterConditions = filters,
            sort = sort.map { it.toRequest() },
            limit = limit,
            prev = prev,
            next = next,
            watch = watch,
        )
        api.queryCalls(
            queryCallsRequest = request,
            connectionId = getConnectionId(),
        )
    }

    override fun deleteDevice(deviceId: String): Call<Unit> = createCall {
        api.deleteDevice(deviceId)
    }

    override fun createDevice(pushDevice: PushDevice): Call<Unit> = createCall {
        pushDevice.toCreateDeviceRequest()
            .mapSuspend {
                api.createDevice(it)
            }
    }

    override fun getEdges(): Call<List<EdgeData>> = createCall {
        api.getEdges().edges.map { it.toEdge() }
    }

    override fun createGuest(user: User): Call<Unit> = createCall {
        api.createGuest(
            CreateGuestRequest(
                UserRequest(
                    id = user.id,
                    image = user.image,
                    name = user.name,
                    custom = user.custom,
                ),
            ),
        )
    }
        .doOnResult(scope) { result ->
            result.onSuccess {
                tokenManager.updateTokenProvider(
                    CacheableTokenProvider(ConstantTokenProvider(it.accessToken)),
                )
                authTypeProvider.setAuthType(AuthTypeProvider.AuthType.JWT)
            }
        }
        .toUnitCall()

    private fun <T : Any> createCall(
        block: suspend () -> T,
    ): Call<T> {
        return CoroutineCall(scope) {
            try {
                Result.Success(block())
            } catch (e: Exception) {
                Result.Failure(Error.ThrowableError("Call failed", e))
            }
        }
    }
}
