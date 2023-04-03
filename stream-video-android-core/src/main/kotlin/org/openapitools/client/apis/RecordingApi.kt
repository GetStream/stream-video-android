package org.openapitools.client.apis

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import org.openapitools.client.models.APIError
import org.openapitools.client.models.ListRecordingsResponse

interface RecordingApi {
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
    suspend fun listRecordings(@Path("type") type: String, @Path("id") id: String, @Path("session") session: String): ListRecordingsResponse

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
    suspend fun startRecording(@Path("type") type: String, @Path("id") id: String): Unit

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
    suspend fun stopRecording(@Path("type") type: String, @Path("id") id: String): Unit

}
