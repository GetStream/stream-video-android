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

package io.getstream.video.android.core.analytics.reporting.datasource

import io.getstream.android.video.generated.models.ClientEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SynchronizedPendingEventDataSourceTest {

    private val delegate = mockk<PendingEventDataSource>(relaxed = true)
    private val dataSource = SynchronizedPendingEventDataSource(delegate)

    private fun event(id: String) = ClientEvent(id = id)

    @Test
    fun `save delegates to the wrapped data source`() {
        val events = listOf(event("e1"))

        dataSource.save(events)

        verify(exactly = 1) { delegate.save(events) }
    }

    @Test
    fun `loadAndClear delegates and returns the delegate result`() {
        val pending = listOf(event("e1"), event("e2"))
        every { delegate.loadAndClear() } returns pending

        assertEquals(pending, dataSource.loadAndClear())
        verify(exactly = 1) { delegate.loadAndClear() }
    }

    @Test
    fun `isEmpty delegates to the wrapped data source`() {
        every { delegate.isEmpty() } returns false

        assertEquals(false, dataSource.isEmpty())
        verify(exactly = 1) { delegate.isEmpty() }
    }

    @Test
    fun `clear delegates to the wrapped data source`() {
        dataSource.clear()

        verify(exactly = 1) { delegate.clear() }
    }

    @Test
    fun `concurrent saves and drains neither lose nor duplicate events`() {
        val source = SynchronizedPendingEventDataSource(InMemoryPendingEventDataSource())
        val drained = ConcurrentLinkedQueue<ClientEvent>()
        val executor = Executors.newFixedThreadPool(6)

        val writers = (0 until 4).map { writer ->
            executor.submit {
                repeat(100) { index -> source.save(listOf(event("w$writer-$index"))) }
            }
        }
        val drainers = (0 until 2).map {
            executor.submit {
                repeat(200) { drained.addAll(source.loadAndClear()) }
            }
        }

        (writers + drainers).forEach { it.get(10, TimeUnit.SECONDS) }
        drained.addAll(source.loadAndClear())
        executor.shutdown()

        val ids = drained.map { it.id }
        assertEquals(400, ids.size)
        assertEquals(400, ids.toSet().size)
        assertTrue(source.isEmpty())
    }
}
