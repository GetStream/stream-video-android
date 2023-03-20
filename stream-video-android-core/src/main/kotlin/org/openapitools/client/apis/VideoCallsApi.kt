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

package org.openapitools.client.apis

import org.openapitools.client.models.BlockUserRequest
import org.openapitools.client.models.BlockUserResponse
import org.openapitools.client.models.EndCallResponse
import org.openapitools.client.models.GetCallEdgeServerRequest
import org.openapitools.client.models.GetCallEdgeServerResponse
import org.openapitools.client.models.GetEdgesResponse
import org.openapitools.client.models.GetOrCreateCallRequest
import org.openapitools.client.models.GetOrCreateCallResponse
import org.openapitools.client.models.GoLiveResponse
import org.openapitools.client.models.JoinCallResponse
import org.openapitools.client.models.MuteUsersRequest
import org.openapitools.client.models.MuteUsersResponse
import org.openapitools.client.models.QueryMembersRequest
import org.openapitools.client.models.QueryMembersResponse
import org.openapitools.client.models.StopLiveResponse
import org.openapitools.client.models.UnblockUserRequest
import org.openapitools.client.models.UnblockUserResponse
import org.openapitools.client.models.UpdateCallRequest
import org.openapitools.client.models.UpdateCallResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface VideoCallsApi {

    /**
     * Block user on a call
     * Block a user, preventing them from joining the call until they are unblocked.
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param blockUserRequest
     * @return [BlockUserResponse]
     */
    @POST("video/call/{type}/{id}/block")
    suspend fun blockUser(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body blockUserRequest: BlockUserRequest
    ): BlockUserResponse

    /**
     * End call
     *
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [EndCallResponse]
     */
    @POST("/video/call/{type}/{id}/mark_ended")
    suspend fun endCall(
        @Path("type") type: String,
        @Path("id") id: String
    ): EndCallResponse

    /**
     * Get Call Edge Server
     *
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param getCallEdgeServerRequest
     * @return [GetCallEdgeServerResponse]
     */
    @POST("/video/call/{type}/{id}/get_edge_server")
    suspend fun getCallEdgeServer(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body getCallEdgeServerRequest: GetCallEdgeServerRequest
    ): GetCallEdgeServerResponse

    /**
     * Get Edges
     *
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @return [GetEdgesResponse]
     */
    @GET("/video/edges")
    suspend fun getEdges(): GetEdgesResponse

    /**
     * Get or create a call
     * Gets or creates a new call
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param getOrCreateCallRequest
     * @return [GetOrCreateCallResponse]
     */
    @POST("/video/call/{type}/{id}")
    suspend fun getOrCreateCall(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body getOrCreateCallRequest: GetOrCreateCallRequest
    ): GetOrCreateCallResponse

    /**
     * Set call as live
     *
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [GoLiveResponse]
     */
    @POST("video/call/{type}/{id}/go_live")
    suspend fun goLive(
        @Path("type") type: String,
        @Path("id") id: String
    ): GoLiveResponse

    /**
     * Join call
     * Request to join a call
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param getOrCreateCallRequest
     * @return [JoinCallResponse]
     */
    @POST("/video/join_call/{type}/{id}")
    suspend fun joinCall(
        @Path("type") type: String,
        @Path("id") id: String,
        @Query("connection_id") connectionId: String,
        @Body getOrCreateCallRequest: GetOrCreateCallRequest
    ): JoinCallResponse

    /**
     * Mute users
     * Mutes users in a call
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param muteUsersRequest
     * @return [MuteUsersResponse]
     */
    @POST("/video/call/{type}/{id}/mute_users")
    suspend fun muteUsers(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body muteUsersRequest: MuteUsersRequest
    ): MuteUsersResponse

    /**
     * Query call members
     * Query call members with filter query
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param queryMembersRequest
     * @return [QueryMembersResponse]
     */
    @POST("/video/call/members")
    suspend fun queryMembers(
        @Body queryMembersRequest: QueryMembersRequest,
    ): QueryMembersResponse

    /**
     * Set call as not live
     *
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [StopLiveResponse]
     */
    @POST("video/call/{type}/{id}/stop_live")
    suspend fun stopLive(
        @Path("type") type: String,
        @Path("id") id: String
    ): StopLiveResponse

    /**
     * Unblocks user on a call
     * Removes the block for a user on a call. The user will be able to join the call again.
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param unblockUserRequest
     * @return [UnblockUserResponse]
     */
    @POST("/video/call/{type}/{id}/unblock")
    suspend fun unblockUser(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body unblockUserRequest: UnblockUserRequest
    ): UnblockUserResponse

    /**
     * Update Call
     *
     * Responses:
     *  - 200: Call
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param updateCallRequest
     * @return [UpdateCallResponse]
     */
    @PATCH("/video/call/{type}/{id}")
    suspend fun updateCall(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body updateCallRequest: UpdateCallRequest
    ): UpdateCallResponse
}
