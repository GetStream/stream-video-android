package io.getstream.video.android.client

import io.getstream.video.android.api.CallCoordinatorService
import stream.video.SelectEdgeServerRequest
import stream.video.SelectEdgeServerResponse
import utils.Result

class CallCoordinatorClientImpl(
    private val callCoordinatorService: CallCoordinatorService
) : CallCoordinatorClient {

    /**
     * Finds the correct server to connect to for given user and [request]. In case there are no
     * servers, returns an error to the user.
     *
     * @param request The data used to find the best server.
     * @return [Result] wrapper around the response from the server, or an error if something went
     * wrong.
     */
    override suspend fun selectEdgeServer(request: SelectEdgeServerRequest): Result<SelectEdgeServerResponse> =
        try {
            val response = callCoordinatorService.selectEdgeServer(request)

            if (response.edge_server != null) {
                Result.success(response)
            } else {
                throw NullPointerException("Invalid response, edge server is null.")
            }
        } catch (error: Throwable) {
            Result.error(error)
        }
}