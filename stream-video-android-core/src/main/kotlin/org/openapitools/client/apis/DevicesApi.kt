package org.openapitools.client.apis


import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query






import org.openapitools.client.models.CreateDeviceRequest
import org.openapitools.client.models.ListDevicesResponse
import org.openapitools.client.models.Response

interface DevicesApi {
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

    /**
     * Delete device
     * Deletes one device 
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param id  (optional)
     * @param userId  (optional)
     * @return [Response]
     */
    @DELETE("/video/devices")
    suspend fun deleteDevice(
        @Query("id") id: String? = null, 
        @Query("user_id") userId: String? = null
    ): Response

    /**
     * List devices
     * Returns all available devices 
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param userId  (optional)
     * @return [ListDevicesResponse]
     */
    @GET("/video/devices")
    suspend fun listDevices(
        @Query("user_id") userId: String? = null
    ): ListDevicesResponse

}
