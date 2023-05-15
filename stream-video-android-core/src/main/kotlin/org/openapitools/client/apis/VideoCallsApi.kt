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


import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query






import org.openapitools.client.models.EndCallResponse
import org.openapitools.client.models.GetCallEdgeServerRequest
import org.openapitools.client.models.GetCallEdgeServerResponse
import org.openapitools.client.models.GetCallResponse
import org.openapitools.client.models.GetEdgesResponse
import org.openapitools.client.models.GetOrCreateCallRequest
import org.openapitools.client.models.GetOrCreateCallResponse
import org.openapitools.client.models.GoLiveResponse
import org.openapitools.client.models.JoinCallRequest
import org.openapitools.client.models.JoinCallResponse
import org.openapitools.client.models.QueryCallsRequest
import org.openapitools.client.models.QueryCallsResponse
import org.openapitools.client.models.QueryMembersRequest
import org.openapitools.client.models.QueryMembersResponse
import org.openapitools.client.models.SendReactionRequest
import org.openapitools.client.models.SendReactionResponse
import org.openapitools.client.models.StopLiveResponse
import org.openapitools.client.models.UnblockUserRequest
import org.openapitools.client.models.UnblockUserResponse
import org.openapitools.client.models.UpdateCallMembersRequest
import org.openapitools.client.models.UpdateCallMembersResponse
import org.openapitools.client.models.UpdateCallRequest
import org.openapitools.client.models.UpdateCallResponse

interface VideoCallsApi {
    /**
     * End call
     *   Sends events: - call.ended  Required permissions: - EndCall
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
     * Get Call
     *   Required permissions: - ReadCall
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param connectionId  (optional)
     * @return [GetCallResponse]
     */
    @GET("/video/call/{type}/{id}")
    suspend fun getCall(
        @Path("type") type: String,
        @Path("id") id: String,
        @Query("connection_id") connectionId: String? = null
    ): GetCallResponse

    /**
     * Get Call Edge Server
     * Retrieve the edge server information and credentials for a call.  Required permissions: - JoinCall
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
     * Returns the list of all edges available for video calls.
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @return [GetEdgesResponse]
     */
    @GET("/video/edges")
    suspend fun getEdges(
    ): GetEdgesResponse

    /**
     * Get or create a call
     * Gets or creates a new call  Sends events: - call.created  Required permissions: - CreateCall - ReadCall - UpdateCallSettings
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param getOrCreateCallRequest
     * @param connectionId  (optional)
     * @return [GetOrCreateCallResponse]
     */
    @POST("/video/call/{type}/{id}")
    suspend fun getOrCreateCall(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body getOrCreateCallRequest: GetOrCreateCallRequest,
        @Query("connection_id") connectionId: String? = null
    ): GetOrCreateCallResponse

    /**
     * Set call as live
     *   Sends events: - call.live_started  Required permissions: - UpdateCall
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [GoLiveResponse]
     */
    @POST("/video/call/{type}/{id}/go_live")
    suspend fun goLive(
        @Path("type") type: String,
        @Path("id") id: String
    ): GoLiveResponse

    /**
     * Join call
     * Request to join a call  Required permissions: - CreateCall - JoinCall
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param joinCallRequest
     * @param connectionId  (optional)
     * @return [JoinCallResponse]
     */
    @POST("/video/call/{type}/{id}/join")
    suspend fun joinCall(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body joinCallRequest: JoinCallRequest,
        @Query("connection_id") connectionId: String? = null
    ): JoinCallResponse

    /**
     * Query call
     * Query calls with filter query  Required permissions: - ReadCall
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param queryCallsRequest
     * @param connectionId  (optional)
     * @return [QueryCallsResponse]
     */
    @POST("/video/calls")
    suspend fun queryCalls(
        @Body queryCallsRequest: QueryCallsRequest,
        @Query("connection_id") connectionId: String? = null
    ): QueryCallsResponse

    /**
     * Query call members
     * Query call members with filter query  Required permissions: - ReadCall
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
        @Body queryMembersRequest: QueryMembersRequest
    ): QueryMembersResponse

    /**
     * Send reaction to the call
     * Sends reaction to the call  Sends events: - call.reaction_new  Required permissions: - CreateCallReaction
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param sendReactionRequest
     * @return [SendReactionResponse]
     */
    @POST("/video/call/{type}/{id}/reaction")
    suspend fun sendVideoReaction(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body sendReactionRequest: SendReactionRequest
    ): SendReactionResponse

    /**
     * Set call as not live
     *   Sends events: - call.updated  Required permissions: - UpdateCall
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [StopLiveResponse]
     */
    @POST("/video/call/{type}/{id}/stop_live")
    suspend fun stopLive(
        @Path("type") type: String,
        @Path("id") id: String
    ): StopLiveResponse

    /**
     * Unblocks user on a call
     * Removes the block for a user on a call. The user will be able to join the call again.  Sends events: - call.unblocked_user  Required permissions: - BlockUser
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
     *   Sends events: - call.updated  Required permissions: - UpdateCall
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

    /**
     * Update Call Member
     *   Sends events: - call.member_added - call.member_removed - call.member_updated  Required permissions: - RemoveCallMember - UpdateCallMember - UpdateCallMemberRole
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param updateCallMembersRequest
     * @return [UpdateCallMembersResponse]
     */
    @POST("/video/call/{type}/{id}/members")
    suspend fun updateCallMembers(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body updateCallMembersRequest: UpdateCallMembersRequest
    ): UpdateCallMembersResponse

}
