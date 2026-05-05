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

package io.getstream.video.android.core.retry

import io.getstream.log.StreamLog
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

/**
 * Strategy-based retry executor copied from stream-core-android.
 * Will be replaced by the core module dependency once integrated.
 *
 * Behaviour:
 * - Success → returns `Result.success(value)`.
 * - Give-up / max retries → returns `Result.failure(originalThrowable)`.
 * - Cancellation → re-throws `CancellationException`.
 */
internal interface StreamRetryProcessor {

    suspend fun <T> retry(
        policy: StreamRetryPolicy,
        block: suspend (attempt: StreamRetryAttemptInfo) -> T,
    ): Result<T>
}

internal fun StreamRetryProcessor(tag: String = "StreamRetryProcessor"): StreamRetryProcessor =
    StreamRetryProcessorImpl(tag)

private class StreamRetryProcessorImpl(private val tag: String) : StreamRetryProcessor {

    override suspend fun <T> retry(
        policy: StreamRetryPolicy,
        block: suspend (attempt: StreamRetryAttemptInfo) -> T,
    ): Result<T> = runCatchingCancellable {
        var delayMs = policy.initialDelayMillis
        var attempt = 1
        var previousError: Throwable? = null
        while (attempt <= policy.maxRetries) {
            if (delayMs > 0) {
                delay(delayMs)
            }
            try {
                return@runCatchingCancellable block(
                    StreamRetryAttemptInfo(attempt, delayMs, previousError, policy),
                )
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                StreamLog.v(tag) { "Retry attempt $attempt failed: ${t.message}" }
                val checkGiveUp = attempt >= policy.minRetries
                val shouldGiveUp = checkGiveUp && policy.giveUpFunction(attempt, t)
                val isLastAttempt = attempt == policy.maxRetries
                previousError = t
                if (shouldGiveUp || isLastAttempt) {
                    StreamLog.v(tag) {
                        "Retry attempt $attempt failed: ${t.message}. Giving up."
                    }
                    throw t
                }
                delayMs = policy
                    .nextBackOffDelayFunction(attempt, delayMs)
                    .coerceIn(policy.minBackoffMills, policy.maxBackoffMills)
            }
            attempt++
        }

        StreamLog.e(tag) {
            "Retry loop completed without success or failure. Policy: $policy"
        }
        throw IllegalStateException("Check your policy: $policy")
    }
}

@Suppress("TooGenericExceptionCaught")
private inline fun <T> runCatchingCancellable(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (ce: CancellationException) {
        throw ce
    } catch (t: Throwable) {
        Result.failure(t)
    }
}
