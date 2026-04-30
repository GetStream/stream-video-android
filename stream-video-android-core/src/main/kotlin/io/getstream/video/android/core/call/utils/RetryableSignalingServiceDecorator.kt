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
 * Thrown when the SFU reports a session-fatal error (PARTICIPANT_NOT_FOUND,
 * SIGNAL_LOST, etc.). Reconnection has already been initiated via
 * [RetryableSignalingServiceDecorator.onSessionError] — callers should
 * **not** retry the failed request.
 */
internal class SessionFatalException(val sfuError: Error) : Exception(sfuError.message)

/**
 * Decorator that retries SFU API calls on transient failures and propagates
 * **session-fatal** errors to [onSessionError] for reconnection handling.
 *
 * **Retry behaviour** (via [StreamRetryProcessor]):
 * - Network errors (IOException, timeout) are retried automatically.
 * - SFU responses with `should_retry = true` are retried (except session-fatal
 *   codes like SIGNAL_LOST which need a WebSocket reconnect, not an HTTP retry).
 * - Retries stop on success, terminal error (`should_retry = false`),
 *   max attempts exhausted, or coroutine cancellation.
 *
 * **Error propagation** — only errors that indicate the SFU session is broken
 * trigger reconnection. Regular API errors (validation, permission, etc.)
 * are returned to the caller without triggering a reconnect:
 *
 * - Session-fatal SFU errors (SIGNAL_LOST, PARTICIPANT_NOT_FOUND, etc.)
 *   → [onSessionError] (triggers fast reconnect or rejoin).
 * - Network errors after all retries exhausted → rethrown to the caller.
 *   No reconnect is triggered because the WebSocket has its own health
 *   monitoring (HealthMonitor / stateJob) and will detect connectivity
 *   problems independently.
 * - All other SFU errors → returned to the caller as-is, no reconnect.
 */
internal class RetryableSignalingServiceDecorator(
    private val decorated: SignalServerService,
    private val onSessionError: (Error) -> Unit = {},
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
     * Four possible outcomes after the retry loop finishes:
     *
     * 1. **Success** — response has no error. Returned directly.
     *
     * 2. **Non-fatal SFU error** — the response carries an [Error] that is
     *    not session-fatal. Returned to the caller as-is. If retries were
     *    exhausted (`should_retry` errors), the last error response is
     *    returned rather than throwing.
     *
     * 3. **Session-fatal SFU error** — fires [onSessionError] to trigger
     *    reconnection, then throws [SessionFatalException]. Callers must
     *    not retry — the session is being rebuilt.
     *
     * 4. **Network failure** — the HTTP call itself threw (IOException, timeout)
     *    on every retry and no response was ever received. Rethrown to the
     *    caller. No reconnect — the WebSocket has its own health monitoring.
     *
     * The retry loop retries both network exceptions (thrown naturally) and
     * SFU errors with `should_retry = true` (converted to [SfuRetryableException]).
     * Session-fatal errors are never retried because they require a WebSocket
     * reconnect or rejoin, not another HTTP attempt.
     */
    private suspend inline fun <T> retryCall(
        crossinline errorExtractor: (T) -> Error?,
        crossinline block: suspend () -> T,
    ): T {
        var lastResponse: T? = null

        val response = retryProcessor.retry(policy) {
            val response = block()
            lastResponse = response
            val error = errorExtractor(response)
            if (error != null && error.should_retry && !error.isSessionFatal()) {
                throw SfuRetryableException(error)
            }
            response
        }.getOrElse { throwable ->
            // SFU retries exhausted → return the last error response so callers
            // handle it normally (via sfuCall → RtcException). No exception thrown.
            // Network failures (IOException) still propagate — lastResponse is null
            // because the HTTP call itself never completed.
            lastResponse ?: throw throwable
        }

        val sfuError = errorExtractor(response)
        if (sfuError != null && sfuError.isSessionFatal()) {
            onSessionError(sfuError)
            throw SessionFatalException(sfuError)
        }

        return response
    }

    /**
     * Returns true for error codes that indicate the SFU session is broken
     * and cannot recover with a simple HTTP retry. These need a WebSocket
     * reconnect (SIGNAL_LOST) or a full rejoin (PARTICIPANT_NOT_FOUND, etc.).
     *
     * All other errors (validation, permission, rate-limit, etc.) are
     * regular API failures that don't affect the underlying session.
     */
    private fun Error.isSessionFatal(): Boolean = when (code) {
        ErrorCode.ERROR_CODE_PARTICIPANT_NOT_FOUND,
        ErrorCode.ERROR_CODE_PARTICIPANT_SIGNAL_LOST,
        ErrorCode.ERROR_CODE_PARTICIPANT_MEDIA_TRANSPORT_FAILURE,
        ErrorCode.ERROR_CODE_PARTICIPANT_RECONNECT_FAILED,
        ErrorCode.ERROR_CODE_CALL_NOT_FOUND,
        -> true
        else -> false
    }
}
