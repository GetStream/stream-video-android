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

import io.getstream.video.android.utils.Result
import stream.video.CreateCallRequest
import stream.video.CreateCallResponse
import stream.video.JoinCallRequest
import stream.video.JoinCallResponse
import stream.video.SelectEdgeServerRequest
import stream.video.SelectEdgeServerResponse

public interface CallCoordinatorClient {

    /**
     *
     */
    public suspend fun createCall(createCallRequest: CreateCallRequest): Result<CreateCallResponse>

    /**
     * Asks the server to join a call. This gives the user information which servers they can
     * choose from to fully join the call experience, based on latency.
     *
     * @param request The information used to prepare a call.
     * @return [JoinCallResponse] which helps us determine the correct connection.
     */
    public suspend fun joinCall(request: JoinCallRequest): Result<JoinCallResponse>

    /**
     * Asks the API for a correct edge server that can handle a connection for the given request.
     *
     * @param request The set of information used to find the server.
     * @return a [Result] wrapper of the [SelectEdgeServerResponse], based on the API response.
     */
    public suspend fun selectEdgeServer(request: SelectEdgeServerRequest): Result<SelectEdgeServerResponse>
}
