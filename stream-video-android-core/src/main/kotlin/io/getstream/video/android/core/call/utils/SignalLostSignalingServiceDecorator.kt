/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.call.utils

import io.getstream.video.android.core.api.SignalServerService
import stream.video.sfu.models.Error
import stream.video.sfu.models.ErrorCode
import stream.video.sfu.models.ICETrickle
import stream.video.sfu.signal.ICERestartRequest
import stream.video.sfu.signal.ICERestartResponse
import stream.video.sfu.signal.ICETrickleResponse
import stream.video.sfu.signal.SendAnswerRequest
import stream.video.sfu.signal.SendAnswerResponse
import stream.video.sfu.signal.SendStatsRequest
import stream.video.sfu.signal.SendStatsResponse
import stream.video.sfu.signal.SetPublisherRequest
import stream.video.sfu.signal.SetPublisherResponse
import stream.video.sfu.signal.StartNoiseCancellationRequest
import stream.video.sfu.signal.StartNoiseCancellationResponse
import stream.video.sfu.signal.StopNoiseCancellationRequest
import stream.video.sfu.signal.StopNoiseCancellationResponse
import stream.video.sfu.signal.UpdateMuteStatesRequest
import stream.video.sfu.signal.UpdateMuteStatesResponse
import stream.video.sfu.signal.UpdateSubscriptionsRequest
import stream.video.sfu.signal.UpdateSubscriptionsResponse

internal class SignalLostSignalingServiceDecorator(private val decorated: SignalServerService, private val onPropagateError: (Error) -> Unit) : SignalServerService {

    override suspend fun setPublisher(setPublisherRequest: SetPublisherRequest): SetPublisherResponse {
        return decorated.setPublisher(setPublisherRequest).onError {
            if (error?.code == ErrorCode.ERROR_CODE_PARTICIPANT_SIGNAL_LOST) {
                onPropagateError(error)
            }
        }
    }

    override suspend fun sendAnswer(sendAnswerRequest: SendAnswerRequest): SendAnswerResponse {
        return decorated.sendAnswer(sendAnswerRequest).onError {
            if (error?.code == ErrorCode.ERROR_CODE_PARTICIPANT_SIGNAL_LOST) {
                onPropagateError(error)
            }
        }
    }

    override suspend fun iceTrickle(iceTrickle: ICETrickle): ICETrickleResponse {
        return decorated.iceTrickle(iceTrickle).onError {
            if (error?.code == ErrorCode.ERROR_CODE_PARTICIPANT_SIGNAL_LOST) {
                onPropagateError(error)
            }
        }
    }
    override suspend fun updateSubscriptions(
        updateSubscriptionsRequest: UpdateSubscriptionsRequest,
    ): UpdateSubscriptionsResponse {
        return decorated.updateSubscriptions(updateSubscriptionsRequest).onError {
            if (error?.code == ErrorCode.ERROR_CODE_PARTICIPANT_SIGNAL_LOST) {
                onPropagateError(error)
            }
        }
    }

    override suspend fun updateMuteStates(updateMuteStatesRequest: UpdateMuteStatesRequest): UpdateMuteStatesResponse {
        return decorated.updateMuteStates(updateMuteStatesRequest).onError {
            if (error?.code == ErrorCode.ERROR_CODE_PARTICIPANT_SIGNAL_LOST) {
                onPropagateError(error)
            }
        }
    }

    override suspend fun iceRestart(iceRestartRequest: ICERestartRequest): ICERestartResponse {
        return decorated.iceRestart(iceRestartRequest).onError {
            if (error?.code == ErrorCode.ERROR_CODE_PARTICIPANT_SIGNAL_LOST) {
                onPropagateError(error)
            }
        }
    }

    override suspend fun sendStats(sendStatsRequest: SendStatsRequest): SendStatsResponse {
        return decorated.sendStats(sendStatsRequest).onError {
            if (error?.code == ErrorCode.ERROR_CODE_PARTICIPANT_SIGNAL_LOST) {
                onPropagateError(error)
            }
        }
    }

    override suspend fun startNoiseCancellation(startNoiseCancellationRequest: StartNoiseCancellationRequest): StartNoiseCancellationResponse {
        return decorated.startNoiseCancellation(startNoiseCancellationRequest).onError {
            if (error?.code == ErrorCode.ERROR_CODE_PARTICIPANT_SIGNAL_LOST) {
                onPropagateError(error)
            }
        }
    }

    override suspend fun stopNoiseCancellation(stopNoiseCancellationRequest: StopNoiseCancellationRequest): StopNoiseCancellationResponse {
        return decorated.stopNoiseCancellation(stopNoiseCancellationRequest).onError {
            if (error?.code == ErrorCode.ERROR_CODE_PARTICIPANT_SIGNAL_LOST) {
                onPropagateError(error)
            }
        }
    }

    inline fun <reified T> T.onError(callback: T.() -> Unit): T {
        callback()
        return this
    }
}
