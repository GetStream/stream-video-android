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

import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.model.StreamCallCid
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.utils.Result
import org.openapitools.client.models.GetCallEdgeServerRequest
import org.openapitools.client.models.GetCallEdgeServerResponse
import org.openapitools.client.models.GetOrCreateCallRequest
import org.openapitools.client.models.GetOrCreateCallResponse
import org.openapitools.client.models.JoinCallResponse
import org.openapitools.client.models.QueryMembersRequest
import org.openapitools.client.models.SendEventRequest
import stream.video.coordinator.call_v1.Call
import stream.video.coordinator.client_v1_rpc.CreateCallResponse
import stream.video.coordinator.client_v1_rpc.CreateDeviceRequest
import stream.video.coordinator.client_v1_rpc.CreateDeviceResponse
import stream.video.coordinator.client_v1_rpc.DeleteDeviceRequest

internal interface CallCoordinatorClient {

    /**
     * Create a new Device used to receive Push Notifications.
     *
     * @param createDeviceRequest The device data.
     * @return [CreateDeviceResponse] which holds the device.
     */
    suspend fun createDevice(createDeviceRequest: CreateDeviceRequest): Result<CreateDeviceResponse>

    /**
     * Delete a Device used to receive Push Notifications.
     *
     * @param deleteDeviceRequest The device data.
     * @return Result if the operation was successful or not.
     */
    suspend fun deleteDevice(deleteDeviceRequest: DeleteDeviceRequest): Result<Unit>

    /**
     * If the call exists, it fetches the existing instance rather than creating a brand new call.
     *
     * @param getOrCreateCallRequest The information used to describe the call.
     * @return [CreateCallResponse] which holds the cached or newly created [Call].
     */
    suspend fun getOrCreateCall(
        id: String,
        type: String,
        getOrCreateCallRequest: GetOrCreateCallRequest
    ): Result<GetOrCreateCallResponse>

    /**
     * Asks the server to join a call. This gives the user information which servers they can
     * choose from to fully join the call experience, based on latency.
     *
     * @param request The information used to prepare a call.
     * @return [JoinCallResponse] which helps us determine the correct connection.
     */
    suspend fun joinCall(
        type: String,
        id: String,
        request: GetOrCreateCallRequest
    ): Result<JoinCallResponse>

    /**
     * Asks the API for a correct edge server that can handle a connection for the given request.
     *
     * @param request The set of information used to find the server.
     * @return a [Result] wrapper of the [GetCallEdgeServerRequest], based on the API response.
     */
    suspend fun selectEdgeServer(
        type: String,
        id: String,
        request: GetCallEdgeServerRequest
    ): Result<GetCallEdgeServerResponse>

    /**
     * Sends a user-based event to the API to notify if we've changed something in the state of the
     * call.
     *
     * @param sendEventRequest The request holding information about the event type and the call.
     * @return a [Result] wrapper if the call succeeded or not.
     */
    suspend fun sendUserEvent(
        id: String,
        type: String,
        sendEventRequest: SendEventRequest
    ): Result<Boolean>

    /**
     * Sends invite to people for an existing call.
     *
     * @param users The users to invite.
     * @param cid The call ID.
     * @return [Result] if the operation is successful or not.
     */
    suspend fun inviteUsers(users: List<User>, cid: StreamCallCid): Result<Unit>

    /**
     * Queries users based on the given [request].
     *
     * @param request The request that describes the query filter, limit and sort.
     * @return [List] of [CallUser]s that match the given query.
     */
    suspend fun queryMembers(request: QueryMembersRequest): Result<List<CallUser>>
}
