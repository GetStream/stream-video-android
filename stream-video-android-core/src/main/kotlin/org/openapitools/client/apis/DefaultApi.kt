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

import org.openapitools.client.models.CreateCallTypeRequest
import org.openapitools.client.models.CreateCallTypeResponse
import org.openapitools.client.models.GetCallResponse
import org.openapitools.client.models.GetCallTypeResponse
import org.openapitools.client.models.ListCallTypeResponse
import org.openapitools.client.models.ListRecordingsResponse
import org.openapitools.client.models.QueryCallsRequest
import org.openapitools.client.models.QueryCallsResponse
import org.openapitools.client.models.RequestPermissionRequest
import org.openapitools.client.models.RequestPermissionResponse
import org.openapitools.client.models.SendReactionRequest
import org.openapitools.client.models.SendReactionResponse
import org.openapitools.client.models.UpdateCallTypeRequest
import org.openapitools.client.models.UpdateCallTypeResponse
import org.openapitools.client.models.UpdateUserPermissionsRequest
import org.openapitools.client.models.UpdateUserPermissionsResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal interface DefaultApi {
    /**
     * Create Call Type
     *
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param createCallTypeRequest
     * @return [CreateCallTypeResponse]
     */
    @POST("/video/calltypes")
    suspend fun createCallType(
        @Body createCallTypeRequest: CreateCallTypeRequest,
    ): CreateCallTypeResponse

    /**
     * Delete Call Type
     *
     * Responses:
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param name
     * @return [Unit]
     */
    @DELETE("/video/calltypes/{name}")
    suspend fun deleteCallType(
        @Path("name") name: String,
    ): Unit

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
    suspend fun getCall(
        @Path("type") type: String,
        @Path("id") id: String,
    ): GetCallResponse

    /**
     * Get Call Type
     *
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param name
     * @return [GetCallTypeResponse]
     */
    @GET("/video/calltypes/{name}")
    suspend fun getCallType(
        @Path("name") name: String,
    ): GetCallTypeResponse

    /**
     * List Call Type
     *
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @return [ListCallTypeResponse]
     */
    @GET("/video/calltypes")
    suspend fun listCallTypes(): ListCallTypeResponse

    /**
     * List recordings
     * Lists recordings
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param session
     * @return [ListRecordingsResponse]
     */
    @GET("/video/call/{type}/{id}/{session}/recordings")
    suspend fun listRecordings(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("session") session: String,
    ): ListRecordingsResponse

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
    suspend fun queryCalls(
        @Body queryCallsRequest: QueryCallsRequest,
        @Query("connection_id") connectionId: String? = null,
    ): QueryCallsResponse

    /**
     * Request permission
     * Request permission to perform an action
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param requestPermissionRequest
     * @return [RequestPermissionResponse]
     */
    @POST("/video/call/{type}/{id}/request_permission")
    suspend fun requestPermission(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body requestPermissionRequest: RequestPermissionRequest,
    ): RequestPermissionResponse

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
    suspend fun sendVideoReaction(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body sendReactionRequest: SendReactionRequest,
    ): SendReactionResponse

    /**
     * Start broadcasting
     * Starts broadcasting
     * Responses:
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [Unit]
     */
    @POST("/video/call/{type}/{id}/start_broadcasting")
    suspend fun startBroadcasting(
        @Path("type") type: String,
        @Path("id") id: String,
    ): Unit

    /**
     * Start recording
     * Starts recording
     * Responses:
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [Unit]
     */
    @POST("/video/call/{type}/{id}/start_recording")
    suspend fun startRecording(
        @Path("type") type: String,
        @Path("id") id: String,
    ): Unit

    /**
     * Stop broadcasting
     * Stops broadcasting
     * Responses:
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [Unit]
     */
    @POST("/video/call/{type}/{id}/stop_broadcasting")
    suspend fun stopBroadcasting(
        @Path("type") type: String,
        @Path("id") id: String,
    ): Unit

    /**
     * Stop recording
     * Stops recording
     * Responses:
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [Unit]
     */
    @POST("/video/call/{type}/{id}/stop_recording")
    suspend fun stopRecording(
        @Path("type") type: String,
        @Path("id") id: String,
    ): Unit

    /**
     * Update Call Type
     *
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param name
     * @param updateCallTypeRequest
     * @return [UpdateCallTypeResponse]
     */
    @PUT("/video/calltypes/{name}")
    suspend fun updateCallType(
        @Path("name") name: String,
        @Body updateCallTypeRequest: UpdateCallTypeRequest,
    ): UpdateCallTypeResponse

    /**
     * Update user permissions
     * Updates user permissions
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @param updateUserPermissionsRequest
     * @return [UpdateUserPermissionsResponse]
     */
    @POST("/video/call/{type}/{id}/user_permissions")
    suspend fun updateUserPermissions(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body updateUserPermissionsRequest: UpdateUserPermissionsRequest,
    ): UpdateUserPermissionsResponse
}
