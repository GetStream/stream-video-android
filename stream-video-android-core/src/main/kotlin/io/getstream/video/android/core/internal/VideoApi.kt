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
import io.getstream.android.video.generated.models.AcceptCallResponse
import io.getstream.android.video.generated.models.BlockUserResponse
import io.getstream.android.video.generated.models.GetCallResponse
import io.getstream.android.video.generated.models.GetOrCreateCallResponse
import io.getstream.android.video.generated.models.GoLiveResponse
import io.getstream.android.video.generated.models.JoinCallResponse
import io.getstream.android.video.generated.models.ListRecordingsResponse
import io.getstream.android.video.generated.models.ListTranscriptionsResponse
import io.getstream.android.video.generated.models.MemberRequest
import io.getstream.android.video.generated.models.MuteUsersResponse
import io.getstream.android.video.generated.models.PinResponse
import io.getstream.android.video.generated.models.QueryCallMembersResponse
import io.getstream.android.video.generated.models.QueryCallsResponse
import io.getstream.android.video.generated.models.RejectCallResponse
import io.getstream.android.video.generated.models.SendCallEventResponse
import io.getstream.android.video.generated.models.SendReactionResponse
import io.getstream.android.video.generated.models.StartClosedCaptionsResponse
import io.getstream.android.video.generated.models.StartHLSBroadcastingResponse
import io.getstream.android.video.generated.models.StartTranscriptionResponse
import io.getstream.android.video.generated.models.StopClosedCaptionsResponse
import io.getstream.android.video.generated.models.StopLiveResponse
import io.getstream.android.video.generated.models.StopTranscriptionResponse
import io.getstream.android.video.generated.models.UpdateCallMembersRequest
import io.getstream.android.video.generated.models.UpdateCallMembersResponse
import io.getstream.android.video.generated.models.UpdateCallResponse
import io.getstream.android.video.generated.models.UpdateUserPermissionsResponse
import io.getstream.result.call.Call
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
import io.getstream.video.android.model.User
import org.threeten.bp.OffsetDateTime

internal interface VideoApi {

    @Deprecated(
        message = "This is an internal API that will be removed in the future. It is exposing the " +
            "raw GetCallResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("getCall(type, id, ring, notify)"),
    )
    fun oldQueryCallMembers(
        type: String,
        id: String,
        filter: Map<String, Any>?,
        sort: List<SortField>,
        prev: String?,
        next: String?,
        limit: Int,
    ): Call<QueryCallMembersResponse>

    fun queryCallMembers(
        type: String,
        id: String,
        filter: Map<String, Any>?,
        sort: List<SortField>,
        prev: String?,
        next: String?,
        limit: Int,
    ): Call<QueriedMembers>

    @Deprecated(
        message = "This is an internal API that will be removed in the future. It is exposing the " +
            "raw GetCallResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("getCall(type, id, ring, notify)"),
    )
    fun oldGetCall(
        type: String,
        id: String,
        ring: Boolean? = null,
        notify: Boolean? = null,
    ): Call<GetCallResponse>

    fun getCall(
        type: String,
        id: String,
        ring: Boolean? = null,
        notify: Boolean? = null,
    ): Call<CallData>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw UpdateCallResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("updateCall(type, id, custom, startsAt)"),
    )
    fun oldUpdateCall(
        type: String,
        id: String,
        custom: Map<String, Any>?,
        startsAt: OffsetDateTime?,
    ): Call<UpdateCallResponse>

    fun updateCall(
        type: String,
        id: String,
        custom: Map<String, Any>?,
        startsAt: OffsetDateTime?,
    ): Call<CallData>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw GetOrCreateCallResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith(
            "getOrCreateCall(type, id, members, custom, settingsOverride, startsAt, team, ring, notify)",
        ),
    )
    fun oldGetOrCreateCall(
        type: String,
        id: String,
        members: List<MemberRequest>? = null,
        custom: Map<String, Any>? = null,
        startsAt: OffsetDateTime? = null,
        team: String? = null,
        ring: Boolean,
        notify: Boolean,
    ): Call<GetOrCreateCallResponse>

    fun getOrCreateCall(
        type: String,
        id: String,
        members: List<MemberRequest>? = null,
        custom: Map<String, Any>? = null,
        startsAt: OffsetDateTime? = null,
        team: String? = null,
        ring: Boolean,
        notify: Boolean,
    ): Call<CallData>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw AcceptCallResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("acceptCall(type, id)"),
    )
    fun oldAcceptCall(
        type: String,
        id: String,
    ): Call<AcceptCallResponse>

    fun acceptCall(
        type: String,
        id: String,
    ): Call<Unit>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw BlockUserResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("blockUser(type, id, userId)"),
    )
    fun oldBlockUser(
        type: String,
        id: String,
        userId: String,
    ): Call<BlockUserResponse>

    fun blockUser(
        type: String,
        id: String,
        userId: String,
    ): Call<Unit>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw SendCallEventResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("sendCallEvent(type, id, dataJson)"),
    )
    fun oldSendCallEvent(
        type: String,
        id: String,
        dataJson: Map<String, Any>,
    ): Call<SendCallEventResponse>

    fun sendCallEvent(
        type: String,
        id: String,
        dataJson: Map<String, Any>,
    ): Call<Unit>

    fun collectUserFeedback(
        callType: String,
        id: String,
        sessionId: String,
        rating: Int,
        reason: String?,
        custom: Map<String, Any>?,
    ): Call<Unit>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw GoLiveResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("goLive(type, id, startHls, startRecording, startTranscription)"),
    )
    fun oldGoLive(
        type: String,
        id: String,
        startHls: Boolean,
        startRecording: Boolean,
        startTranscription: Boolean,
    ): Call<GoLiveResponse>

    fun goLive(
        type: String,
        id: String,
        startHls: Boolean,
        startRecording: Boolean,
        startTranscription: Boolean,
    ): Call<CallInfo>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw JoinCallResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith(
            "joinCall(type, id, create, members, custom, settingsOverride, startsAt, team, ring, notify, location, migratingFrom)",
        ),
    )
    fun oldJoinCall(
        type: String,
        id: String,
        create: Boolean = false,
        membersId: List<String>? = null,
        custom: Map<String, Any>? = null,
        startsAt: OffsetDateTime? = null,
        team: String? = null,
        ring: Boolean = false,
        notify: Boolean = false,
        location: String,
        migratingFrom: String?,
    ): Call<JoinCallResponse>

    fun joinCall(
        type: String,
        id: String,
        create: Boolean = false,
        membersId: List<String>? = null,
        custom: Map<String, Any>? = null,
        startsAt: OffsetDateTime? = null,
        team: String? = null,
        ring: Boolean = false,
        notify: Boolean = false,
        location: String,
        migratingFrom: String?,
    ): Call<CallData>

    fun endCall(
        type: String,
        id: String,
    ): Call<Unit>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw UpdateCallMembersResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("updateCallMembers(type, id, request)"),
    )
    fun oldUpdateCallMembers(
        type: String,
        id: String,
        request: UpdateCallMembersRequest,
    ): Call<UpdateCallMembersResponse>

    fun updateCallMembers(
        type: String,
        id: String,
        removeMemberIds: List<String>? = null,
        updateMembers: List<UpdateMemberData>? = null,
    ): Call<List<CallUser>>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw MuteUsersResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("muteUsers(type, id, muteUsersData)"),
    )
    fun oldMuteUsers(
        type: String,
        id: String,
        muteUsersData: MuteUsersData,
    ): Call<MuteUsersResponse>

    fun muteUsers(
        type: String,
        id: String,
        muteUsersData: MuteUsersData,
    ): Call<Unit>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw PinResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("videoPin(type, callId, sessionId, userId)"),
    )
    fun oldVideoPin(
        type: String,
        callId: String,
        sessionId: String,
        userId: String,
    ): Call<PinResponse>

    fun videoPin(
        type: String,
        callId: String,
        sessionId: String,
        userId: String,
    ): Call<Unit>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw SendReactionResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("sendVideoReaction(callType, id, type, emoji, custom)"),
    )
    fun oldSendVideoReaction(
        callType: String,
        id: String,
        type: String,
        emoji: String? = null,
        custom: Map<String, Any>? = null,
    ): Call<SendReactionResponse>

    fun sendVideoReaction(
        callType: String,
        id: String,
        type: String,
        emoji: String? = null,
        custom: Map<String, Any>? = null,
    ): Call<ReactionData>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw ListRecordingsResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("listRecordings(type, id)"),
    )
    fun oldListRecordings(
        type: String,
        id: String,
    ): Call<ListRecordingsResponse>

    fun listRecordings(
        type: String,
        id: String,
    ): Call<List<CallRecordingData>>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw RejectCallResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("rejectCall(type, id, reason)"),
    )
    fun oldRejectCall(
        type: String,
        id: String,
        reason: RejectReason? = null,
    ): Call<RejectCallResponse>

    fun rejectCall(
        type: String,
        id: String,
        reason: RejectReason? = null,
    ): Call<Unit>

    fun requestPermission(
        type: String,
        id: String,
        permissions: List<String>,
    ): Call<Unit>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw StartHLSBroadcastingResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("startHLSBroadcasting(type, id)"),
    )
    fun oldStartHLSBroadcasting(
        type: String,
        id: String,
    ): Call<StartHLSBroadcastingResponse>

    fun startHLSBroadcasting(
        type: String,
        id: String,
    ): Call<String>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw StartClosedCaptionsResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("startClosedCaptions(type, id)"),
    )
    fun oldStartClosedCaptions(
        type: String,
        id: String,
    ): Call<StartClosedCaptionsResponse>

    fun startClosedCaptions(
        type: String,
        id: String,
    ): Call<Unit>

    fun startRecording(
        type: String,
        id: String,
        externalStorage: String? = null,
    ): Call<Unit>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw StartTranscriptionResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("startTranscription(type, id, externalStorage)"),
    )
    fun oldStartTranscription(
        type: String,
        id: String,
        externalStorage: String? = null,
    ): Call<StartTranscriptionResponse>

    fun startTranscription(
        type: String,
        id: String,
        externalStorage: String? = null,
    ): Call<Unit>

    fun stopHLSBroadcasting(
        type: String,
        id: String,
    ): Call<Unit>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw StopClosedCaptionsResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("stopClosedCaptions(type, id)"),
    )
    fun oldStopClosedCaptions(
        type: String,
        id: String,
    ): Call<StopClosedCaptionsResponse>

    fun stopClosedCaptions(
        type: String,
        id: String,
    ): Call<Unit>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw StopLiveResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("stopLive(type, id)"),
    )
    fun oldStopLive(
        type: String,
        id: String,
    ): Call<StopLiveResponse>

    fun stopLive(
        type: String,
        id: String,
    ): Call<CallInfo>

    fun stopRecording(
        type: String,
        id: String,
    ): Call<Unit>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw StopTranscriptionResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("stopTranscription(type, id)"),
    )
    fun oldStopTranscription(
        type: String,
        id: String,
    ): Call<StopTranscriptionResponse>

    fun stopTranscription(
        type: String,
        id: String,
    ): Call<Unit>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw ListTranscriptionsResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("listTranscriptions(type, id)"),
    )
    fun oldListTranscriptions(
        type: String,
        id: String,
    ): Call<ListTranscriptionsResponse>

    fun listTranscriptions(
        type: String,
        id: String,
    ): Call<List<CallTranscription>>

    fun unblockUser(
        type: String,
        id: String,
        userId: String,
    ): Call<Unit>

    fun videoUnpin(
        type: String,
        callId: String,
        sessionId: String,
        userId: String,
    ): Call<Unit>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw UpdateUserPermissionsResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("updateUserPermissions(type, id, updateUserPermissionsData)"),
    )
    fun oldUpdateUserPermissions(
        type: String,
        id: String,
        updateUserPermissionsData: UpdateUserPermissionsData,
    ): Call<UpdateUserPermissionsResponse>

    fun updateUserPermissions(
        type: String,
        id: String,
        updateUserPermissionsData: UpdateUserPermissionsData,
    ): Call<Unit>

    @Deprecated(
        message = "This is an internal API that will be change in the future. It is exposing the " +
            "raw QueryCallsResponse object, which is not recommended for public use.",
        replaceWith = ReplaceWith("oldQueryCalls(filters, sort, limit, prev, next, watch)"),
    )
    fun oldQueryCalls(
        filters: Map<String, Any>,
        sort: List<SortField>,
        limit: Int,
        prev: String?,
        next: String?,
        watch: Boolean,
    ): Call<QueryCallsResponse>

    fun queryCalls(
        filters: Map<String, Any>,
        sort: List<SortField>,
        limit: Int,
        prev: String?,
        next: String?,
        watch: Boolean,
    ): Call<QueriedCalls>

    fun deleteDevice(deviceId: String): Call<Unit>

    fun createDevice(pushDevice: PushDevice): Call<Unit>

    fun getEdges(): Call<List<EdgeData>>

    fun createGuest(user: User): Call<Unit>
}
