package org.openapitools.client.apis


import retrofit2.http.Body
import retrofit2.http.POST






import org.openapitools.client.models.CreateDeviceRequest

interface PushApi {
    /**
     * Create device
     * Adds a new device to a user, if the same device already exists the call will have no effect 
     * Responses:
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param createDeviceRequest 
     * @return [Unit]
     */
    @POST("/video/devices")
    suspend fun createDevice(
        @Body createDeviceRequest: CreateDeviceRequest
    ): Unit

}
