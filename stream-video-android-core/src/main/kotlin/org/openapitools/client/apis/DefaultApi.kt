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

import org.openapitools.client.models.QueryCallRequest
import org.openapitools.client.models.QueryCallsResponse
import org.openapitools.client.models.RequestPermissionRequest
import org.openapitools.client.models.RequestPermissionResponse
import org.openapitools.client.models.UpdateUserPermissionsRequest
import org.openapitools.client.models.UpdateUserPermissionsResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface DefaultApi {
    /**
     * Query call
     * Query calls with filter query
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param queryCallRequest
     * @param clientId  (optional)
     * @param connectionId  (optional)
     * @return [QueryCallsResponse]
     */
    @POST("calls")
    suspend fun queryCalls(
        @Body queryCallRequest: QueryCallRequest,
        @Query("client_id") clientId: String? = null,
        @Query("connection_id") connectionId: String? = null
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
    @POST("call/{type}/{id}/request_permission")
    suspend fun requestPermission(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body requestPermissionRequest: RequestPermissionRequest
    ): RequestPermissionResponse

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
    @POST("call/{type}/{id}/user_permissions")
    suspend fun updateUserPermissions(
        @Path("type") type: String,
        @Path("id") id: String,
        @Body updateUserPermissionsRequest: UpdateUserPermissionsRequest
    ): UpdateUserPermissionsResponse
}
