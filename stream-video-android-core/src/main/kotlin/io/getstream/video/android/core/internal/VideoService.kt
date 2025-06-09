package io.getstream.video.android.core.internal

import io.getstream.android.push.PushDevice
import io.getstream.android.video.generated.apis.ProductvideoApi
import io.getstream.android.video.generated.models.AcceptCallResponse
import io.getstream.android.video.generated.models.BlockUserRequest
import io.getstream.android.video.generated.models.CallRequest
import io.getstream.android.video.generated.models.CallSettingsRequest
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
import io.getstream.android.video.generated.models.QueryCallMembersRequest
import io.getstream.android.video.generated.models.QueryCallsRequest
import io.getstream.android.video.generated.models.QueryCallsResponse
import io.getstream.android.video.generated.models.RejectCallRequest
import io.getstream.android.video.generated.models.RequestPermissionRequest
import io.getstream.android.video.generated.models.SendCallEventRequest
import io.getstream.android.video.generated.models.SendCallEventResponse
import io.getstream.android.video.generated.models.SendReactionRequest
import io.getstream.android.video.generated.models.StartClosedCaptionsRequest
import io.getstream.android.video.generated.models.StartRecordingRequest
import io.getstream.android.video.generated.models.StartTranscriptionRequest
import io.getstream.android.video.generated.models.StopClosedCaptionsRequest
import io.getstream.android.video.generated.models.StopClosedCaptionsResponse
import io.getstream.android.video.generated.models.StopLiveRequest
import io.getstream.android.video.generated.models.StopTranscriptionRequest
import io.getstream.android.video.generated.models.UnblockUserRequest
import io.getstream.android.video.generated.models.UnpinRequest
import io.getstream.android.video.generated.models.UpdateCallMembersRequest
import io.getstream.android.video.generated.models.UpdateCallMembersResponse
import io.getstream.android.video.generated.models.UpdateCallRequest
import io.getstream.android.video.generated.models.UpdateCallResponse
import io.getstream.android.video.generated.models.UserRequest
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.result.call.Call
import io.getstream.result.call.CoroutineCall
import io.getstream.result.call.doOnResult
import io.getstream.result.call.toUnitCall
import io.getstream.video.android.core.BuildConfig
import io.getstream.video.android.core.internal.network.AuthTypeProvider
import io.getstream.video.android.core.model.EdgeData
import io.getstream.video.android.core.model.MuteUsersData
import io.getstream.video.android.core.model.QueriedMembers
import io.getstream.video.android.core.model.RejectReason
import io.getstream.video.android.core.model.SortField
import io.getstream.video.android.core.model.UpdateUserPermissionsData
import io.getstream.video.android.core.model.toCreateDeviceRequest
import io.getstream.video.android.core.model.toRequest
import io.getstream.video.android.core.socket.common.token.CacheableTokenProvider
import io.getstream.video.android.core.socket.common.token.ConstantTokenProvider
import io.getstream.video.android.core.socket.common.token.TokenManager
import io.getstream.video.android.core.utils.toEdge
import io.getstream.video.android.core.utils.toQueriedMembers
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope
import org.threeten.bp.OffsetDateTime

internal class VideoService(
    private val scope: CoroutineScope,
    private val tokenManager: TokenManager,
    private val authTypeProvider: AuthTypeProvider,
    private val api: ProductvideoApi,
) : VideoApi {
    override fun queryCallMembers(
        type: String,
        id: String,
        filter: Map<String, Any>?,
        sort: List<SortField>,
        prev: String?,
        next: String?,
        limit: Int,
    ): Call<QueriedMembers> = createCall {
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
                .toQueriedMembers()
        }

    @ExperimentalStreamVideoApi
    override fun getCall(
        type: String,
        id: String,
        connectionId: String?,
        ring: Boolean?,
        notify: Boolean?,
    ): Call<GetCallResponse> = createCall {
        api.getCall(
            type = type,
            id = id,
            connectionId = connectionId,
            ring = ring,
            notify = notify,
        )
    }

    @ExperimentalStreamVideoApi
    override fun updateCall(
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

    @ExperimentalStreamVideoApi
    override fun getOrCreateCall(
        type: String,
        id: String,
        members: List<MemberRequest>?,
        custom: Map<String, Any>?,
        settingsOverride: CallSettingsRequest?,
        startsAt: OffsetDateTime?,
        team: String?,
        ring: Boolean,
        notify: Boolean,
        connectionId: String?,
    ): Call<GetOrCreateCallResponse> = createCall {
        api.getOrCreateCall(
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
            connectionId = connectionId,
        )
    }

    @ExperimentalStreamVideoApi
    override fun acceptCall(
        type: String,
        id: String,
    ): Call<AcceptCallResponse> = createCall {
        api.acceptCall(type, id)
    }

    override fun blockUser(
        type: String,
        id: String,
        userId: String,
    ): Call<Unit> = createCall {
        api.blockUser(
            type,
            id,
            BlockUserRequest(userId),
        )
    }

    @ExperimentalStreamVideoApi
    override fun sendCallEvent(
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

    @ExperimentalStreamVideoApi
    override fun goLive(
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

    @ExperimentalStreamVideoApi
    override fun joinCall(
        type: String,
        id: String,
        create: Boolean,
        members: List<MemberRequest>?,
        custom: Map<String, Any>?,
        settingsOverride: CallSettingsRequest?,
        startsAt: OffsetDateTime?,
        team: String?,
        ring: Boolean,
        notify: Boolean,
        location: String,
        migratingFrom: String?,
        connectionId: String?,
    ): Call<JoinCallResponse> = createCall {
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

        api.joinCall(
            type = type,
            id = id,
            joinCallRequest = joinCallRequest,
            connectionId = connectionId,
        )
    }

    override fun endCall(
        type: String,
        id: String,
    ): Call<Unit> = createCall {
        api.endCall(type, id)
    }

    @ExperimentalStreamVideoApi
    override fun updateCallMembers(
        type: String,
        id: String,
        request: UpdateCallMembersRequest,
    ): Call<UpdateCallMembersResponse> = createCall {
        api.updateCallMembers(type, id, request)
    }

    @ExperimentalStreamVideoApi
    override fun muteUsers(
        type: String,
        id: String,
        muteUsersData: MuteUsersData,
    ): Call<MuteUsersResponse> = createCall {
        val request = muteUsersData.toRequest()
        api.muteUsers(type, id, request)
    }

    override suspend fun videoPin(
        type: String,
        callId: String,
        sessionId: String,
        userId: String,
    ): Call<Unit> = createCall {
        api.videoPin(
            type,
            callId,
            PinRequest(
                sessionId,
                userId,
            ),
        )
    }

    override fun sendVideoReaction(
        callType: String,
        id: String,
        type: String,
        emoji: String?,
        custom: Map<String, Any>?,
    ): Call<Unit> = createCall {
        val request = SendReactionRequest(type, custom = custom, emojiCode = emoji)
        api.sendVideoReaction(callType, id, request)
    }

    @ExperimentalStreamVideoApi
    override fun listRecordings(
        type: String,
        id: String,
    ): Call<ListRecordingsResponse> = createCall {
        api.listRecordings(type, id)
    }

    override fun rejectCall(
        type: String,
        id: String,
        reason: RejectReason?,
    ): Call<Unit> = createCall {
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

    override fun startHLSBroadcasting(
        type: String,
        id: String
    ): Call<Unit> = createCall { api.startHLSBroadcasting(type, id) }

    override fun startClosedCaptions(
        type: String,
        id: String
    ): Call<Unit> = createCall {
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

    override fun startTranscription(
        type: String,
        id: String,
        externalStorage: String?,
    ): Call<Unit> = createCall {
        val startTranscriptionRequest =
            StartTranscriptionRequest(transcriptionExternalStorage = externalStorage)
        api.startTranscription(type, id, startTranscriptionRequest)
    }

    override fun stopHLSBroadcasting(
        type: String,
        id: String
    ): Call<Unit> = createCall {
        api.stopHLSBroadcasting(type, id)
    }

    @ExperimentalStreamVideoApi
    override fun stopClosedCaptions(
        type: String,
        id: String
    ): Call<StopClosedCaptionsResponse> = createCall {
        api.stopClosedCaptions(
            type,
            id,
            StopClosedCaptionsRequest(),
        )
    }

    override fun stopLive(
        type: String,
        id: String
    ): Call<Unit> = createCall {
        api.stopLive(type, id, StopLiveRequest())
    }

    override fun stopRecording(
        type: String,
        id: String
    ): Call<Unit> = createCall {
        api.stopRecording(type, id)
    }

    override fun stopTranscription(
        type: String,
        id: String
    ): Call<Unit> = createCall {
        api.stopTranscription(type, id, StopTranscriptionRequest())
    }

    @ExperimentalStreamVideoApi
    override fun listTranscriptions(
        type: String,
        id: String
    ): Call<ListTranscriptionsResponse> = createCall {
        api.listTranscriptions(type, id)
    }

    override fun unblockUser(
        type: String,
        id: String,
        userId: String
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
        userId: String
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

    @ExperimentalStreamVideoApi
    override fun queryCalls(
        filters: Map<String, Any>,
        sort: List<SortField>,
        limit: Int,
        prev: String?,
        next: String?,
        watch: Boolean,
        connectionId: String?,
    ): Call<QueryCallsResponse> = createCall {
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
            connectionId = connectionId,
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
                )
            )
        )
    }
        .doOnResult(scope) { result ->
        result.onSuccess {
            tokenManager.updateTokenProvider(CacheableTokenProvider(ConstantTokenProvider(it.accessToken)))
            authTypeProvider.setAuthType(AuthTypeProvider.AuthType.JWT)
        }
    }
        .toUnitCall()

    private fun <T: Any> createCall(
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