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

import org.openapitools.client.models.CreateDeviceRequest
import org.openapitools.client.models.ListDevicesResponse
import org.openapitools.client.models.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface DevicesApi {
    /**
     * Create device
     * Adds a new device to a user, if the same device already exists the call will have no effect * Responses:
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param createDeviceRequest * @return [Unit]
     */
    @POST("/video/devices")
    suspend fun createDevice(
        @Body createDeviceRequest: CreateDeviceRequest
    ): Unit

    /**
     * Delete device
     * Deletes one device * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param id  (optional)
     * @param userId  (optional)
     * @return [Response]
     */
    @DELETE("/video/devices")
    suspend fun deleteDevice(
        @Query("id") id: String? = null,
        @Query("user_id") userId: String? = null
    ): Response

    /**
     * List devices
     * Returns all available devices * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param userId  (optional)
     * @return [ListDevicesResponse]
     */
    @GET("/video/devices")
    suspend fun listDevices(
        @Query("user_id") userId: String? = null
    ): ListDevicesResponse
}
