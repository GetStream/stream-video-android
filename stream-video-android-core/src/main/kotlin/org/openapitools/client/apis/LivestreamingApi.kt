package org.openapitools.client.apis


import retrofit2.http.POST
import retrofit2.http.Path






import org.openapitools.client.models.StartBroadcastingResponse
import org.openapitools.client.models.StopBroadcastingResponse
import org.openapitools.client.models.StopLiveResponse
import org.openapitools.client.models.StopRecordingResponse

interface LivestreamingApi {
    /**
     * Start broadcasting
     * Starts broadcasting  Required permissions: - StartBroadcasting 
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type 
     * @param id 
     * @return [StartBroadcastingResponse]
     */
    @POST("/video/call/{type}/{id}/start_broadcasting")
    suspend fun startBroadcasting(
        @Path("type") type: String, 
        @Path("id") id: String
    ): StartBroadcastingResponse

    /**
     * Stop broadcasting
     * Stops broadcasting  Required permissions: - StopBroadcasting 
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type 
     * @param id 
     * @return [StopBroadcastingResponse]
     */
    @POST("/video/call/{type}/{id}/stop_broadcasting")
    suspend fun stopBroadcasting(
        @Path("type") type: String, 
        @Path("id") id: String
    ): StopBroadcastingResponse

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
