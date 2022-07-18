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

package io.getstream.video.android.api

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import stream.video.JoinCallRequest
import stream.video.JoinCallResponse
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
    @POST("/stream.video.CallCoordinatorService/JoinCall")
    public suspend fun joinCall(@Body joinCallRequest: JoinCallRequest): JoinCallResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/stream.video.CallCoordinatorService/SelectEdgeServer")
    public suspend fun selectEdgeServer(@Body selectEdgeServerRequest: SelectEdgeServerRequest): SelectEdgeServerResponse
}
