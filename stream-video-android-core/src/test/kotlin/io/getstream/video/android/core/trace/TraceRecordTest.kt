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

package io.getstream.video.android.core.trace

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class TraceRecordTest {
    @Test
    fun `constructor sets properties correctly`() {
        val record = TraceRecord(tag = "test", id = "123", data = 42, timestamp = 1000L)
        assertEquals("test", record.tag)
        assertEquals("123", record.id)
        assertEquals(42, record.data)
        assertEquals(1000L, record.timestamp)
    }

    @Test
    fun `serialize returns correct array`() {
        val record = TraceRecord(tag = "tag1", id = "id1", data = "payload", timestamp = 12345L)
        val arr = record.serialize()
        assertArrayEquals(arrayOf("tag1", "id1", "payload", 12345L), arr)
    }

    @Test
    fun `equality and copy work as expected`() {
        val r1 = TraceRecord("a", "b", 1, 2L)
        val r2 = TraceRecord("a", "b", 1, 2L)
        assertEquals(r1, r2)
        val r3 = r1.copy(tag = "c")
        assertNotEquals(r1, r3)
        assertEquals("c", r3.tag)
    }

    @Test
    fun `null id and data are handled`() {
        val record = TraceRecord(tag = "t", id = null, data = null, timestamp = 0L)
        val arr = record.serialize()
        assertArrayEquals(arrayOf("t", null, null, 0L), arr)
    }

    @Test
    fun `take returns snapshot and clears buffer, rollback restores it`() {
        val tracer = Tracer("test-id")
        tracer.trace("tag1", "data1")
        tracer.trace("tag2", "data2")
        tracer.trace("tag3", "data3")
        val beforeTake = tracer.take()
        // The buffer should now be empty
        assertTrue(tracer.take().snapshot.isEmpty())
        // The snapshot should contain all previous entries
        assertEquals(3, beforeTake.snapshot.size)
        assertEquals("tag1", beforeTake.snapshot[0].tag)
        assertEquals("data1", beforeTake.snapshot[0].data)
        // Rollback should restore the buffer
        beforeTake.rollback()
        val afterRollback = tracer.take()
        assertEquals(3, afterRollback.snapshot.size)
        assertEquals("tag1", afterRollback.snapshot[0].tag)
        assertEquals("data1", afterRollback.snapshot[0].data)
    }

    /**
     * Many threads hammer [Tracer.trace] at once. The buffer must remain consistent and no
     * exception (e.g. ConcurrentModificationException) may escape. Every emitted record must be
     * accounted for in the drained snapshot.
     */
    @Test
    fun `concurrent trace calls do not throw and record every entry`() {
        val tracer = Tracer("concurrent-trace")
        val threadCount = 8
        val perThread = 2_000
        val pool = Executors.newFixedThreadPool(threadCount)
        val start = CountDownLatch(1)
        val errors = CopyOnWriteArrayList<Throwable>()

        val futures = (0 until threadCount).map { t ->
            pool.submit {
                try {
                    start.await()
                    repeat(perThread) { i -> tracer.trace("thread-$t", i) }
                } catch (e: Throwable) {
                    errors.add(e)
                }
            }
        }
        start.countDown() // release all threads at once to maximise contention
        futures.forEach { it.get() }
        pool.shutdown()
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS))

        assertTrue("Unexpected errors during concurrent trace: $errors", errors.isEmpty())
        val snapshot = tracer.take().snapshot
        assertEquals(threadCount * perThread, snapshot.size)
    }

    /**
     * Draining via [Tracer.take] (which iterates the buffer internally) while other threads keep
     * appending must not throw ConcurrentModificationException. This guards the locking contract:
     * iteration and mutation are serialised by the same monitor.
     */
    @Test
    fun `take while concurrently tracing never throws`() {
        val tracer = Tracer("concurrent-take")
        val pool = Executors.newFixedThreadPool(4)
        val running = AtomicBoolean(true)
        val errors = CopyOnWriteArrayList<Throwable>()

        val writers = (0 until 3).map { t ->
            pool.submit {
                try {
                    while (running.get()) tracer.trace("writer-$t", t)
                } catch (e: Throwable) {
                    errors.add(e)
                }
            }
        }
        val reader = pool.submit {
            try {
                repeat(1_000) {
                    // Force iteration of the drained slice as well as the internal copy.
                    tracer.take().snapshot.forEach { _ -> }
                }
            } catch (e: Throwable) {
                errors.add(e)
            }
        }

        reader.get()
        running.set(false)
        writers.forEach { it.get() }
        pool.shutdown()
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS))

        assertTrue("Unexpected errors during concurrent take: $errors", errors.isEmpty())
    }

    /**
     * Drives [Tracer.trace] from many threads. Buffer order is ignored; instead records are grouped
     * by producer (tag) and ordered by the per-thread call index in `data`. Because the timestamp
     * is stamped at call time and each thread calls sequentially, every producer's timestamps must
     * be non-decreasing along its own call order — regardless of lock contention or buffer
     * interleaving. Asserted as `>=` because call-time [System.currentTimeMillis] ties at
     * millisecond resolution.
     */
    @Test
    fun `concurrent traces keep per-thread timestamps non-decreasing in call order`() {
        val tracer = Tracer("fifo-concurrent")
        val threadCount = 8
        val perThread = 2_000
        val pool = Executors.newFixedThreadPool(threadCount)
        val start = CountDownLatch(1)

        val futures = (0 until threadCount).map { t ->
            pool.submit {
                start.await() // release together to maximise contention
                repeat(perThread) { i -> tracer.trace("thread-$t", i) }
            }
        }
        start.countDown()
        futures.forEach { it.get() }
        pool.shutdown()
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS))

        val snapshot = tracer.take().snapshot
        assertEquals(threadCount * perThread, snapshot.size) // nothing lost

        val byThread = snapshot.groupBy { it.tag }
        assertEquals(threadCount, byThread.size)
        byThread.forEach { (tag, records) ->
            val inCallOrder = records.sortedBy { it.data as Int }
            assertEquals(perThread, inCallOrder.size)
            for (i in 1 until inCallOrder.size) {
                assertTrue(
                    "timestamp regressed for $tag at call $i: " +
                        "${inCallOrder[i - 1].timestamp} > ${inCallOrder[i].timestamp}",
                    inCallOrder[i].timestamp >= inCallOrder[i - 1].timestamp,
                )
            }
        }
    }
}
