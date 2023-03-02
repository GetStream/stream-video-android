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

import io.getstream.video.android.core.api.ClientRPCService
import io.getstream.video.android.core.errors.VideoError
import io.getstream.video.android.core.model.CallInfo
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.model.QueriedCalls
import io.getstream.video.android.core.model.StreamCallCid
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.toCallInfo
import io.getstream.video.android.core.utils.Failure
import io.getstream.video.android.core.utils.Result
import io.getstream.video.android.core.utils.Success
import io.getstream.video.android.core.utils.toCallUser
import io.getstream.video.android.core.utils.toQueriedCalls
import org.openapitools.client.apis.DefaultApi
import org.openapitools.client.apis.EventsApi
import org.openapitools.client.apis.VideoCallsApi
import org.openapitools.client.models.BlockUserRequest
import org.openapitools.client.models.GetCallEdgeServerRequest
import org.openapitools.client.models.GetCallEdgeServerResponse
import org.openapitools.client.models.GetOrCreateCallRequest
import org.openapitools.client.models.GetOrCreateCallResponse
import org.openapitools.client.models.JoinCallResponse
import org.openapitools.client.models.MuteUsersRequest
import org.openapitools.client.models.QueryCallsRequest
import org.openapitools.client.models.QueryMembersRequest
import org.openapitools.client.models.RequestPermissionRequest
import org.openapitools.client.models.SendEventRequest
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
    private val defaultApi: DefaultApi
) : CallCoordinatorClient {

    /**
     * @see CallCoordinatorClient.createDevice
     */
    override suspend fun createDevice(
        createDeviceRequest: CreateDeviceRequest
    ): Result<CreateDeviceResponse> = try {
        Success(callCoordinatorService.createDevice(createDeviceRequest))
    } catch (error: Throwable) {
        Failure(VideoError(error.message, error))
    }

    /**
     * @see CallCoordinatorClient.deleteDevice
     */
    override suspend fun deleteDevice(deleteDeviceRequest: DeleteDeviceRequest): Result<Unit> =
        try {
            callCoordinatorService.deleteDevice(deleteDeviceRequest)
            Success(Unit)
        } catch (error: Throwable) {
            Failure(VideoError(error.message, error))
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
            Failure(VideoError(error.message, error))
        }

    /**
     * @see CallCoordinatorClient.joinCall
     */
    override suspend fun joinCall(
        id: String,
        type: String,
        connectionId: String,
        request: GetOrCreateCallRequest
    ): Result<JoinCallResponse> = try {
        val response = videoCallApi.joinCall(
            type = type,
            id = id,
            connectionId = connectionId,
            getOrCreateCallRequest = request
        )

        Success(response)
    } catch (error: Throwable) {
        Failure(VideoError(error.message, error))
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
            Failure(VideoError(error.message, error))
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
        Failure(VideoError(error.message, error))
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
        Failure(VideoError(error.message, error))
    }

    /**
     * @see CallCoordinatorClient.queryMembers
     */
    override suspend fun queryMembers(request: QueryMembersRequest): Result<List<CallUser>> = try {
        val users = videoCallApi.queryMembers(request).members

        Success(users.map { it.toCallUser() })
    } catch (error: Throwable) {
        Failure(VideoError(error.message, error))
    }

    override suspend fun blockUser(
        id: String,
        type: String,
        blockUserRequest: BlockUserRequest
    ): Result<Unit> = try {
        videoCallApi.blockUser(type, id, blockUserRequest)

        Success(Unit)
    } catch (error: Throwable) {
        Failure(VideoError(error.message, error))
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
        Failure(VideoError(error.message, error))
    }

    /**
     * @see CallCoordinatorClient.endCall
     */
    override suspend fun endCall(id: String, type: String): Result<Unit> = try {
        videoCallApi.endCall(type, id)

        Success(Unit)
    } catch (error: Throwable) {
        Failure(VideoError(error.message, error))
    }

    /**
     * @see CallCoordinatorClient.goLive
     */
    override suspend fun goLive(id: String, type: String): Result<CallInfo> = try {
        val result = videoCallApi.goLive(type, id)

        Success(result.call.toCallInfo())
    } catch (error: Throwable) {
        Failure(VideoError(error.message, error))
    }

    /**
     * @see CallCoordinatorClient.stopLive
     */
    override suspend fun stopLive(id: String, type: String): Result<CallInfo> = try {
        val result = videoCallApi.stopLive(type, id)

        Success(result.call.toCallInfo())
    } catch (error: Throwable) {
        Failure(VideoError(error.message, error))
    }

    /**
     * @see CallCoordinatorClient.muteUsers
     */
    override suspend fun muteUsers(
        id: String,
        type: String,
        muteUsersRequest: MuteUsersRequest
    ): Result<Unit> = try {
        videoCallApi.muteUsers(type, id, muteUsersRequest)

        Success(Unit)
    } catch (error: Throwable) {
        Failure(VideoError(error.message, error))
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
        Failure(VideoError(error.message, error))
    }

    /**
     * @see CallCoordinatorClient.queryCalls
     */
    override suspend fun queryCalls(queryCallsRequest: QueryCallsRequest): Result<QueriedCalls> =
        try {
            val result = defaultApi.queryCalls(queryCallsRequest)

            Success(result.toQueriedCalls())
        } catch (error: Throwable) {
            Failure(VideoError(error.message, error))
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
        Failure(VideoError(error.message, error))
    }

    /**
     * @see CallCoordinatorClient.startBroadcasting
     */
    override suspend fun startBroadcasting(id: String, type: String): Result<Unit> = try {
        defaultApi.startBroadcasting(type, id)

        Success(Unit)
    } catch (error: Throwable) {
        Failure(VideoError(error.message, error))
    }

    /**
     * @see CallCoordinatorClient.stopBroadcasting
     */
    override suspend fun stopBroadcasting(id: String, type: String): Result<Unit> = try {
        defaultApi.stopBroadcasting(type, id)

        Success(Unit)
    } catch (error: Throwable) {
        Failure(VideoError(error.message, error))
    }

    /**
     * @see CallCoordinatorClient.startRecording
     */
    override suspend fun startRecording(id: String, type: String): Result<Unit> = try {
        defaultApi.startRecording(type, id)

        Success(Unit)
    } catch (error: Throwable) {
        Failure(VideoError(error.message, error))
    }

    /**
     * @see CallCoordinatorClient.stopRecording
     */
    override suspend fun stopRecording(id: String, type: String): Result<Unit> = try {
        defaultApi.stopRecording(type, id)

        Success(Unit)
    } catch (error: Throwable) {
        Failure(VideoError(error.message, error))
    }

    override suspend fun updateUserPermissions(
        id: String,
        type: String,
        updateUserPermissionsRequest: UpdateUserPermissionsRequest
    ): Result<Unit> = try {
        defaultApi.updateUserPermissions(type, id, updateUserPermissionsRequest)

        Success(Unit)
    } catch (error: Throwable) {
        Failure(VideoError(error.message, error))
    }
}
