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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import stream.video.sfu.models.Error
import stream.video.sfu.models.ErrorCode
import stream.video.sfu.signal.UpdateSubscriptionsRequest
import stream.video.sfu.signal.UpdateSubscriptionsResponse
import java.io.IOException

class RetryableSignalingServiceDecoratorTest {

    private val noDelayPolicy = StreamRetryPolicy.fixed(
        minRetries = 1,
        maxRetries = 3,
        delayMillis = 0,
        initialDelayMillis = 0,
    )

    @Test
    fun `successful call returns response without callbacks`() = runTest {
        val successResponse = UpdateSubscriptionsResponse()
        val inner = mockk<SignalServerService>(relaxed = true) {
            coEvery { updateSubscriptions(any()) } returns successResponse
        }
        val terminalErrors = mutableListOf<Error>()
        val networkErrors = mutableListOf<Throwable>()

        val decorator = RetryableSignalingServiceDecorator(
            decorated = inner,
            onTerminalError = { terminalErrors.add(it) },
            onNetworkFailure = { networkErrors.add(it) },
            retryProcessor = StreamRetryProcessor("test"),
            policy = noDelayPolicy,
        )

        val result = decorator.updateSubscriptions(UpdateSubscriptionsRequest())
        assertNull(result.error)
        assertTrue(terminalErrors.isEmpty())
        assertTrue(networkErrors.isEmpty())
    }

    @Test
    fun `retries on transient SFU error with should_retry=true then succeeds`() = runTest {
        var callCount = 0
        val retriableError = Error(
            code = ErrorCode.ERROR_CODE_SFU_SHUTTING_DOWN,
            message = "shutting down",
            should_retry = true,
        )
        val successResponse = UpdateSubscriptionsResponse()
        val inner = mockk<SignalServerService>(relaxed = true) {
            coEvery { updateSubscriptions(any()) } answers {
                callCount++
                if (callCount < 3) {
                    UpdateSubscriptionsResponse(error = retriableError)
                } else {
                    successResponse
                }
            }
        }
        val terminalErrors = mutableListOf<Error>()

        val decorator = RetryableSignalingServiceDecorator(
            decorated = inner,
            onTerminalError = { terminalErrors.add(it) },
            retryProcessor = StreamRetryProcessor("test"),
            policy = noDelayPolicy,
        )

        val result = decorator.updateSubscriptions(UpdateSubscriptionsRequest())
        assertNull(result.error)
        assertEquals(3, callCount)
        assertTrue(terminalErrors.isEmpty())
    }

    @Test
    fun `terminal SFU error (should_retry=false) invokes onTerminalError`() = runTest {
        val terminalError = Error(
            code = ErrorCode.ERROR_CODE_PARTICIPANT_NOT_FOUND,
            message = "participant gone",
            should_retry = false,
        )
        val inner = mockk<SignalServerService>(relaxed = true) {
            coEvery { updateSubscriptions(any()) } returns
                UpdateSubscriptionsResponse(error = terminalError)
        }
        val terminalErrors = mutableListOf<Error>()

        val decorator = RetryableSignalingServiceDecorator(
            decorated = inner,
            onTerminalError = { terminalErrors.add(it) },
            retryProcessor = StreamRetryProcessor("test"),
            policy = noDelayPolicy,
        )

        val result = decorator.updateSubscriptions(UpdateSubscriptionsRequest())
        assertEquals(terminalError, result.error)
        assertEquals(1, terminalErrors.size)
        assertEquals(ErrorCode.ERROR_CODE_PARTICIPANT_NOT_FOUND, terminalErrors[0].code)
    }

    @Test
    fun `SIGNAL_LOST error is not retried and fires onTerminalError`() = runTest {
        val signalLostError = Error(
            code = ErrorCode.ERROR_CODE_PARTICIPANT_SIGNAL_LOST,
            message = "signal lost",
            should_retry = true,
        )
        val inner = mockk<SignalServerService>(relaxed = true) {
            coEvery { updateSubscriptions(any()) } returns
                UpdateSubscriptionsResponse(error = signalLostError)
        }
        val terminalErrors = mutableListOf<Error>()

        val decorator = RetryableSignalingServiceDecorator(
            decorated = inner,
            onTerminalError = { terminalErrors.add(it) },
            retryProcessor = StreamRetryProcessor("test"),
            policy = noDelayPolicy,
        )

        val result = decorator.updateSubscriptions(UpdateSubscriptionsRequest())
        assertEquals(signalLostError, result.error)
        assertEquals(1, terminalErrors.size)
        assertEquals(ErrorCode.ERROR_CODE_PARTICIPANT_SIGNAL_LOST, terminalErrors[0].code)
        coVerify(exactly = 1) { inner.updateSubscriptions(any()) }
    }

    @Test
    fun `network failure after max retries invokes onNetworkFailure`() = runTest {
        val inner = mockk<SignalServerService>(relaxed = true) {
            coEvery { updateSubscriptions(any()) } throws IOException("timeout")
        }
        val networkErrors = mutableListOf<Throwable>()
        val terminalErrors = mutableListOf<Error>()

        val decorator = RetryableSignalingServiceDecorator(
            decorated = inner,
            onTerminalError = { terminalErrors.add(it) },
            onNetworkFailure = { networkErrors.add(it) },
            retryProcessor = StreamRetryProcessor("test"),
            policy = noDelayPolicy,
        )

        var thrown: Throwable? = null
        try {
            decorator.updateSubscriptions(UpdateSubscriptionsRequest())
        } catch (e: Throwable) {
            thrown = e
        }

        assertTrue(thrown is IOException)
        assertEquals(1, networkErrors.size)
        assertTrue(networkErrors[0] is IOException)
        assertTrue(terminalErrors.isEmpty())
    }

    @Test
    fun `retries on network error then succeeds`() = runTest {
        var callCount = 0
        val successResponse = UpdateSubscriptionsResponse()
        val inner = mockk<SignalServerService>(relaxed = true) {
            coEvery { updateSubscriptions(any()) } answers {
                callCount++
                if (callCount < 3) throw IOException("network error")
                successResponse
            }
        }
        val networkErrors = mutableListOf<Throwable>()

        val decorator = RetryableSignalingServiceDecorator(
            decorated = inner,
            onNetworkFailure = { networkErrors.add(it) },
            retryProcessor = StreamRetryProcessor("test"),
            policy = noDelayPolicy,
        )

        val result = decorator.updateSubscriptions(UpdateSubscriptionsRequest())
        assertNull(result.error)
        assertEquals(3, callCount)
        assertTrue(networkErrors.isEmpty())
    }

    @Test
    fun `exhausted SFU retries fires onTerminalError with last error`() = runTest {
        val retriableError = Error(
            code = ErrorCode.ERROR_CODE_SFU_SHUTTING_DOWN,
            message = "shutting down",
            should_retry = true,
        )
        val inner = mockk<SignalServerService>(relaxed = true) {
            coEvery { updateSubscriptions(any()) } returns
                UpdateSubscriptionsResponse(error = retriableError)
        }
        val terminalErrors = mutableListOf<Error>()

        val decorator = RetryableSignalingServiceDecorator(
            decorated = inner,
            onTerminalError = { terminalErrors.add(it) },
            retryProcessor = StreamRetryProcessor("test"),
            policy = noDelayPolicy,
        )

        val result = decorator.updateSubscriptions(UpdateSubscriptionsRequest())
        assertEquals(retriableError, result.error)
        assertEquals(1, terminalErrors.size)
        assertSame(retriableError, terminalErrors[0])
    }

    @Test
    fun `all SFU API methods are retried`() = runTest {
        val inner = mockk<SignalServerService>(relaxed = true)
        val decorator = RetryableSignalingServiceDecorator(
            decorated = inner,
            retryProcessor = StreamRetryProcessor("test"),
            policy = noDelayPolicy,
        )

        decorator.setPublisher(mockk(relaxed = true))
        decorator.sendAnswer(mockk(relaxed = true))
        decorator.iceTrickle(mockk(relaxed = true))
        decorator.updateSubscriptions(mockk(relaxed = true))
        decorator.updateMuteStates(mockk(relaxed = true))
        decorator.iceRestart(mockk(relaxed = true))
        decorator.sendStats(mockk(relaxed = true))
        decorator.startNoiseCancellation(mockk(relaxed = true))
        decorator.stopNoiseCancellation(mockk(relaxed = true))

        coVerify(exactly = 1) { inner.setPublisher(any()) }
        coVerify(exactly = 1) { inner.sendAnswer(any()) }
        coVerify(exactly = 1) { inner.iceTrickle(any()) }
        coVerify(exactly = 1) { inner.updateSubscriptions(any()) }
        coVerify(exactly = 1) { inner.updateMuteStates(any()) }
        coVerify(exactly = 1) { inner.iceRestart(any()) }
        coVerify(exactly = 1) { inner.sendStats(any()) }
        coVerify(exactly = 1) { inner.startNoiseCancellation(any()) }
        coVerify(exactly = 1) { inner.stopNoiseCancellation(any()) }
    }
}
