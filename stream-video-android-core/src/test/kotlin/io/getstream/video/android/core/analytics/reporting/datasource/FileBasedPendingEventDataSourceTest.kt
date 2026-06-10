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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileBasedPendingEventDataSourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storageDir: File
    private lateinit var dataSource: FileBasedPendingEventDataSource

    private val pendingFile: File
        get() = File(storageDir, "stream_video_analytics_pending_events.ndjson")

    @Before
    fun setup() {
        storageDir = tempFolder.newFolder()
        dataSource = FileBasedPendingEventDataSource(storageDir)
    }

    private fun event(index: Int, userAgent: String? = null) =
        ClientEvent(id = "call-$index", userAgent = userAgent)

    private fun pendingLines() = pendingFile.readLines().filter { it.isNotBlank() }

    @Test
    fun `save and loadAndClear roundtrip preserves events and their order`() {
        dataSource.save(listOf(event(0), event(1), event(2)))

        val loaded = dataSource.loadAndClear()

        assertEquals(listOf("call-0", "call-1", "call-2"), loaded.map { it.id })
    }

    @Test
    fun `events survive a new instance over the same directory`() {
        dataSource.save(listOf(event(0)))

        val restarted = FileBasedPendingEventDataSource(storageDir)

        assertFalse(restarted.isEmpty())
        assertEquals(listOf("call-0"), restarted.loadAndClear().map { it.id })
    }

    @Test
    fun `loadAndClear drains at most batchSize events and keeps the remainder`() {
        dataSource.save((0 until 12).map { event(it) })

        val firstBatch = dataSource.loadAndClear()
        val secondBatch = dataSource.loadAndClear()

        assertEquals((0 until 10).map { "call-$it" }, firstBatch.map { it.id })
        assertEquals(listOf("call-10", "call-11"), secondBatch.map { it.id })
        assertTrue(dataSource.isEmpty())
    }

    @Test
    fun `loadAndClear deletes the file once fully drained`() {
        dataSource.save(listOf(event(0)))
        assertTrue(pendingFile.exists())

        dataSource.loadAndClear()

        assertFalse(pendingFile.exists())
    }

    @Test
    fun `loadAndClear keeps the file intact when a line is corrupt`() {
        dataSource.save(listOf(event(0)))
        pendingFile.appendText("{not-valid-json\n")

        val loaded = dataSource.loadAndClear()

        assertTrue(loaded.isEmpty())
        assertEquals(2, pendingLines().size)
    }

    @Test
    fun `isEmpty is true before any save and after clear`() {
        assertTrue(dataSource.isEmpty())

        dataSource.save(listOf(event(0)))
        assertFalse(dataSource.isEmpty())

        dataSource.clear()
        assertTrue(dataSource.isEmpty())
    }

    @Test
    fun `exceeding the event cap drops the oldest events in chunks`() {
        dataSource.save((0 until 1000).map { event(it) })
        assertEquals(1000, pendingLines().size)

        dataSource.save(listOf(event(1000)))

        val lines = pendingLines()
        assertEquals(901, lines.size)
        assertTrue(lines.first().contains("\"call-100\""))
        assertTrue(lines.last().contains("\"call-1000\""))
    }

    @Test
    fun `exceeding the size cap drops the oldest events until under the limit`() {
        val largePayload = "a".repeat(20_000)

        dataSource.save((0 until 150).map { event(it, userAgent = largePayload) })

        val lines = pendingLines()
        assertEquals(50, lines.size)
        assertTrue(pendingFile.length() <= 2_000_000L)
        assertTrue(lines.first().contains("\"call-100\""))
    }
}
