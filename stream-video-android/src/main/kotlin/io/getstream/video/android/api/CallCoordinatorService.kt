package io.getstream.video.android.api

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import stream.video.SelectEdgeServerRequest
import stream.video.SelectEdgeServerResponse

/**
 * Main service used to communicate with our API regarding video services.
 *
 * BASE_URL: http://<base-endpoint>/stream.video.CallCoordinatorService
 * For testing purposes, use "localhost:26991" for <base-endpoint>.
 *
 * We also override the Content-Type header to match our BE implementation.
 */
public interface CallCoordinatorService {

    @Headers("Content-Type: application/protobuf")
    @POST("/stream.video.CallCoordinatorService/SelectEdgeServer")
    public suspend fun selectEdgeServer(@Body selectEdgeServerRequest: SelectEdgeServerRequest): SelectEdgeServerResponse
}