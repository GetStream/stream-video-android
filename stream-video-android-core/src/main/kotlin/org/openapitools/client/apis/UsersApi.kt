package org.openapitools.client.apis

import org.openapitools.client.infrastructure.CollectionFormats.*
import retrofit2.http.*
import retrofit2.Response
import okhttp3.RequestBody
import com.squareup.moshi.Json

import org.openapitools.client.models.APIError
import org.openapitools.client.models.VideoWSAuthMessageRequest

internal interface UsersApi {
    /**
     * Video Connect (WebSocket)
     * Establishes WebSocket connection for user
     * Responses:
     *  - 400: Bad request
     *  - 429: Too many requests
     *
     * @param videoWSAuthMessageRequest
     * @return [Unit]
     */
    @GET("video/connect")
    suspend fun videoConnect(@Body videoWSAuthMessageRequest: VideoWSAuthMessageRequest): Response<Unit>
}
