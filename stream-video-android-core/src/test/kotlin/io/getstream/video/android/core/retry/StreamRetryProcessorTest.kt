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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class StreamRetryProcessorTest {

    private val processor = StreamRetryProcessor("test")

    private val fixedPolicy = StreamRetryPolicy.fixed(
        minRetries = 1,
        maxRetries = 3,
        delayMillis = 0,
        initialDelayMillis = 0,
    )

    @Test
    fun `succeeds on first attempt`() = runTest {
        val result = processor.retry(fixedPolicy) { "ok" }
        assertTrue(result.isSuccess)
        assertEquals("ok", result.getOrNull())
    }

    @Test
    fun `retries and succeeds on second attempt`() = runTest {
        var attempts = 0
        val result = processor.retry(fixedPolicy) {
            attempts++
            if (attempts < 2) throw IOException("fail")
            "recovered"
        }
        assertTrue(result.isSuccess)
        assertEquals("recovered", result.getOrNull())
        assertEquals(2, attempts)
    }

    @Test
    fun `gives up after maxRetries`() = runTest {
        var attempts = 0
        val result = processor.retry(fixedPolicy) {
            attempts++
            throw IOException("always fails")
        }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(3, attempts)
    }

    @Test
    fun `respects minRetries before consulting giveUp`() = runTest {
        val policy = StreamRetryPolicy.fixed(
            minRetries = 3,
            maxRetries = 5,
            delayMillis = 0,
            initialDelayMillis = 0,
            giveUp = { _, _ -> true },
        )
        var attempts = 0
        val result = processor.retry(policy) {
            attempts++
            throw IOException("fail")
        }
        assertTrue(result.isFailure)
        assertEquals(3, attempts)
    }

    @Test
    fun `giveUp function stops retrying early`() = runTest {
        val policy = StreamRetryPolicy.fixed(
            minRetries = 1,
            maxRetries = 10,
            delayMillis = 0,
            initialDelayMillis = 0,
            giveUp = { attempt, _ -> attempt >= 2 },
        )
        var attempts = 0
        val result = processor.retry(policy) {
            attempts++
            throw IOException("fail")
        }
        assertTrue(result.isFailure)
        assertEquals(2, attempts)
    }

    @Test(expected = CancellationException::class)
    fun `CancellationException propagates immediately`() = runTest {
        processor.retry(fixedPolicy) {
            throw CancellationException("cancelled")
        }
    }

    @Test
    fun `attempt info carries previous error`() = runTest {
        val errors = mutableListOf<Throwable?>()
        val policy = StreamRetryPolicy.fixed(
            minRetries = 1,
            maxRetries = 3,
            delayMillis = 0,
            initialDelayMillis = 0,
        )
        processor.retry(policy) { info ->
            errors.add(info.previousAttemptError)
            if (info.attempt < 3) throw IOException("fail #${info.attempt}")
            "done"
        }
        assertEquals(3, errors.size)
        assertTrue(errors[0] == null)
        assertTrue(errors[1] is IOException)
        assertEquals("fail #1", errors[1]?.message)
        assertTrue(errors[2] is IOException)
        assertEquals("fail #2", errors[2]?.message)
    }
}
