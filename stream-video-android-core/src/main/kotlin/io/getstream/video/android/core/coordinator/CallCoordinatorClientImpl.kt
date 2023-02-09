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
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.model.StreamCallCid
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.toCallUser
import io.getstream.video.android.core.utils.Failure
import io.getstream.video.android.core.utils.Result
import io.getstream.video.android.core.utils.Success
import org.openapitools.client.apis.VideoCallsApi
import org.openapitools.client.models.GetCallEdgeServerRequest
import org.openapitools.client.models.GetCallEdgeServerResponse
import org.openapitools.client.models.GetOrCreateCallRequest
import org.openapitools.client.models.GetOrCreateCallResponse
import org.openapitools.client.models.JoinCallResponse
import stream.video.coordinator.call_v1.Call
import stream.video.coordinator.client_v1_rpc.CreateDeviceRequest
import stream.video.coordinator.client_v1_rpc.CreateDeviceResponse
import stream.video.coordinator.client_v1_rpc.DeleteDeviceRequest
import stream.video.coordinator.client_v1_rpc.MemberInput
import stream.video.coordinator.client_v1_rpc.QueryUsersRequest
import stream.video.coordinator.client_v1_rpc.SendCustomEventRequest
import stream.video.coordinator.client_v1_rpc.SendEventRequest
import stream.video.coordinator.client_v1_rpc.UpsertCallMembersRequest

/**
 * An accessor that allows us to communicate with the API around video calls.
 */
internal class CallCoordinatorClientImpl(
    private val callCoordinatorService: ClientRPCService,
    private val videoCallApi: VideoCallsApi
) : CallCoordinatorClient {

    /**
     * Create a new Device used to receive Push Notifications.
     *
     * @param createDeviceRequest The device data.
     * @return [CreateDeviceResponse] witch holds the device.
     */
    override suspend fun createDevice(
        createDeviceRequest: CreateDeviceRequest
    ): Result<CreateDeviceResponse> = try {
        Success(callCoordinatorService.createDevice(createDeviceRequest))
    } catch (error: Throwable) {
        Failure(VideoError(error.message, error))
    }

    /**
     * Delete a Device used to receive Push Notifications.
     *
     * @param deleteDeviceRequest The device data.
     * @return Result if the operation was successful or not.
     */
    override suspend fun deleteDevice(deleteDeviceRequest: DeleteDeviceRequest): Result<Unit> =
        try {
            callCoordinatorService.deleteDevice(deleteDeviceRequest)
            Success(Unit)
        } catch (error: Throwable) {
            Failure(VideoError(error.message, error))
        }

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
     * Attempts to join a [Call]. If successful, gives us more information about the
     * user and the call itself.
     *
     * @param request The details of the call, like the ID and its type.
     * @return [Result] wrapper around the response from the server, or an error if something went
     * wrong.
     */
    override suspend fun joinCall(
        type: String,
        id: String,
        request: GetOrCreateCallRequest
    ): Result<JoinCallResponse> = try {
        val response = videoCallApi.joinCall(
            type = type,
            id = id,
            getOrCreateCallRequest = request
        )

        Success(response)
    } catch (error: Throwable) {
        Failure(VideoError(error.message, error))
    }

    /**
     * Finds the correct server to connect to for given user and [request]. In case there are no
     * servers, returns an error to the user.
     *
     * @param request The data used to find the best server.
     * @return [Result] wrapper around the response from the server, or an error if something went
     * wrong.
     */
    override suspend fun selectEdgeServer(
        type: String,
        id: String,
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
     * Sends a user-based event to the API to notify if we've changed something in the state of the
     * call.
     *
     * @param sendEventRequest The request holding information about the event type and the call.
     * @return a [Result] wrapper if the call succeeded or not.
     */
    override suspend fun sendUserEvent(sendEventRequest: SendEventRequest): Result<Boolean> = try {
        callCoordinatorService.sendEvent(sendEventRequest = sendEventRequest)

        Success(true)
    } catch (error: Throwable) {
        Failure(VideoError(error.message, error))
    }

    /**
     * Sends a custom event with encoded JSON data.
     *
     * @param sendCustomEventRequest The request holding the CID and the data.
     */
    override suspend fun sendCustomEvent(sendCustomEventRequest: SendCustomEventRequest): Result<Boolean> =
        try {
            callCoordinatorService.sendCustomEvent(sendCustomEventRequest = sendCustomEventRequest)

            Success(true)
        } catch (error: Throwable) {
            Failure(VideoError(error.message, error))
        }

    /**
     * Sends invite to people for an existing call.
     *
     * @param users The users to invite.
     * @param cid The call ID.
     * @return [Result] if the operation is successful or not.
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

    override suspend fun queryUsers(request: QueryUsersRequest): Result<List<CallUser>> = try {
        val users = callCoordinatorService.queryUsers(request).users

        Success(users.map { it.toCallUser() })
    } catch (error: Throwable) {
        Failure(VideoError(error.message, error))
    }
}
