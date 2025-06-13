package io.getstream.video.android.core.trace

import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TracerManagerTest {
    private lateinit var manager: TracerManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        manager = TracerManager(true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `tracer returns same instance for same name`() {
        val t1 = manager.tracer("foo")
        val t2 = manager.tracer("foo")
        assertSame(t1, t2)
    }

    @Test
    fun `tracers returns all created tracers`() {
        val t1 = manager.tracer("a")
        val t2 = manager.tracer("b")
        val all = manager.tracers()
        assertTrue(all.contains(t1))
        assertTrue(all.contains(t2))
        assertEquals(2, all.size)
    }

    @Test
    fun `clear disposes and removes all tracers`() {
        manager.tracer("x")
        manager.tracer("y")
        manager.clear()
        assertTrue(manager.tracers().isEmpty())
    }

    @Test
    fun `setEnabled propagates to all tracers`() {
        val t1 = manager.tracer("foo")
        val t2 = manager.tracer("bar")
        manager.setEnabled(false)
        assertFalse(t1.isEnabled())
        assertFalse(t2.isEnabled())
        manager.setEnabled(true)
        assertTrue(t1.isEnabled())
        assertTrue(t2.isEnabled())
    }
}

