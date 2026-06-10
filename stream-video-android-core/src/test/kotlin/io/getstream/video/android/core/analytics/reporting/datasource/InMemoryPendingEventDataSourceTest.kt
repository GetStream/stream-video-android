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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryPendingEventDataSourceTest {

    private val dataSource = InMemoryPendingEventDataSource()

    private fun event(index: Int) = ClientEvent(id = "call-$index")

    @Test
    fun `isEmpty is true before anything is saved`() {
        assertTrue(dataSource.isEmpty())
    }

    @Test
    fun `save stores events and isEmpty becomes false`() {
        dataSource.save(listOf(event(0)))

        assertFalse(dataSource.isEmpty())
    }

    @Test
    fun `loadAndClear returns all saved events in order and clears the storage`() {
        dataSource.save(listOf(event(0), event(1)))
        dataSource.save(listOf(event(2)))

        val loaded = dataSource.loadAndClear()

        assertEquals(listOf("call-0", "call-1", "call-2"), loaded.map { it.id })
        assertTrue(dataSource.isEmpty())
    }

    @Test
    fun `loadAndClear on an empty source returns an empty list`() {
        assertTrue(dataSource.loadAndClear().isEmpty())
    }

    @Test
    fun `clear removes all stored events`() {
        dataSource.save(listOf(event(0), event(1)))

        dataSource.clear()

        assertTrue(dataSource.isEmpty())
        assertTrue(dataSource.loadAndClear().isEmpty())
    }
}
