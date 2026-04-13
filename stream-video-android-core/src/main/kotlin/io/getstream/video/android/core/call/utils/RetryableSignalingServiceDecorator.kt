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

package io.getstream.video.android.core.call.utils

import io.getstream.video.android.core.api.SignalServerService
import io.getstream.video.android.core.retry.StreamRetryPolicy
import io.getstream.video.android.core.retry.StreamRetryProcessor
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

/**
 * SFU response error wrapper that lets the retry loop inspect `should_retry`
 * without knowing the concrete response type.
 */
internal class SfuRetryableException(val sfuError: Error) : Exception(sfuError.message)

/**
 * Decorator that retries SFU API calls on transient failures and propagates
 * errors to callbacks for reconnection handling.
 *
 * **Retry behaviour** (via [StreamRetryProcessor]):
 * - Network errors (IOException, timeout) are retried automatically.
 * - SFU responses with `should_retry = true` are retried (except SIGNAL_LOST,
 *   which needs a WebSocket reconnect, not an HTTP retry).
 * - Retries stop on success, terminal error (`should_retry = false`),
 *   max attempts exhausted, or coroutine cancellation.
 *
 * **Error propagation**:
 * - SFU response errors → [onTerminalError] (maps to fastReconnect / rejoin).
 * - Network errors after all retries exhausted → [onNetworkFailure] (triggers
 *   reconnect since the SFU is unreachable via HTTP).
 */
internal class RetryableSignalingServiceDecorator(
    private val decorated: SignalServerService,
    private val onTerminalError: (Error) -> Unit = {},
    private val onNetworkFailure: (Throwable) -> Unit = {},
    private val retryProcessor: StreamRetryProcessor = StreamRetryProcessor("SfuRetry"),
    private val policy: StreamRetryPolicy = StreamRetryPolicy.linear(
        minRetries = 1,
        maxRetries = 3,
        backoffStepMillis = 250,
        maxBackoffMillis = 2_000,
        initialDelayMillis = 0,
    ),
) : SignalServerService {

    override suspend fun setPublisher(setPublisherRequest: SetPublisherRequest): SetPublisherResponse =
        retryCall({ it.error }) { decorated.setPublisher(setPublisherRequest) }

    override suspend fun sendAnswer(sendAnswerRequest: SendAnswerRequest): SendAnswerResponse =
        retryCall({ it.error }) { decorated.sendAnswer(sendAnswerRequest) }

    override suspend fun iceTrickle(iceTrickle: ICETrickle): ICETrickleResponse =
        retryCall({ it.error }) { decorated.iceTrickle(iceTrickle) }

    override suspend fun updateSubscriptions(
        updateSubscriptionsRequest: UpdateSubscriptionsRequest,
    ): UpdateSubscriptionsResponse =
        retryCall({ it.error }) { decorated.updateSubscriptions(updateSubscriptionsRequest) }

    override suspend fun updateMuteStates(updateMuteStatesRequest: UpdateMuteStatesRequest): UpdateMuteStatesResponse =
        retryCall({ it.error }) { decorated.updateMuteStates(updateMuteStatesRequest) }

    override suspend fun iceRestart(iceRestartRequest: ICERestartRequest): ICERestartResponse =
        retryCall({ it.error }) { decorated.iceRestart(iceRestartRequest) }

    override suspend fun sendStats(sendStatsRequest: SendStatsRequest): SendStatsResponse =
        retryCall({ it.error }) { decorated.sendStats(sendStatsRequest) }

    override suspend fun startNoiseCancellation(startNoiseCancellationRequest: StartNoiseCancellationRequest): StartNoiseCancellationResponse =
        retryCall({ it.error }) { decorated.startNoiseCancellation(startNoiseCancellationRequest) }

    override suspend fun stopNoiseCancellation(stopNoiseCancellationRequest: StopNoiseCancellationRequest): StopNoiseCancellationResponse =
        retryCall({ it.error }) { decorated.stopNoiseCancellation(stopNoiseCancellationRequest) }

    /**
     * Wraps an SFU API call with retry + error propagation.
     *
     * Network errors throw naturally and are retried by the processor.
     * SFU-level `should_retry=true` errors are converted to [SfuRetryableException]
     * to re-enter the retry loop (except SIGNAL_LOST which requires WS reconnect).
     *
     * After the final outcome:
     * - SFU response with error → [onTerminalError]
     * - Network failure after all retries → [onNetworkFailure], then rethrow
     */
    private suspend inline fun <T> retryCall(
        crossinline errorExtractor: (T) -> Error?,
        crossinline block: suspend () -> T,
    ): T {
        var lastResponse: T? = null

        val result = retryProcessor.retry(policy) {
            val response = block()
            lastResponse = response
            val error = errorExtractor(response)
            if (error != null && error.should_retry && !error.requiresReconnect()) {
                throw SfuRetryableException(error)
            }
            response
        }

        val response = result.getOrElse { throwable ->
            if (throwable is SfuRetryableException && lastResponse != null) {
                @Suppress("UNCHECKED_CAST")
                lastResponse as T
            } else {
                onNetworkFailure(throwable)
                throw throwable
            }
        }

        errorExtractor(response)?.let { onTerminalError(it) }

        return response
    }

    private fun Error.requiresReconnect(): Boolean =
        code == ErrorCode.ERROR_CODE_PARTICIPANT_SIGNAL_LOST
}
