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

package io.getstream.video.android.core.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertFailsWith

class StreamRefCountedSingleFlightProcessorTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun `concurrent callers share one execution`() = runTest(testDispatcher) {
        val processor = StreamRefCountedSingleFlightProcessor(testScope)
        val executions = AtomicInteger(0)
        val gate = CompletableDeferred<Unit>()

        val jobs = (1..5).map {
            async {
                processor.run("key") {
                    executions.incrementAndGet()
                    gate.await()
                    "ok"
                }
            }
        }
        advanceUntilIdle()
        gate.complete(Unit)
        val results = jobs.awaitAll()
        advanceUntilIdle()

        assertEquals(listOf("ok", "ok", "ok", "ok", "ok"), results.map { it.getOrThrow() })
        assertEquals(1, executions.get())
    }

    @Test
    fun `cancelling one waiter leaves the shared job running`() = runTest(testDispatcher) {
        val processor = StreamRefCountedSingleFlightProcessor(testScope)
        val executions = AtomicInteger(0)
        val gate = CompletableDeferred<Unit>()

        val first = async {
            processor.run("key") {
                executions.incrementAndGet()
                gate.await()
                "ok"
            }
        }
        advanceUntilIdle()
        val second = async {
            processor.run("key") {
                executions.incrementAndGet()
                gate.await()
                "ok"
            }
        }
        advanceUntilIdle()

        first.cancel()
        advanceUntilIdle()
        assertFailsWith<CancellationException> { first.await() }

        gate.complete(Unit)
        assertEquals("ok", second.await().getOrThrow())
        advanceUntilIdle()
        assertEquals(1, executions.get())
    }

    @Test
    fun `cancelling the last waiter cancels the shared job`() = runTest(testDispatcher) {
        val processor = StreamRefCountedSingleFlightProcessor(testScope)
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        var completed = false

        val first = async {
            processor.run("key") {
                started.complete(Unit)
                gate.await()
                completed = true
                "ok"
            }
        }
        advanceUntilIdle()
        started.await()

        val second = async {
            processor.run("key") {
                gate.await()
                completed = true
                "ok"
            }
        }
        advanceUntilIdle()

        first.cancel()
        second.cancel()
        advanceUntilIdle()
        assertFailsWith<CancellationException> { first.await() }
        assertFailsWith<CancellationException> { second.await() }

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(completed)
        assertFalse(processor.has("key"))
    }

    @Test
    fun `cancelling the sole waiter cancels the shared job`() = runTest(testDispatcher) {
        val processor = StreamRefCountedSingleFlightProcessor(testScope)
        val gate = CompletableDeferred<Unit>()
        var completed = false

        val job = async {
            processor.run("key") {
                gate.await()
                completed = true
                "ok"
            }
        }
        advanceUntilIdle()
        job.cancel()
        advanceUntilIdle()
        assertFailsWith<CancellationException> { job.await() }

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(completed)
    }

    @Test
    fun `a run after the previous one finished starts a fresh attempt`() = runTest(
        testDispatcher,
    ) {
        val processor = StreamRefCountedSingleFlightProcessor(testScope)
        val executions = AtomicInteger(0)

        assertEquals(
            "a",
            processor.run("key") {
                executions.incrementAndGet()
                "a"
            }.getOrThrow(),
        )
        advanceUntilIdle()
        assertEquals(
            "b",
            processor.run("key") {
                executions.incrementAndGet()
                "b"
            }.getOrThrow(),
        )
        advanceUntilIdle()

        assertEquals(2, executions.get())
    }

    @Test
    fun `different keys do not coalesce`() = runTest(testDispatcher) {
        val processor = StreamRefCountedSingleFlightProcessor(testScope)
        val gate = CompletableDeferred<Unit>()
        val executions = AtomicInteger(0)

        val a = async {
            processor.run("a") {
                executions.incrementAndGet()
                gate.await()
                "a"
            }
        }
        val b = async {
            processor.run("b") {
                executions.incrementAndGet()
                gate.await()
                "b"
            }
        }
        advanceUntilIdle()
        gate.complete(Unit)
        assertEquals(
            listOf("a", "b"),
            listOf(a.await().getOrThrow(), b.await().getOrThrow()),
        )
        assertEquals(2, executions.get())
    }

    @Test
    fun `block exceptions propagate to all waiters as Result failure`() = runTest(
        testDispatcher,
    ) {
        val processor = StreamRefCountedSingleFlightProcessor(testScope)
        val gate = CompletableDeferred<Unit>()

        val first = async {
            processor.run("key") {
                gate.await()
                throw IllegalStateException("boom")
            }
        }
        advanceUntilIdle()
        val second = async {
            processor.run("key") {
                gate.await()
                throw IllegalStateException("boom")
            }
        }
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        val firstResult = first.await()
        val secondResult = second.await()
        assertTrue(firstResult.isFailure)
        assertTrue(secondResult.isFailure)
        assertTrue(firstResult.exceptionOrNull() is IllegalStateException)
        assertTrue(secondResult.exceptionOrNull() is IllegalStateException)
        assertEquals("boom", firstResult.exceptionOrNull()?.message)
        assertEquals("boom", secondResult.exceptionOrNull()?.message)
    }

    @Test
    fun `stop rejects new runs with Result failure`() = runTest(testDispatcher) {
        val processor = StreamRefCountedSingleFlightProcessor(testScope)
        assertTrue(processor.stop().isSuccess)

        val result = processor.run<String>("key") { error("should not run") }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ClosedSendChannelException)
    }
}
