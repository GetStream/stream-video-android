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
        val sessionErrors = mutableListOf<Error>()

        val decorator = RetryableSignalingServiceDecorator(
            decorated = inner,
            onSessionError = { sessionErrors.add(it) },
            retryProcessor = StreamRetryProcessor("test"),
            policy = noDelayPolicy,
        )

        val result = decorator.updateSubscriptions(UpdateSubscriptionsRequest())
        assertNull(result.error)
        assertTrue(sessionErrors.isEmpty())
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
        val sessionErrors = mutableListOf<Error>()

        val decorator = RetryableSignalingServiceDecorator(
            decorated = inner,
            onSessionError = { sessionErrors.add(it) },
            retryProcessor = StreamRetryProcessor("test"),
            policy = noDelayPolicy,
        )

        val result = decorator.updateSubscriptions(UpdateSubscriptionsRequest())
        assertNull(result.error)
        assertEquals(3, callCount)
        assertTrue(sessionErrors.isEmpty())
    }

    @Test
    fun `session-fatal error (PARTICIPANT_NOT_FOUND) fires onSessionError and throws`() = runTest {
        val fatalError = Error(
            code = ErrorCode.ERROR_CODE_PARTICIPANT_NOT_FOUND,
            message = "participant gone",
            should_retry = false,
        )
        val inner = mockk<SignalServerService>(relaxed = true) {
            coEvery { updateSubscriptions(any()) } returns
                UpdateSubscriptionsResponse(error = fatalError)
        }
        val sessionErrors = mutableListOf<Error>()

        val decorator = RetryableSignalingServiceDecorator(
            decorated = inner,
            onSessionError = { sessionErrors.add(it) },
            retryProcessor = StreamRetryProcessor("test"),
            policy = noDelayPolicy,
        )

        var thrown: Throwable? = null
        try {
            decorator.updateSubscriptions(UpdateSubscriptionsRequest())
        } catch (e: Throwable) {
            thrown = e
        }

        assertTrue(
            "Session-fatal error should throw SessionFatalException",
            thrown is SessionFatalException,
        )
        assertEquals(fatalError, (thrown as SessionFatalException).sfuError)
        assertEquals(1, sessionErrors.size)
        assertEquals(ErrorCode.ERROR_CODE_PARTICIPANT_NOT_FOUND, sessionErrors[0].code)
    }

    @Test
    fun `non-fatal SFU error (validation) does NOT fire onSessionError`() = runTest {
        val validationError = Error(
            code = ErrorCode.ERROR_CODE_REQUEST_VALIDATION_FAILED,
            message = "invalid request",
            should_retry = false,
        )
        val inner = mockk<SignalServerService>(relaxed = true) {
            coEvery { updateSubscriptions(any()) } returns
                UpdateSubscriptionsResponse(error = validationError)
        }
        val sessionErrors = mutableListOf<Error>()

        val decorator = RetryableSignalingServiceDecorator(
            decorated = inner,
            onSessionError = { sessionErrors.add(it) },
            retryProcessor = StreamRetryProcessor("test"),
            policy = noDelayPolicy,
        )

        val result = decorator.updateSubscriptions(UpdateSubscriptionsRequest())
        assertEquals(validationError, result.error)
        assertTrue("Non-fatal errors should not trigger onSessionError", sessionErrors.isEmpty())
    }

    @Test
    fun `SIGNAL_LOST error is not retried and fires onSessionError and throws`() = runTest {
        val signalLostError = Error(
            code = ErrorCode.ERROR_CODE_PARTICIPANT_SIGNAL_LOST,
            message = "signal lost",
            should_retry = true,
        )
        val inner = mockk<SignalServerService>(relaxed = true) {
            coEvery { updateSubscriptions(any()) } returns
                UpdateSubscriptionsResponse(error = signalLostError)
        }
        val sessionErrors = mutableListOf<Error>()

        val decorator = RetryableSignalingServiceDecorator(
            decorated = inner,
            onSessionError = { sessionErrors.add(it) },
            retryProcessor = StreamRetryProcessor("test"),
            policy = noDelayPolicy,
        )

        var thrown: Throwable? = null
        try {
            decorator.updateSubscriptions(UpdateSubscriptionsRequest())
        } catch (e: Throwable) {
            thrown = e
        }

        assertTrue(
            "SIGNAL_LOST should throw SessionFatalException",
            thrown is SessionFatalException,
        )
        assertEquals(signalLostError, (thrown as SessionFatalException).sfuError)
        assertEquals(1, sessionErrors.size)
        assertEquals(ErrorCode.ERROR_CODE_PARTICIPANT_SIGNAL_LOST, sessionErrors[0].code)
        coVerify(exactly = 1) { inner.updateSubscriptions(any()) }
    }

    @Test
    fun `network failure after max retries rethrows without triggering reconnect`() = runTest {
        val inner = mockk<SignalServerService>(relaxed = true) {
            coEvery { updateSubscriptions(any()) } throws IOException("timeout")
        }
        val sessionErrors = mutableListOf<Error>()

        val decorator = RetryableSignalingServiceDecorator(
            decorated = inner,
            onSessionError = { sessionErrors.add(it) },
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
        assertTrue("Network failures should not trigger onSessionError", sessionErrors.isEmpty())
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
        val sessionErrors = mutableListOf<Error>()

        val decorator = RetryableSignalingServiceDecorator(
            decorated = inner,
            onSessionError = { sessionErrors.add(it) },
            retryProcessor = StreamRetryProcessor("test"),
            policy = noDelayPolicy,
        )

        val result = decorator.updateSubscriptions(UpdateSubscriptionsRequest())
        assertNull(result.error)
        assertEquals(3, callCount)
        assertTrue(sessionErrors.isEmpty())
    }

    @Test
    fun `exhausted retries on non-fatal error returns last error response without throwing`() = runTest {
        val retriableError = Error(
            code = ErrorCode.ERROR_CODE_SFU_SHUTTING_DOWN,
            message = "shutting down",
            should_retry = true,
        )
        val inner = mockk<SignalServerService>(relaxed = true) {
            coEvery { updateSubscriptions(any()) } returns
                UpdateSubscriptionsResponse(error = retriableError)
        }
        val sessionErrors = mutableListOf<Error>()

        val decorator = RetryableSignalingServiceDecorator(
            decorated = inner,
            onSessionError = { sessionErrors.add(it) },
            retryProcessor = StreamRetryProcessor("test"),
            policy = noDelayPolicy,
        )

        val result = decorator.updateSubscriptions(UpdateSubscriptionsRequest())

        assertEquals(
            "Exhausted retries should return the last error response",
            retriableError,
            result.error,
        )
        assertTrue(
            "Non-fatal errors should not trigger onSessionError even after retry exhaustion",
            sessionErrors.isEmpty(),
        )
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
