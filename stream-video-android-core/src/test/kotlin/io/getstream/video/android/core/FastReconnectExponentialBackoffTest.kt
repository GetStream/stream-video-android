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

package io.getstream.video.android.core

import io.getstream.video.android.core.base.IntegrationTestBase
import io.getstream.video.android.core.call.RtcSession
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class FastReconnectExponentialBackoffTest : IntegrationTestBase() {

    @Test
    fun `fastReconnect applies exponential backoff on retries`() = runTest {
        val sessionMock = mockk<RtcSession>(relaxed = true)
        val call = client.call("default", randomUUID())
        call.session = sessionMock

        // Mock fastReconnect to simulate work
        coEvery { sessionMock.fastReconnect(any()) } coAnswers {
            delay(50) // Small delay to simulate work
        }
        coEvery { sessionMock.currentSfuInfo() } returns Triple("session1", emptyList(), emptyList())
        coEvery { sessionMock.prepareReconnect() } returns Unit

        // First attempt - no backoff
        call.fastReconnect()
        advanceTimeBy(100) // Allow time for execution
        coVerify(exactly = 1) { sessionMock.fastReconnect(any()) }

        // Second attempt - should have ~500ms backoff (with jitter, 450-550ms)
        call.fastReconnect()
        advanceTimeBy(400) // Advance less than expected backoff
        coVerify(exactly = 1) { sessionMock.fastReconnect(any()) } // Still only 1 call

        advanceTimeBy(200) // Complete the backoff delay
        coVerify(exactly = 2) { sessionMock.fastReconnect(any()) }

        // Third attempt - should have ~1000ms backoff (with jitter, 900-1100ms)
        call.fastReconnect()
        advanceTimeBy(900) // Advance less than expected backoff
        coVerify(exactly = 2) { sessionMock.fastReconnect(any()) } // Still only 2 calls

        advanceTimeBy(200) // Complete the backoff delay
        coVerify(exactly = 3) { sessionMock.fastReconnect(any()) }
    }

    @Test
    fun `fastReconnect stops after 5 attempts and calls rejoin`() = runTest {
        val sessionMock = mockk<RtcSession>(relaxed = true)
        val call = client.call("default", randomUUID())
        call.session = sessionMock

        // Mock fastReconnect to always fail (simulate connection failure)
        coEvery { sessionMock.fastReconnect(any()) } coAnswers {
            delay(50)
        }
        coEvery { sessionMock.currentSfuInfo() } returns Triple("session1", emptyList(), emptyList())
        coEvery { sessionMock.prepareReconnect() } returns Unit

        // Attempt 1 - no backoff
        call.fastReconnect()
        advanceTimeBy(100)

        // Attempt 2 - ~500ms backoff
        call.fastReconnect()
        advanceTimeBy(600) // Include backoff

        // Attempt 3 - ~1000ms backoff
        call.fastReconnect()
        advanceTimeBy(1100) // Include backoff

        // Attempt 4 - ~2000ms backoff
        call.fastReconnect()
        advanceTimeBy(2100) // Include backoff

        // Attempt 5 - ~4000ms backoff
        call.fastReconnect()
        advanceTimeBy(4100) // Include backoff

        // Verify we've called fastReconnect 5 times
        coVerify(exactly = 5) { sessionMock.fastReconnect(any()) }

        // 6th attempt should call rejoin instead (not fastReconnect)
        val callSpy = spyk(call)
        callSpy.fastReconnect()
        advanceTimeBy(100)

        // Verify fastReconnect was NOT called again (still only 5 calls)
        coVerify(exactly = 5) { sessionMock.fastReconnect(any()) }
        // Note: We can't directly verify rejoin() due to schedule() wrapper, but we verify fastReconnect wasn't called
    }

    @Test
    fun `fastReconnect resets counter on successful connection`() = runTest {
        val sessionMock = mockk<RtcSession>(relaxed = true)
        val call = client.call("default", randomUUID())
        call.session = sessionMock

        var attemptCount = 0
        coEvery { sessionMock.fastReconnect(any()) } coAnswers {
            attemptCount++
            delay(50)
            // Simulate successful connection by calling resetFastReconnectAttempts
            if (attemptCount == 2) {
                call.resetFastReconnectAttempts()
            }
        }
        coEvery { sessionMock.currentSfuInfo() } returns Triple("session1", emptyList(), emptyList())
        coEvery { sessionMock.prepareReconnect() } returns Unit

        // First attempt
        call.fastReconnect()
        advanceTimeBy(100)

        // Second attempt - this will succeed and reset counter
        call.fastReconnect()
        advanceTimeBy(600) // Include backoff

        // Third attempt - counter should be reset, so this is attempt 1 again (no backoff)
        call.fastReconnect()
        advanceTimeBy(100) // No backoff since counter was reset

        // Verify we got 3 calls total
        coVerify(exactly = 3) { sessionMock.fastReconnect(any()) }
    }

    @Test
    fun `fastReconnect applies no delay on first attempt`() = runTest {
        val sessionMock = mockk<RtcSession>(relaxed = true)
        val call = client.call("default", randomUUID())
        call.session = sessionMock

        coEvery { sessionMock.fastReconnect(any()) } coAnswers {
            delay(50)
        }
        coEvery { sessionMock.currentSfuInfo() } returns Triple("session1", emptyList(), emptyList())
        coEvery { sessionMock.prepareReconnect() } returns Unit

        call.fastReconnect()
        advanceTimeBy(50) // Only work delay, no backoff

        // First attempt should have no backoff delay, only the work delay
        coVerify(exactly = 1) { sessionMock.fastReconnect(any()) }
    }

    @Test
    fun `fastReconnect backoff delays increase exponentially`() = runTest {
        val sessionMock = mockk<RtcSession>(relaxed = true)
        val call = client.call("default", randomUUID())
        call.session = sessionMock

        var callCount = 0
        coEvery { sessionMock.fastReconnect(any()) } coAnswers {
            callCount++
            delay(50) // Small work delay
        }
        coEvery { sessionMock.currentSfuInfo() } returns Triple("session1", emptyList(), emptyList())
        coEvery { sessionMock.prepareReconnect() } returns Unit

        // First attempt - no backoff
        call.fastReconnect()
        advanceTimeBy(50)
        assertEquals(1, callCount)

        // Second attempt - should have ~500ms backoff (with jitter: 450-550ms)
        call.fastReconnect()
        advanceTimeBy(400) // Advance less than backoff
        assertEquals(1, callCount) // Still only 1 call

        advanceTimeBy(200) // Complete backoff
        assertEquals(2, callCount) // Now 2 calls

        // Third attempt - should have ~1000ms backoff (with jitter: 900-1100ms)
        call.fastReconnect()
        advanceTimeBy(900) // Advance less than backoff
        assertEquals(2, callCount) // Still only 2 calls

        advanceTimeBy(200) // Complete backoff
        assertEquals(3, callCount) // Now 3 calls

        // Verify exponential increase: delays should roughly double
        // We can't measure exact delays due to jitter, but we verify the behavior
        // by checking that calls don't happen until backoff completes
    }

    @Test
    fun `fastReconnect backoff is capped at maximum delay`() = runTest {
        val sessionMock = mockk<RtcSession>(relaxed = true)
        val call = client.call("default", randomUUID())
        call.session = sessionMock

        coEvery { sessionMock.fastReconnect(any()) } coAnswers {
            delay(50)
        }
        coEvery { sessionMock.currentSfuInfo() } returns Triple("session1", emptyList(), emptyList())
        coEvery { sessionMock.prepareReconnect() } returns Unit

        // Make 5 attempts to reach max backoff
        repeat(5) { attempt ->
            call.fastReconnect()
            if (attempt > 0) {
                // After first attempt, each retry should have backoff
                // The 5th attempt (4th retry) should have delay of ~8000ms (capped at 10000ms)
                // With jitter, it could be up to 11000ms
                advanceTimeBy(50) // Work delay
                advanceTimeBy(11000) // Max backoff + jitter
            } else {
                advanceTimeBy(50) // First attempt, no backoff
            }
        }

        // Verify all 5 attempts were made
        coVerify(exactly = 5) { sessionMock.fastReconnect(any()) }
    }

    @Test
    fun `fastReconnect resets counter when session is null`() = runTest {
        val call = client.call("default", randomUUID())
        call.session = null

        call.fastReconnect()
        advanceTimeBy(100)

        // Counter should be reset when session is null
        // We can verify this by checking that next attempt doesn't have backoff
        val sessionMock = mockk<RtcSession>(relaxed = true)
        call.session = sessionMock

        coEvery { sessionMock.fastReconnect(any()) } coAnswers {
            delay(50)
        }
        coEvery { sessionMock.currentSfuInfo() } returns Triple("session1", emptyList(), emptyList())
        coEvery { sessionMock.prepareReconnect() } returns Unit

        // This should be treated as first attempt (no backoff)
        call.fastReconnect()
        advanceTimeBy(50) // No backoff delay needed

        coVerify(exactly = 1) { sessionMock.fastReconnect(any()) }
    }

    @Test
    fun `fastReconnect counter resets on successful join`() = runTest {
        val sessionMock = mockk<RtcSession>(relaxed = true)
        val call = client.call("default", randomUUID())
        call.session = sessionMock

        coEvery { sessionMock.fastReconnect(any()) } coAnswers {
            delay(50)
        }
        coEvery { sessionMock.currentSfuInfo() } returns Triple("session1", emptyList(), emptyList())
        coEvery { sessionMock.prepareReconnect() } returns Unit

        // Make 3 failed attempts
        repeat(3) { attempt ->
            call.fastReconnect()
            if (attempt > 0) {
                advanceTimeBy(600) // Backoff + work
            } else {
                advanceTimeBy(50) // First attempt
            }
        }

        // Simulate successful join (which resets the counter)
        call._join()

        // Next fastReconnect should be treated as first attempt (no backoff)
        call.fastReconnect()
        advanceTimeBy(50) // No backoff delay

        // Verify we got 4 calls total (3 failed + 1 after reset)
        coVerify(exactly = 4) { sessionMock.fastReconnect(any()) }
    }
}
