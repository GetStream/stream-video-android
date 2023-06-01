package org.openapitools.client.apis


import retrofit2.http.Body
import retrofit2.http.POST






import org.openapitools.client.models.CreateGuestRequest
import org.openapitools.client.models.CreateGuestResponse

interface DefaultApi {
    /**
     * Create Guest
     *  
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param createGuestRequest 
     * @return [CreateGuestResponse]
     */
    @POST("/video/guest")
    suspend fun createGuest(
        @Body createGuestRequest: CreateGuestRequest
    ): CreateGuestResponse

}
