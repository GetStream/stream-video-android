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

package io.getstream.video.android.webrtc.signal

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import stream.video.sfu.IceCandidateRequest
import stream.video.sfu.IceCandidateResponse
import stream.video.sfu.JoinRequest
import stream.video.sfu.JoinResponse
import stream.video.sfu.SendAnswerRequest
import stream.video.sfu.SendAnswerResponse
import stream.video.sfu.SetPublisherRequest
import stream.video.sfu.SetPublisherResponse
import stream.video.sfu.UpdateSubscriptionsRequest
import stream.video.sfu.UpdateSubscriptionsResponse

public interface SignalService {

    /**
     * TODO - when local remove /rpc"
     */
    @Headers("Content-Type: application/protobuf")
    @POST("/twirp/stream.video.sfu.SignalServer/SendAnswer")
    public suspend fun sendAnswer(
        @Body answerRequest: SendAnswerRequest,
    ): SendAnswerResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/twirp/stream.video.sfu.SignalServer/SendIceCandidate")
    public suspend fun sendIceCandidate(@Body request: IceCandidateRequest): IceCandidateResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/twirp/stream.video.sfu.SignalServer/Join")
    public suspend fun join(@Body joinRequest: JoinRequest): JoinResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/twirp/stream.video.sfu.SignalServer/SetPublisher")
    public suspend fun setPublisher(@Body request: SetPublisherRequest): SetPublisherResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/twirp/stream.video.sfu.SignalServer/UpdateSubscriptions")
    public suspend fun updateSubscriptions(@Body request: UpdateSubscriptionsRequest): UpdateSubscriptionsResponse
}
