package org.openapitools.client.apis

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import org.openapitools.client.models.APIError

interface TranscriptionApi {
    /**
     * Start transcription
     * Starts transcription  Required permissions: - StartTranscription 
     * Responses:
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type 
     * @param id 
     * @return [Unit]
     */
    @POST("/video/call/{type}/{id}/start_transcription")
    suspend fun startTranscription(@Path("type") type: String, @Path("id") id: String): Unit

    /**
     * Stop transcription
     * Stops transcription  Required permissions: - StopTranscription 
     * Responses:
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type 
     * @param id 
     * @return [Unit]
     */
    @POST("/video/call/{type}/{id}/stop_transcription")
    suspend fun stoptranscription(@Path("type") type: String, @Path("id") id: String): Unit

}
