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
import retrofit2.http.Query
import stream.video.coordinator.client_v1_rpc.CreateCallRequest
import stream.video.coordinator.client_v1_rpc.CreateCallResponse
import stream.video.coordinator.client_v1_rpc.GetCallEdgeServerRequest
import stream.video.coordinator.client_v1_rpc.GetCallEdgeServerResponse
import stream.video.coordinator.client_v1_rpc.JoinCallRequest
import stream.video.coordinator.client_v1_rpc.JoinCallResponse
import stream.video.coordinator.client_v1_rpc.SendCustomEventRequest
import stream.video.coordinator.client_v1_rpc.SendEventRequest

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
    @POST("/rpc/stream.video.coordinator.client_v1_rpc.ClientRPC/CreateCall")
    public suspend fun createCall(
        @Body createCallRequest: CreateCallRequest,
        @Query(QUERY_API_KEY) apiKey: String
    ): CreateCallResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/rpc/stream.video.coordinator.client_v1_rpc.ClientRPC/GetOrCreateCall")
    public suspend fun getOrCreateCall(
        @Body createCallRequest: CreateCallRequest,
        @Query(QUERY_API_KEY) apiKey: String
    ): CreateCallResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/rpc/stream.video.coordinator.client_v1_rpc.ClientRPC/JoinCall")
    public suspend fun joinCall(
        @Body joinCallRequest: JoinCallRequest,
        @Query(QUERY_API_KEY) apiKey: String
    ): JoinCallResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/rpc/stream.video.coordinator.client_v1_rpc.ClientRPC/GetCallEdgeServer")
    public suspend fun getCallEdgeServer(
        @Body selectEdgeServerRequest: GetCallEdgeServerRequest,
        @Query(QUERY_API_KEY) apiKey: String
    ): GetCallEdgeServerResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/rpc/stream.video.coordinator.client_v1_rpc.ClientRPC/SendEvent")
    public suspend fun sendUserEvent(
        @Body sendEventRequest: SendEventRequest,
        @Query(QUERY_API_KEY) apiKey: String
    ): SendCustomEventRequest
}

/**
 * API key query we need to send for all API calls.
 */
private const val QUERY_API_KEY = "api_key"
