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

package io.getstream.video.android.core.trace

import io.mockk.MockKAnnotations
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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
