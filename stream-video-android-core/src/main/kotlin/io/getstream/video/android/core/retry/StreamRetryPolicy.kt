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

/**
 * Retry-policy value object copied from stream-core-android's `StreamRetryPolicy`.
 * Will be replaced by the core module dependency once integrated.
 *
 * A policy answers two questions for each failed attempt:
 * 1. **Should we try again?** — via [giveUpFunction].
 * 2. **How long should we wait?** — via [nextBackOffDelayFunction].
 *
 * @param minRetries Minimum number of retry attempts before [giveUpFunction] is consulted.
 * @param maxRetries Upper bound for attempts.
 * @param minBackoffMills Smallest delay the policy may return (ms).
 * @param maxBackoffMills Largest delay the policy may return (ms).
 * @param initialDelayMillis Delay before the first retry.
 * @param giveUpFunction Receives the 1-based retry index and the last Throwable; return
 *   true to stop retrying.
 * @param nextBackOffDelayFunction Computes the delay for the upcoming retry.
 */
internal data class StreamRetryPolicy
internal constructor(
    val minRetries: Int,
    val maxRetries: Int,
    val minBackoffMills: Long,
    val maxBackoffMills: Long,
    val initialDelayMillis: Long,
    val giveUpFunction: (retry: Int, cause: Throwable) -> Boolean,
    val nextBackOffDelayFunction: (retry: Int, previousDelay: Long) -> Long,
) {
    companion object {
        fun exponential(
            minRetries: Int = 1,
            maxRetries: Int = 5,
            backoffStepMillis: Long = 250,
            maxBackoffMillis: Long = 15_000,
            initialDelayMillis: Long = 0,
            giveUp: (Int, Throwable) -> Boolean = { retry, _ -> retry > maxRetries },
        ): StreamRetryPolicy =
            StreamRetryPolicy(
                minRetries = minRetries,
                maxRetries = maxRetries,
                minBackoffMills = backoffStepMillis,
                maxBackoffMills = maxBackoffMillis,
                initialDelayMillis = initialDelayMillis,
                giveUpFunction = giveUp,
                nextBackOffDelayFunction = { retry, prev ->
                    (prev + retry * backoffStepMillis)
                        .coerceAtMost(maxBackoffMillis)
                        .coerceIn(backoffStepMillis, maxBackoffMillis)
                },
            ).validate()

        fun linear(
            minRetries: Int = 1,
            maxRetries: Int = 5,
            backoffStepMillis: Long = 250,
            maxBackoffMillis: Long = 15_000,
            initialDelayMillis: Long = 0,
            giveUp: (Int, Throwable) -> Boolean = { retry, _ -> retry > maxRetries },
        ): StreamRetryPolicy =
            StreamRetryPolicy(
                minRetries = minRetries,
                maxRetries = maxRetries,
                minBackoffMills = backoffStepMillis,
                maxBackoffMills = maxBackoffMillis,
                initialDelayMillis = initialDelayMillis,
                giveUpFunction = giveUp,
                nextBackOffDelayFunction = { _, prev ->
                    (prev + backoffStepMillis).coerceAtMost(maxBackoffMillis)
                },
            ).validate()

        fun fixed(
            minRetries: Int = 1,
            maxRetries: Int = 5,
            delayMillis: Long = 500,
            maxBackoffMillis: Long = 15_000,
            initialDelayMillis: Long = 0,
            giveUp: (Int, Throwable) -> Boolean = { retry, _ -> retry > maxRetries },
        ): StreamRetryPolicy =
            StreamRetryPolicy(
                minRetries = minRetries,
                maxRetries = maxRetries,
                minBackoffMills = delayMillis,
                maxBackoffMills = maxBackoffMillis,
                initialDelayMillis = initialDelayMillis,
                giveUpFunction = giveUp,
                nextBackOffDelayFunction = { _, _ ->
                    delayMillis.coerceAtMost(maxBackoffMillis)
                },
            ).validate()

        private fun StreamRetryPolicy.validate(): StreamRetryPolicy {
            require(minRetries >= 1) { "minRetries must be >= 1" }
            require(maxRetries >= minRetries) { "maxRetries must be >= minRetries" }
            require(minBackoffMills >= 0) { "minBackoffMills must be >= 0" }
            require(maxBackoffMills >= minBackoffMills) {
                "maxBackoffMills must be >= minBackoffMills"
            }
            require(initialDelayMillis >= 0) { "initialDelayMillis must be >= 0" }
            return this
        }
    }
}
