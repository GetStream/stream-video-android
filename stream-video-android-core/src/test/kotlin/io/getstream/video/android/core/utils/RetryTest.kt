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

package io.getstream.video.android.core.utils

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.test.Test

class RetryTest {
    @Test
    fun `test job is executed and updated correctly`() = runTest {
        val scope = CoroutineScope(Dispatchers.Default)
        var job: Job? = null
        var jobExecuted = false

        waitForJob(scope, job) {
            update { newJob -> job = newJob }
            execute {
                jobExecuted = true
            }
        }

        job?.join()
        assertTrue(jobExecuted)
    }

    @Test
    fun `test previous job is finished if exists`() = runTest {
        val scope = CoroutineScope(Dispatchers.Default)
        var job: Job? = null
        var previousJobCanceled = false
        var jobFinished = false

        // Start the first job
        waitForJob(scope, job) {
            update { newJob -> job = newJob }
            execute {
                delay(500L)
                jobFinished = true
            }
        }

        // Start the second job
        waitForJob(scope, job) {
            update { newJob -> job = newJob }
            execute {
                previousJobCanceled = true
            }
        }

        job?.join()
        assertTrue(jobFinished)
        assertTrue(previousJobCanceled)
    }

    @Test
    fun `test job completes within timeout`() = runTest {
        val scope = CoroutineScope(Dispatchers.Default)
        var job: Job? = null
        var jobCompleted = false

        waitForJob(scope, job, timeout = 1000L) {
            update { newJob -> job = newJob }
            execute {
                delay(500L) // Job completes within the timeout
                jobCompleted = true
            }
        }

        job?.join()
        assertTrue(jobCompleted)
    }

    @Test
    fun `test job is canceled if it exceeds timeout`() = runTest {
        val scope = CoroutineScope(Dispatchers.Default)
        val job: Job = mockk(relaxed = true) {
            coEvery { join() } coAnswers {
                delay(2000L) // Job exceeds the timeout
            }
        }
        var updatedJob: Job? = null

        waitForJob(scope, job, timeout = 500L) {
            update { newJob ->
                // Do nothing
                updatedJob = newJob
            }
            execute {
                // Do whatever
                delay(1000L)
            }
        }

        updatedJob?.join()
        coVerify { job.join() }
        coVerify { job.cancel() }
    }

    @Test
    fun `test retry logic is executed correctly`() = runTest {
        var attempt = 0
        val maxAttempts = 3

        retry(maxAttempts) {
            attempt++
            if (attempt < maxAttempts) {
                throw Exception("Retrying...")
            }
        }

        assertEquals(maxAttempts, attempt)
    }

    @Test
    fun `test retry logic stops after max attempts`() = runTest {
        var attempt = 0
        val maxAttempts = 3

        try {
            retry(maxAttempts) {
                attempt++
                throw Exception("Retrying...")
            }
        } catch (e: Exception) {
            // Expected exception after max attempts
        }

        assertEquals(maxAttempts, attempt)
    }
}
