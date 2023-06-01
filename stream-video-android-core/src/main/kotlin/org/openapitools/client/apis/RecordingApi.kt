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


import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path






import org.openapitools.client.models.ListRecordingsResponse
import org.openapitools.client.models.StartRecordingResponse
import org.openapitools.client.models.StopRecordingResponse

interface RecordingApi {
    /**
     * List recordings (type, id)
     * Lists recordings  Required permissions: - ListRecordings
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [ListRecordingsResponse]
     */
    @GET("/video/call/{type}/{id}/recordings")
    suspend fun listRecordingsTypeId0(
        @Path("type") type: String,
        @Path("id") id: String
    ): ListRecordingsResponse

    /**
     * List recordings (type, id, session)
     * Lists recordings  Required permissions: - ListRecordings
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
    suspend fun listRecordingsTypeIdSession1(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("session") session: String
    ): ListRecordingsResponse

    /**
     * Start recording
     * Starts recording  Sends events: - call.recording_started  Required permissions: - StopRecording
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [StartRecordingResponse]
     */
    @POST("/video/call/{type}/{id}/start_recording")
    suspend fun startRecording(
        @Path("type") type: String,
        @Path("id") id: String
    ): StartRecordingResponse

    /**
     * Stop recording
     * Stops recording  Sends events: - call.recording_stopped  Required permissions: - StopRecording
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type
     * @param id
     * @return [StopRecordingResponse]
     */
    @POST("/video/call/{type}/{id}/stop_recording")
    suspend fun stopRecording(
        @Path("type") type: String,
        @Path("id") id: String
    ): StopRecordingResponse

}
