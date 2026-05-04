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

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamRetryPolicyTest {

    @Test
    fun `linear backoff computes increasing delays`() {
        val policy = StreamRetryPolicy.linear(
            backoffStepMillis = 100,
            maxBackoffMillis = 1000,
        )
        var delay = policy.initialDelayMillis
        val delays = mutableListOf<Long>()
        for (attempt in 1..5) {
            delay = policy.nextBackOffDelayFunction(attempt, delay)
                .coerceIn(policy.minBackoffMills, policy.maxBackoffMills)
            delays.add(delay)
        }
        assertEquals(listOf(100L, 200L, 300L, 400L, 500L), delays)
    }

    @Test
    fun `linear backoff caps at maxBackoffMillis`() {
        val policy = StreamRetryPolicy.linear(
            backoffStepMillis = 500,
            maxBackoffMillis = 1000,
        )
        var delay = 0L
        for (attempt in 1..5) {
            delay = policy.nextBackOffDelayFunction(attempt, delay)
                .coerceIn(policy.minBackoffMills, policy.maxBackoffMills)
        }
        assertEquals(1000L, delay)
    }

    @Test
    fun `fixed backoff returns constant delay`() {
        val policy = StreamRetryPolicy.fixed(delayMillis = 300, maxBackoffMillis = 5000)
        val delays = (1..5).map {
            policy.nextBackOffDelayFunction(it, 300)
                .coerceIn(policy.minBackoffMills, policy.maxBackoffMills)
        }
        assertEquals(listOf(300L, 300L, 300L, 300L, 300L), delays)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation rejects minRetries less than 1`() {
        StreamRetryPolicy.linear(minRetries = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation rejects maxRetries less than minRetries`() {
        StreamRetryPolicy.linear(minRetries = 5, maxRetries = 3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation rejects negative backoff`() {
        StreamRetryPolicy.linear(backoffStepMillis = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `validation rejects negative initialDelay`() {
        StreamRetryPolicy.linear(initialDelayMillis = -1)
    }
}
