/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.coordinator

import io.getstream.result.Result
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.result.StreamError
import io.getstream.video.android.core.api.ClientRPCService
import io.getstream.video.android.core.model.CallInfo
import io.getstream.video.android.core.model.CallRecordingData
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.model.EdgeData
import io.getstream.video.android.core.model.QueriedCalls
import io.getstream.video.android.core.model.ReactionData
import io.getstream.video.android.core.model.StreamCallCid
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.toCallInfo
import io.getstream.video.android.core.utils.toCallUser
import io.getstream.video.android.core.utils.toEdge
import io.getstream.video.android.core.utils.toQueriedCalls
import io.getstream.video.android.core.utils.toReaction
import io.getstream.video.android.core.utils.toRecording
import org.openapitools.client.apis.DefaultApi
import org.openapitools.client.apis.EventsApi
import org.openapitools.client.apis.ModerationApi
import org.openapitools.client.apis.VideoCallsApi
import org.openapitools.client.models.BlockUserRequest
import org.openapitools.client.models.GetCallEdgeServerRequest
import org.openapitools.client.models.GetCallEdgeServerResponse
import org.openapitools.client.models.GetOrCreateCallRequest
import org.openapitools.client.models.GetOrCreateCallResponse
import org.openapitools.client.models.JoinCallRequest
import org.openapitools.client.models.JoinCallResponse
import org.openapitools.client.models.MuteUsersRequest
import org.openapitools.client.models.QueryCallsRequest
import org.openapitools.client.models.QueryMembersRequest
import org.openapitools.client.models.RequestPermissionRequest
import org.openapitools.client.models.SendEventRequest
import org.openapitools.client.models.SendReactionRequest
import org.openapitools.client.models.UnblockUserRequest
import org.openapitools.client.models.UpdateCallRequest
import org.openapitools.client.models.UpdateUserPermissionsRequest
import stream.video.coordinator.client_v1_rpc.CreateDeviceRequest
import stream.video.coordinator.client_v1_rpc.CreateDeviceResponse
import stream.video.coordinator.client_v1_rpc.DeleteDeviceRequest
import stream.video.coordinator.client_v1_rpc.MemberInput
import stream.video.coordinator.client_v1_rpc.UpsertCallMembersRequest

/**
 * An accessor that allows us to communicate with the API around video calls.
 */
internal class CallCoordinatorClientImpl(
    private val callCoordinatorService: ClientRPCService,
    private val videoCallApi: VideoCallsApi,
    private val eventsApi: EventsApi,
    private val defaultApi: DefaultApi,
    private val moderationApi: ModerationApi,
) : CallCoordinatorClient {

    /**
     * @see CallCoordinatorClient.createDevice
     */
    override suspend fun createDevice(
        createDeviceRequest: CreateDeviceRequest
    ): Result<CreateDeviceResponse> = try {
        Success(callCoordinatorService.createDevice(createDeviceRequest))
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not create a device.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.deleteDevice
     */
    override suspend fun deleteDevice(deleteDeviceRequest: DeleteDeviceRequest): Result<Unit> =
        try {
            callCoordinatorService.deleteDevice(deleteDeviceRequest)
            Success(Unit)
        } catch (error: Throwable) {
            Failure(
                StreamError.ThrowableError(
                    error.message ?: "Could not delete a device.", error
                )
            )
        }

    /**
     * @see CallCoordinatorClient.getOrCreateCall
     */
    override suspend fun getOrCreateCall(
        id: String,
        type: String,
        getOrCreateCallRequest: GetOrCreateCallRequest
    ): Result<GetOrCreateCallResponse> =
        try {
            val response = videoCallApi.getOrCreateCall(
                type = type,
                id = id,
                getOrCreateCallRequest = getOrCreateCallRequest
            )

            Success(response)
        } catch (error: Throwable) {
            Failure(
                StreamError.ThrowableError(
                    error.message ?: "Could not create a video call.", error
                )
            )
        }

    /**
     * @see CallCoordinatorClient.joinCall
     */
    override suspend fun joinCall(
        id: String,
        type: String,
        connectionId: String,
        request: JoinCallRequest
    ): Result<JoinCallResponse> = try {
        val response = videoCallApi.joinCallTypeId0(
            type = type,
            id = id,
            connectionId = connectionId,
            joinCallRequest = request
        )

        Success(response)
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not join a call.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.selectEdgeServer
     */
    override suspend fun selectEdgeServer(
        id: String,
        type: String,
        request: GetCallEdgeServerRequest
    ): Result<GetCallEdgeServerResponse> =
        try {
            val response = videoCallApi.getCallEdgeServer(
                type = type,
                id = id,
                getCallEdgeServerRequest = request
            )

            Success(response)
        } catch (error: Throwable) {
            Failure(
                StreamError.ThrowableError(
                    error.message ?: "Could not select am edge server.", error
                )
            )
        }

    /**
     * @see CallCoordinatorClient.sendUserEvent
     */
    override suspend fun sendUserEvent(
        id: String,
        type: String,
        sendEventRequest: SendEventRequest
    ): Result<Boolean> = try {
        eventsApi.sendEvent(type, id, sendEventRequest)

        Success(true)
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not send an user event.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.inviteUsers
     */
    override suspend fun inviteUsers(users: List<User>, cid: StreamCallCid): Result<Unit> = try {
        callCoordinatorService.upsertCallMembers(
            UpsertCallMembersRequest(
                call_cid = cid,
                members = users.map { user ->
                    MemberInput(
                        user_id = user.id, role = user.role
                    )
                }
            )
        )

        Success(Unit)
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not invite users.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.queryMembers
     */
    override suspend fun queryMembers(request: QueryMembersRequest): Result<List<CallUser>> = try {
        val users = videoCallApi.queryMembers(request).members

        Success(users.map { it.toCallUser() })
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not query members.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.blockUser
     */
    override suspend fun blockUser(
        id: String,
        type: String,
        blockUserRequest: BlockUserRequest
    ): Result<Unit> = try {
        moderationApi.blockUser(type, id, blockUserRequest)

        Success(Unit)
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not block a user.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.unblockUser
     */
    override suspend fun unblockUser(
        id: String,
        type: String,
        unblockUserRequest: UnblockUserRequest
    ): Result<Unit> = try {
        videoCallApi.unblockUser(type, id, unblockUserRequest)

        Success(Unit)
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not unblock a user.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.endCall
     */
    override suspend fun endCall(id: String, type: String): Result<Unit> = try {
        videoCallApi.endCall(type, id)

        Success(Unit)
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not end up a call.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.goLive
     */
    override suspend fun goLive(id: String, type: String): Result<CallInfo> = try {
        val result = videoCallApi.goLive(type, id)

        Success(result.call.toCallInfo())
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not go into a live.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.stopLive
     */
    override suspend fun stopLive(id: String, type: String): Result<CallInfo> = try {
        val result = videoCallApi.stopLive(type, id)

        Success(result.call.toCallInfo())
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not stop a live.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.muteUsers
     */
    override suspend fun muteUsers(
        id: String,
        type: String,
        muteUsersRequest: MuteUsersRequest
    ): Result<Unit> = try {
        moderationApi.muteUsers(type, id, muteUsersRequest)

        Success(Unit)
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not mute users.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.updateCall
     */
    override suspend fun updateCall(
        id: String,
        type: String,
        updateCallRequest: UpdateCallRequest
    ): Result<CallInfo> = try {
        val result = videoCallApi.updateCall(type, id, updateCallRequest)

        Success(result.call.toCallInfo())
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not update a call.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.queryCalls
     */
    override suspend fun queryCalls(queryCallsRequest: QueryCallsRequest): Result<QueriedCalls> =
        try {
            val result = defaultApi.queryCalls(queryCallsRequest)

            Success(result.toQueriedCalls())
        } catch (error: Throwable) {
            Failure(
                StreamError.ThrowableError(
                    error.message ?: "Could not query calls.", error
                )
            )
        }

    /**
     * @see CallCoordinatorClient.requestPermission
     */
    override suspend fun requestPermission(
        id: String,
        type: String,
        requestPermissionRequest: RequestPermissionRequest
    ): Result<Unit> = try {
        defaultApi.requestPermission(type, id, requestPermissionRequest)

        Success(Unit)
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not request a permission.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.startBroadcasting
     */
    override suspend fun startBroadcasting(id: String, type: String): Result<Unit> = try {
        defaultApi.startBroadcasting(type, id)

        Success(Unit)
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not start a broadcasting.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.stopBroadcasting
     */
    override suspend fun stopBroadcasting(id: String, type: String): Result<Unit> = try {
        defaultApi.stopBroadcasting(type, id)

        Success(Unit)
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not stop a broadcasting.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.startRecording
     */
    override suspend fun startRecording(id: String, type: String): Result<Unit> = try {
        defaultApi.startRecording(type, id)

        Success(Unit)
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not start a recording.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.stopRecording
     */
    override suspend fun stopRecording(id: String, type: String): Result<Unit> = try {
        defaultApi.stopRecording(type, id)

        Success(Unit)
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not stop a recording.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.updateUserPermissions
     */
    override suspend fun updateUserPermissions(
        id: String,
        type: String,
        updateUserPermissionsRequest: UpdateUserPermissionsRequest
    ): Result<Unit> = try {
        defaultApi.updateUserPermissions(type, id, updateUserPermissionsRequest)

        Success(Unit)
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not update a user permission.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.listRecordings
     */
    override suspend fun listRecordings(
        id: String,
        type: String,
        sessionId: String
    ): Result<List<CallRecordingData>> = try {
        val result = defaultApi.listRecordings(type, id, sessionId)

        Success(result.recordings.map { it.toRecording() })
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not list recordings.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.sendVideoReaction
     */
    override suspend fun sendVideoReaction(
        id: String,
        type: String,
        request: SendReactionRequest
    ): Result<ReactionData> = try {
        val result = defaultApi.sendVideoReaction(type, id, request)

        Success(result.reaction.toReaction())
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not send a video reaction.", error
            )
        )
    }

    /**
     * @see CallCoordinatorClient.getEdges
     */
    override suspend fun getEdges(): Result<List<EdgeData>> = try {
        val result = videoCallApi.getEdges()

        Success(result.edges.map { it.toEdge() })
    } catch (error: Throwable) {
        Failure(
            StreamError.ThrowableError(
                error.message ?: "Could not get edges.", error
            )
        )
    }
}
