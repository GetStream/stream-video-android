package io.getstream.video.android.core.trace

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
