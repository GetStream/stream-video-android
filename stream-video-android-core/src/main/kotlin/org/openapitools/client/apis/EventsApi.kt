package org.openapitools.client.apis


import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path






import org.openapitools.client.models.SendEventRequest
import org.openapitools.client.models.SendEventResponse

interface EventsApi {
    /**
     * Send custom event
     * Sends custom event to the call  Sends events: - custom  Required permissions: - SendEvent 
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
    suspend fun sendEvent(
        @Path("type") type: String, 
        @Path("id") id: String, 
        @Body sendEventRequest: SendEventRequest
    ): SendEventResponse

}
