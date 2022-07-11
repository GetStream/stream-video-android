package io.getstream.video.android.client

import stream.video.SelectEdgeServerRequest
import stream.video.SelectEdgeServerResponse
import io.getstream.video.android.utils.Result

interface CallCoordinatorClient {

    /**
     * Asks the API for a correct edge server that can handle a connection for the given request.
     *
     * @param request The set of information used to find the server.
     * @return a [Result] wrapper of the [SelectEdgeServerResponse], based on the API response.
     */
    suspend fun selectEdgeServer(request: SelectEdgeServerRequest): Result<SelectEdgeServerResponse>
}