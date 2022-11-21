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

package io.getstream.video.android.call.signal

import io.getstream.video.android.utils.Result
import stream.video.sfu.models.ICETrickle
import stream.video.sfu.signal.ICETrickleResponse
import stream.video.sfu.signal.SendAnswerRequest
import stream.video.sfu.signal.SendAnswerResponse
import stream.video.sfu.signal.SetPublisherRequest
import stream.video.sfu.signal.SetPublisherResponse
import stream.video.sfu.signal.UpdateMuteStateRequest
import stream.video.sfu.signal.UpdateMuteStateResponse
import stream.video.sfu.signal.UpdateSubscriptionsRequest
import stream.video.sfu.signal.UpdateSubscriptionsResponse

// TODO
// We need to think about renaming it to SfuClient for the sake of transparency for other devs
// , since this client works with `video-sfu`.
// In addition all generated proto files have `sfu` in their package name.
// Otherwise 2 names for the same thing may confuse those who are not familiar with the project.
public interface SignalClient {

    public suspend fun sendAnswer(request: SendAnswerRequest): Result<SendAnswerResponse>

    public suspend fun sendIceCandidate(request: ICETrickle): Result<ICETrickleResponse>

    public suspend fun setPublisher(request: SetPublisherRequest): Result<SetPublisherResponse>

    public suspend fun updateSubscriptions(request: UpdateSubscriptionsRequest): Result<UpdateSubscriptionsResponse>

    public suspend fun updateMuteState(muteStateRequest: UpdateMuteStateRequest): Result<UpdateMuteStateResponse>
}
