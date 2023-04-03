package org.openapitools.client.apis

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import org.openapitools.client.models.APIError
import org.openapitools.client.models.StopLiveResponse

interface LivestreamingApi {
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
    suspend fun startBroadcasting(@Path("type") type: String, @Path("id") id: String): Unit

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
    suspend fun stopBroadcasting(@Path("type") type: String, @Path("id") id: String): Unit

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
    suspend fun stopLive(@Path("type") type: String, @Path("id") id: String): StopLiveResponse

    /**
     * Stop recording
     * Stops recording  Sends events: - call.recording_stopped  Required permissions: - StopRecording 
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
