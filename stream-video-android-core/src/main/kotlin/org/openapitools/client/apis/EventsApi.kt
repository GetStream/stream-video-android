package org.openapitools.client.apis

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import org.openapitools.client.models.APIError
import org.openapitools.client.models.SendEventRequest
import org.openapitools.client.models.SendEventResponse

interface EventsApi {
    /**
     * Send event
     * Sends event to the call
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param type 
     * @param id 
     * @param sendEventRequest 
     * @return [SendEventResponse]
     */
    @POST("/video/call/{type}/{id}/event")
    suspend fun sendEvent(@Path("type") type: String, @Path("id") id: String, @Body sendEventRequest: SendEventRequest): SendEventResponse

}
