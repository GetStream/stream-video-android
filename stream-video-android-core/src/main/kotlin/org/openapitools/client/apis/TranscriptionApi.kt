package org.openapitools.client.apis


import retrofit2.http.POST
import retrofit2.http.Path






import org.openapitools.client.models.StartTranscriptionResponse
import org.openapitools.client.models.StopTranscriptionResponse

interface TranscriptionApi {
    /**
     * Start transcription
     * Starts transcription  Required permissions: - StartTranscription 
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type 
     * @param id 
     * @return [StartTranscriptionResponse]
     */
    @POST("/video/call/{type}/{id}/start_transcription")
    suspend fun startTranscription(
        @Path("type") type: String, 
        @Path("id") id: String
    ): StartTranscriptionResponse

    /**
     * Stop transcription
     * Stops transcription  Required permissions: - StopTranscription 
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type 
     * @param id 
     * @return [StopTranscriptionResponse]
     */
    @POST("/video/call/{type}/{id}/stop_transcription")
    suspend fun stopTranscription(
        @Path("type") type: String, 
        @Path("id") id: String
    ): StopTranscriptionResponse

}
