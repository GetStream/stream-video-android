/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.trace

import io.getstream.video.android.core.api.SignalServerService
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

/**
 * Creates a decorator for the given target that traces all method calls.
 *
 * @param target The target object to proxy.
 * @param tracer The tracer to use.
 * @return A decorated target.
 */
internal inline fun <reified T : SignalServerService> tracedWith(
    target: T,
    tracer: Tracer,
): SignalServerService {
    val handler = SignalingServiceTracerDecorator(tracer, target)
    return handler
}

/**
 * Count the invocations of the methods of the target object.
 *
 * @param scope The scope in which the counter should run.
 * @param target The target object to count the invocations of.
 * @param config The configuration for the counter.
 */
internal class SignalingServiceTracerDecorator<T : SignalServerService>(
    private val tracer: Tracer,
    private val target: T,
) : SignalServerService {
    override suspend fun setPublisher(
        setPublisherRequest: SetPublisherRequest,
    ): SetPublisherResponse {
        tracer.trace("setPublisher", setPublisherRequest.toString())
        val response = target.setPublisher(setPublisherRequest)
        if (response.error != null) {
            tracer.trace("setPublisher-error", response.error.toString() ?: "unknown")
        }
        return response
    }

    override suspend fun sendAnswer(sendAnswerRequest: SendAnswerRequest): SendAnswerResponse {
        tracer.trace("sendAnswer", sendAnswerRequest.toString())
        val response = target.sendAnswer(sendAnswerRequest)
        if (response.error != null) {
            tracer.trace("sendAnswer-error", response.error.toString() ?: "unknown")
        }
        return response
    }

    override suspend fun iceTrickle(iCETrickle: ICETrickle): ICETrickleResponse {
        tracer.trace("iceTrickle", iCETrickle.toString())
        val response = target.iceTrickle(iCETrickle)
        if (response.error != null) {
            tracer.trace("iceTrickle-error", response.error.toString() ?: "unknown")
        }
        return response
    }

    override suspend fun updateSubscriptions(updateSubscriptionsRequest: UpdateSubscriptionsRequest): UpdateSubscriptionsResponse {
        tracer.trace("updateSubscriptions", updateSubscriptionsRequest.toString())
        val response = target.updateSubscriptions(updateSubscriptionsRequest)
        if (response.error != null) {
            tracer.trace("updateSubscriptions-error", response.error.toString() ?: "unknown")
        }
        return response
    }

    override suspend fun updateMuteStates(updateMuteStatesRequest: UpdateMuteStatesRequest): UpdateMuteStatesResponse {
        tracer.trace("updateMuteStates", updateMuteStatesRequest.toString())
        val response = target.updateMuteStates(updateMuteStatesRequest)
        if (response.error != null) {
            tracer.trace("updateMuteStates-error", response.error.toString() ?: "unknown")
        }
        return response
    }

    override suspend fun iceRestart(iCERestartRequest: ICERestartRequest): ICERestartResponse {
        tracer.trace("iceRestart", iCERestartRequest.toString())
        val response = target.iceRestart(iCERestartRequest)
        if (response.error != null) {
            tracer.trace("iceRestart-error", response.error.toString() ?: "unknown")
        }
        return response
    }

    override suspend fun sendStats(sendStatsRequest: SendStatsRequest): SendStatsResponse {
        // Not traced
        return target.sendStats(sendStatsRequest)
    }

    override suspend fun startNoiseCancellation(startNoiseCancellationRequest: StartNoiseCancellationRequest): StartNoiseCancellationResponse {
        tracer.trace("startNoiseCancellation", startNoiseCancellationRequest.toString())
        val response = target.startNoiseCancellation(startNoiseCancellationRequest)
        if (response.error != null) {
            tracer.trace("startNoiseCancellation-error", response.error.toString() ?: "unknown")
        }
        return response
    }

    override suspend fun stopNoiseCancellation(stopNoiseCancellationRequest: StopNoiseCancellationRequest): StopNoiseCancellationResponse {
        tracer.trace("stopNoiseCancellation", stopNoiseCancellationRequest.toString())
        val response = target.stopNoiseCancellation(stopNoiseCancellationRequest)
        if (response.error != null) {
            tracer.trace("stopNoiseCancellation-error", response.error.toString() ?: "unknown")
        }
        return response
    }
}
