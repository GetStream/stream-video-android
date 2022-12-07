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

package io.getstream.video.android.coordinator

import io.getstream.video.android.model.CallUser
import io.getstream.video.android.model.StreamCallCid
import io.getstream.video.android.model.User
import io.getstream.video.android.utils.Result
import stream.video.coordinator.call_v1.Call
import stream.video.coordinator.client_v1_rpc.CreateCallRequest
import stream.video.coordinator.client_v1_rpc.CreateCallResponse
import stream.video.coordinator.client_v1_rpc.CreateDeviceRequest
import stream.video.coordinator.client_v1_rpc.CreateDeviceResponse
import stream.video.coordinator.client_v1_rpc.GetCallEdgeServerRequest
import stream.video.coordinator.client_v1_rpc.GetCallEdgeServerResponse
import stream.video.coordinator.client_v1_rpc.GetOrCreateCallRequest
import stream.video.coordinator.client_v1_rpc.GetOrCreateCallResponse
import stream.video.coordinator.client_v1_rpc.JoinCallRequest
import stream.video.coordinator.client_v1_rpc.JoinCallResponse
import stream.video.coordinator.client_v1_rpc.QueryUsersRequest
import stream.video.coordinator.client_v1_rpc.SendCustomEventRequest
import stream.video.coordinator.client_v1_rpc.SendEventRequest

public interface CallCoordinatorClient {

    /**
     * Create a new Device used to receive Push Notifications.
     *
     * @param createDeviceRequest The device data.
     * @return [CreateDeviceResponse] witch holds the device.
     */
    public suspend fun createDevice(createDeviceRequest: CreateDeviceRequest): Result<CreateDeviceResponse>

    /**
     * Creates a new call that users can connect to and communicate in.
     *
     * @param createCallRequest The information used to describe the call.
     * @return [CreateCallResponse] which holds the newly created [Call].
     */
    public suspend fun createCall(createCallRequest: CreateCallRequest): Result<CreateCallResponse>

    /**
     * Does the same as [createCall] but if the call exists, it fetches the existing instance rather
     * than creating a brand new call.
     *
     * @param getOrCreateCallRequest The information used to describe the call.
     * @return [CreateCallResponse] which holds the cached or newly created [Call].
     */
    public suspend fun getOrCreateCall(getOrCreateCallRequest: GetOrCreateCallRequest): Result<GetOrCreateCallResponse>

    /**
     * Asks the server to join a call. This gives the user information which servers they can
     * choose from to fully join the call experience, based on latency.
     *
     * @param request The information used to prepare a call.
     * @return [JoinCallResponse] which helps us determine the correct connection.
     */
    public suspend fun joinCall(request: JoinCallRequest): Result<JoinCallResponse>

    /**
     * Asks the API for a correct edge server that can handle a connection for the given request.
     *
     * @param request The set of information used to find the server.
     * @return a [Result] wrapper of the [GetCallEdgeServerRequest], based on the API response.
     */
    public suspend fun selectEdgeServer(request: GetCallEdgeServerRequest): Result<GetCallEdgeServerResponse>

    /**
     * Sends a user-based event to the API to notify if we've changed something in the state of the
     * call.
     *
     * @param sendEventRequest The request holding information about the event type and the call.
     * @return a [Result] wrapper if the call succeeded or not.
     */
    public suspend fun sendUserEvent(sendEventRequest: SendEventRequest): Result<Boolean>

    /**
     * Sends a custom event with encoded JSON data.
     *
     * @param sendCustomEventRequest The request holding the CID and the data.
     */
    public suspend fun sendCustomEvent(sendCustomEventRequest: SendCustomEventRequest): Result<Boolean>

    /**
     * Sends invite to people for an existing call.
     *
     * @param users The users to invite.
     * @param cid The call ID.
     * @return [Result] if the operation is successful or not.
     */
    public suspend fun inviteUsers(users: List<User>, cid: StreamCallCid): Result<Unit>

    /**
     * Queries users based on the given [request].
     *
     * @param request The request that describes the query filter, limit and sort.
     * @return [List] of [CallUser]s that match the given query.
     */
    public suspend fun queryUsers(request: QueryUsersRequest): Result<List<CallUser>>
}
