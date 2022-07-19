/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.client.coordinator

import io.getstream.video.android.api.CallCoordinatorService
import io.getstream.video.android.errors.VideoError
import io.getstream.video.android.utils.Failure
import io.getstream.video.android.utils.Result
import io.getstream.video.android.utils.Success
import stream.video.CreateCallRequest
import stream.video.CreateCallResponse
import stream.video.JoinCallRequest
import stream.video.JoinCallResponse
import stream.video.SelectEdgeServerRequest
import stream.video.SelectEdgeServerResponse

/**
 * An accessor that allows us to communicate with the API around video calls.
 */
internal class CallCoordinatorClientImpl(
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

            Success(response)
        } catch (error: Throwable) {
            Failure(VideoError(error.message, error))
        }

    override suspend fun joinCall(request: JoinCallRequest): Result<JoinCallResponse> =
        try {
            val response = callCoordinatorService.joinCall(request)

            Success(response)
        } catch (error: Throwable) {
            Failure(VideoError(error.message, error))
        }

    override suspend fun createCall(createCallRequest: CreateCallRequest): Result<CreateCallResponse> =
        try {
            val response = callCoordinatorService.createCall(createCallRequest)

            Success(response)
        } catch (error: Throwable) {
            Failure(VideoError(error.message, error))
        }
}
