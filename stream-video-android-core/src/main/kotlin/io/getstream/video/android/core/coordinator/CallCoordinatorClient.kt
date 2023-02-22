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
import stream.video.coordinator.client_v1_rpc.CreateDeviceRequest
import stream.video.coordinator.client_v1_rpc.CreateDeviceResponse
import stream.video.coordinator.client_v1_rpc.DeleteDeviceRequest

internal interface CallCoordinatorClient {

    /**
     * Create a new Device used to receive Push Notifications.
     *
     * @param createDeviceRequest The device data.
     *
     * @return [CreateDeviceResponse] witch holds the device.
     */
    suspend fun createDevice(createDeviceRequest: CreateDeviceRequest): Result<CreateDeviceResponse>

    /**
     * Delete a Device used to receive Push Notifications.
     *
     * @param deleteDeviceRequest The device data.
     *
     * @return [Result] if the operation was successful or not.
     */
    suspend fun deleteDevice(deleteDeviceRequest: DeleteDeviceRequest): Result<Unit>

    /**
     * Returns an existing call or creates and returns a new one, based on the given request data.
     *
     * @param id The ID of the call.
     * @param type The type of the call.
     * @param getOrCreateCallRequest The request data describing the call.
     *
     * @return [GetOrCreateCallResponse] Containing the call information.
     */
    suspend fun getOrCreateCall(
        id: String,
        type: String,
        getOrCreateCallRequest: GetOrCreateCallRequest
    ): Result<GetOrCreateCallResponse>

    /**
     * Attempts to join a [Call]. If successful, gives us more information about the
     * user and the call itself.
     *
     * @param id The ID of the call.
     * @param type The type of the call.
     * @param request The details of the call, like the ID and its type.
     *
     * @return [Result] wrapper around the response from the server, or an error if something went
     * wrong.
     */
    suspend fun joinCall(
        id: String,
        type: String,
        request: GetOrCreateCallRequest
    ): Result<JoinCallResponse>

    /**
     * Finds the correct server to connect to for given user and [request]. In case there are no
     * servers, returns an error to the user.
     *
     * @param id The ID of the call.
     * @param type The type of the call.
     * @param request The data used to find the best server.
     *
     * @return [Result] wrapper around the response from the server, or an error if something went
     * wrong.
     */
    suspend fun selectEdgeServer(
        id: String,
        type: String,
        request: GetCallEdgeServerRequest
    ): Result<GetCallEdgeServerResponse>

    /**
     * Sends a user-based event to the API to notify if we've changed something in the state of the
     * call.
     *
     * @param id The ID of the call.
     * @param type The type of the call.
     * @param sendEventRequest The request holding information about the event type and the call.
     *
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
     *
     * @return [Result] if the operation is successful or not.
     */
    suspend fun inviteUsers(users: List<User>, cid: StreamCallCid): Result<Unit>

    /**
     * Queries the API for members of a call.
     *
     * @param request The [QueryMembersRequest] containing specific information about the query and
     * the call
     *
     * @return [List] of [CallUser]s that match the given query.
     */
    suspend fun queryMembers(request: QueryMembersRequest): Result<List<CallUser>>
}
