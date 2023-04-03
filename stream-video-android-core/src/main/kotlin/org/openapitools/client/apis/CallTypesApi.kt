package org.openapitools.client.apis

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import org.openapitools.client.models.APIError
import org.openapitools.client.models.CreateCallTypeRequest
import org.openapitools.client.models.CreateCallTypeResponse
import org.openapitools.client.models.GetCallTypeResponse
import org.openapitools.client.models.ListCallTypeResponse
import org.openapitools.client.models.UpdateCallTypeRequest
import org.openapitools.client.models.UpdateCallTypeResponse

interface CallTypesApi {
    /**
     * Create Call Type
     * 
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param createCallTypeRequest 
     * @return [CreateCallTypeResponse]
     */
    @POST("/video/calltypes")
    suspend fun createCallType(@Body createCallTypeRequest: CreateCallTypeRequest): CreateCallTypeResponse

    /**
     * Delete Call Type
     * 
     * Responses:
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param name 
     * @return [Unit]
     */
    @DELETE("/video/calltypes/{name}")
    suspend fun deleteCallType(@Path("name") name: String): Unit

    /**
     * Get Call Type
     * 
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param name 
     * @return [GetCallTypeResponse]
     */
    @GET("/video/calltypes/{name}")
    suspend fun getCallType(@Path("name") name: String): GetCallTypeResponse

    /**
     * List Call Type
     * 
     * Responses:
     *  - 200: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @return [ListCallTypeResponse]
     */
    @GET("/video/calltypes")
    suspend fun listCallTypes(): ListCallTypeResponse

    /**
     * Update Call Type
     * 
     * Responses:
     *  - 201: Successful response
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param name 
     * @param updateCallTypeRequest 
     * @return [UpdateCallTypeResponse]
     */
    @PUT("/video/calltypes/{name}")
    suspend fun updateCallType(@Path("name") name: String, @Body updateCallTypeRequest: UpdateCallTypeRequest): UpdateCallTypeResponse

}
