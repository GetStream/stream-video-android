package org.openapitools.client.apis


import retrofit2.http.Body
import retrofit2.http.POST






import org.openapitools.client.models.CreateDeviceRequest
import org.openapitools.client.models.Response

interface PushApi {
    /**
     * Create device
     * Adds a new device to a user, if the same device already exists the call will have no effect 
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param createDeviceRequest 
     * @return [Response]
     */
    @POST("/video/devices")
    suspend fun createDevice(
        @Body createDeviceRequest: CreateDeviceRequest
    ): Response

}
