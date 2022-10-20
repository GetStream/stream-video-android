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
import stream.video.sfu.models.ICETrickle
import stream.video.sfu.signal.ICETrickleResponse
import stream.video.sfu.signal.SendAnswerRequest
import stream.video.sfu.signal.SendAnswerResponse
import stream.video.sfu.signal.SetPublisherRequest
import stream.video.sfu.signal.SetPublisherResponse
import stream.video.sfu.signal.UpdateSubscriptionsRequest
import stream.video.sfu.signal.UpdateSubscriptionsResponse

public interface LocalSignalService : SignalService {

    @Headers("Content-Type: application/protobuf")
    @POST("/twirp/stream.video.sfu.signal.SignalServer/SendAnswer")
    public override suspend fun sendAnswer(
        @Body answerRequest: SendAnswerRequest,
    ): SendAnswerResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/twirp/stream.video.sfu.signal.SignalServer/IceTrickle")
    public override suspend fun sendIceCandidate(@Body request: ICETrickle): ICETrickleResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/twirp/stream.video.sfu.signal.SignalServer/SetPublisher")
    public override suspend fun setPublisher(@Body request: SetPublisherRequest): SetPublisherResponse

    @Headers("Content-Type: application/protobuf")
    @POST("/twirp/stream.video.sfu.signal.SignalServer/UpdateSubscriptions")
    public override suspend fun updateSubscriptions(@Body request: UpdateSubscriptionsRequest): UpdateSubscriptionsResponse
}
