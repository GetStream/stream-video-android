package org.openapitools.client.apis

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import org.openapitools.client.models.APIError
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
import org.openapitools.client.models.UpdateCallRequest
import org.openapitools.client.models.UpdateCallResponse

interface VideoCallsApi {
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
    suspend fun endCall(@Path("type") type: String, @Path("id") id: String): EndCallResponse

    /**
     * Get Call
     * 
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type 
     * @param id 
     * @return [GetCallResponse]
     */
    @GET("/video/call/{type}/{id}")
    suspend fun getCall(@Path("type") type: String, @Path("id") id: String): GetCallResponse

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
    suspend fun getCallEdgeServer(@Path("type") type: String, @Path("id") id: String, @Body getCallEdgeServerRequest: GetCallEdgeServerRequest): GetCallEdgeServerResponse

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
    suspend fun getOrCreateCall(@Path("type") type: String, @Path("id") id: String, @Body getOrCreateCallRequest: GetOrCreateCallRequest): GetOrCreateCallResponse

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
    @POST("/video/call/{type}/{id}/go_live")
    suspend fun goLive(@Path("type") type: String, @Path("id") id: String): GoLiveResponse

    /**
     * Join call (type, id)
     * Request to join a call
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
    @POST("/video/join_call/{type}/{id}")
    suspend fun joinCallTypeId0(@Path("type") type: String, @Path("id") id: String, @Body joinCallRequest: JoinCallRequest, @Query("connection_id") connectionId: String? = null): JoinCallResponse

    /**
     * Join call (type, id)
     * Request to join a call
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
    suspend fun joinCallTypeId1(@Path("type") type: String, @Path("id") id: String, @Body joinCallRequest: JoinCallRequest, @Query("connection_id") connectionId: String? = null): JoinCallResponse

    /**
     * Query call
     * Query calls with filter query
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
    suspend fun queryCalls(@Body queryCallsRequest: QueryCallsRequest, @Query("connection_id") connectionId: String? = null): QueryCallsResponse

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
    suspend fun queryMembers(@Body queryMembersRequest: QueryMembersRequest): QueryMembersResponse

    /**
     * Send reaction to the call
     * Sends reaction to the call
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
    suspend fun sendVideoReaction(@Path("type") type: String, @Path("id") id: String, @Body sendReactionRequest: SendReactionRequest): SendReactionResponse

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
    @POST("/video/call/{type}/{id}/stop_live")
    suspend fun stopLive(@Path("type") type: String, @Path("id") id: String): StopLiveResponse

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
    suspend fun unblockUser(@Path("type") type: String, @Path("id") id: String, @Body unblockUserRequest: UnblockUserRequest): UnblockUserResponse

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
    @PATCH("call/{type}/{id}")
    suspend fun updateCall(@Path("type") type: String, @Path("id") id: String, @Body updateCallRequest: UpdateCallRequest): UpdateCallResponse

}
