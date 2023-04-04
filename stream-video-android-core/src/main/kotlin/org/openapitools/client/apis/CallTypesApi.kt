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
import org.openapitools.client.models.GetCallTypeResponse
import org.openapitools.client.models.ListCallTypeResponse
import org.openapitools.client.models.UpdateCallTypeRequest
import org.openapitools.client.models.UpdateCallTypeResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CallTypesApi {
    /**
     * Create Call Type
     * * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param createCallTypeRequest * @return [CreateCallTypeResponse]
     */
    @POST("/video/calltypes")
    suspend fun createCallType(
        @Body createCallTypeRequest: CreateCallTypeRequest
    ): CreateCallTypeResponse

    /**
     * Delete Call Type
     * * Responses:
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param name * @return [Unit]
     */
    @DELETE("/video/calltypes/{name}")
    suspend fun deleteCallType(
        @Path("name") name: String
    ): Unit

    /**
     * Get Call Type
     * * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param name * @return [GetCallTypeResponse]
     */
    @GET("/video/calltypes/{name}")
    suspend fun getCallType(
        @Path("name") name: String
    ): GetCallTypeResponse

    /**
     * List Call Type
     * * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @return [ListCallTypeResponse]
     */
    @GET("/video/calltypes")
    suspend fun listCallTypes(): ListCallTypeResponse

    /**
     * Update Call Type
     * * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param name * @param updateCallTypeRequest * @return [UpdateCallTypeResponse]
     */
    @PUT("/video/calltypes/{name}")
    suspend fun updateCallType(
        @Path("name") name: String,
        @Body updateCallTypeRequest: UpdateCallTypeRequest
    ): UpdateCallTypeResponse
}
