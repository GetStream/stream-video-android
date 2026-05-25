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
import stream.video.sfu.models.Error
import stream.video.sfu.models.ICETrickle
import stream.video.sfu.signal.ICERestartRequest
import stream.video.sfu.signal.ICERestartResponse
import stream.video.sfu.signal.ICETrickleResponse
import stream.video.sfu.signal.SendAnswerRequest
import stream.video.sfu.signal.SendAnswerResponse
import stream.video.sfu.signal.SendMetricsRequest
import stream.video.sfu.signal.SendMetricsResponse
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
import kotlin.coroutines.cancellation.CancellationException

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
 * Traces every SFU API call: the outgoing request, any SFU-level error in the
 * response, and any exception thrown by the underlying transport (network
 * timeout, connection reset, etc.). Placed directly around Retrofit so that
 * each retry attempt is individually visible in traces.
 */
internal class SignalingServiceTracerDecorator<T : SignalServerService>(
    private val tracer: Tracer,
    private val target: T,
) : SignalServerService {

    override suspend fun setPublisher(setPublisherRequest: SetPublisherRequest): SetPublisherResponse =
        traced("setPublisher", setPublisherRequest, { it.error }) {
            target.setPublisher(setPublisherRequest)
        }

    override suspend fun sendAnswer(sendAnswerRequest: SendAnswerRequest): SendAnswerResponse =
        traced("sendAnswer", sendAnswerRequest, { it.error }) {
            target.sendAnswer(sendAnswerRequest)
        }

    override suspend fun iceTrickle(iCETrickle: ICETrickle): ICETrickleResponse =
        traced("iceTrickle", iCETrickle, { it.error }) {
            target.iceTrickle(iCETrickle)
        }

    override suspend fun updateSubscriptions(updateSubscriptionsRequest: UpdateSubscriptionsRequest): UpdateSubscriptionsResponse =
        traced("updateSubscriptions", updateSubscriptionsRequest, { it.error }) {
            target.updateSubscriptions(updateSubscriptionsRequest)
        }

    override suspend fun updateMuteStates(updateMuteStatesRequest: UpdateMuteStatesRequest): UpdateMuteStatesResponse =
        traced("updateMuteStates", updateMuteStatesRequest, { it.error }) {
            target.updateMuteStates(updateMuteStatesRequest)
        }

    override suspend fun iceRestart(iCERestartRequest: ICERestartRequest): ICERestartResponse =
        traced("iceRestart", iCERestartRequest, { it.error }) {
            target.iceRestart(iCERestartRequest)
        }

    override suspend fun sendStats(sendStatsRequest: SendStatsRequest): SendStatsResponse =
        target.sendStats(sendStatsRequest)

    override suspend fun sendMetrics(sendMetricsRequest: SendMetricsRequest): SendMetricsResponse =
        target.sendMetrics(sendMetricsRequest)

    override suspend fun startNoiseCancellation(startNoiseCancellationRequest: StartNoiseCancellationRequest): StartNoiseCancellationResponse =
        traced("startNoiseCancellation", startNoiseCancellationRequest, { it.error }) {
            target.startNoiseCancellation(startNoiseCancellationRequest)
        }

    override suspend fun stopNoiseCancellation(stopNoiseCancellationRequest: StopNoiseCancellationRequest): StopNoiseCancellationResponse =
        traced("stopNoiseCancellation", stopNoiseCancellationRequest, { it.error }) {
            target.stopNoiseCancellation(stopNoiseCancellationRequest)
        }

    private suspend inline fun <R, T> traced(
        name: String,
        request: T,
        crossinline errorExtractor: (R) -> Error?,
        crossinline block: suspend () -> R,
    ): R {
        tracer.trace(name, request.toString())
        try {
            val response = block()
            errorExtractor(response)?.let {
                tracer.trace("$name-error", it.toString())
            }
            return response
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            tracer.trace("$name-exception", e.message ?: e::class.simpleName ?: "unknown")
            throw e
        }
    }
}
