package io.getstream.video.android.core.utils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SerialProcessorTest {

    private lateinit var testScope: TestScope
    private lateinit var processor: SerialProcessor

    @Before
    fun setup() {
        testScope = TestScope(UnconfinedTestDispatcher())
        processor = SerialProcessor(testScope)
    }

    @Test
    fun `single job returns value`() = runTest {
        val result = processor.submit { "hello".uppercase() }
        assertTrue(result.isSuccess)
        assertEquals("HELLO", result.getOrNull())
    }

    @Test
    fun `multiple jobs run in FIFO order`() = runTest {
        val results = mutableListOf<Int>()
        // schedule jobs that append to results in order
        repeat(5) { i ->
            processor.submit {
                results += i
                i
            }
        }
        // advance until idle
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(0,1,2,3,4), results)
    }

    @Test
    fun `exception in job produces failure`() = runTest {
        val ex = RuntimeException("boom")
        val result = processor.submit<Int> { throw ex }
        assertTrue(result.isFailure)
        assertSame(ex, result.exceptionOrNull())
    }

    @Test
    fun `stop cancels worker and allows restart`() = runTest {
        // first job
        val first = processor.submit { 1 }
        assertTrue(first.isSuccess)
        assertEquals(1, first.getOrNull())

        // stop the processor
        processor.stop()

        // ensure we can submit again after stop
        val second = processor.submit { 2 }
        assertTrue(second.isSuccess)
        assertEquals(2, second.getOrNull())
    }

    @Test
    fun `concurrent submits are serialized`() = testScope.runTest {

        processor = SerialProcessor(testScope.backgroundScope)
        val order = mutableListOf<Int>()

        // Use `yield()` instead of real delay to force a suspension point
        suspend fun work(i: Int): Int {
            kotlinx.coroutines.yield()
            order += i
            return i
        }

        // Fire off three submits concurrently
        val deferreds = (0 until 3).map { i ->
            async { processor.submit { work(i) } }
        }

        // No need to advance time; yield() works with UnconfinedTestDispatcher
        val results = deferreds.map { it.await() }

        // Check each succeeded with the correct value
        results.forEachIndexed { i, r ->
            assertTrue("job $i should succeed", r.isSuccess)
            assertEquals(i, r.getOrNull())
        }

        // And verify they ran in FIFO order
        assertEquals(listOf(0, 1, 2), order)
    }

}
