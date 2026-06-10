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

import androidx.annotation.IntRange
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.getstream.android.video.generated.infrastructure.Serializer
import io.getstream.android.video.generated.models.ClientEvent
import io.getstream.log.taggedLogger
import java.io.File

/**
 * Persists pending telemetry events to a single NDJSON file across process restarts.
 *
 * [loadAndClear] reads up to [batchSize] events and removes them from the file.
 * The lock only covers the file read/write itself — not the network call — so the
 * blocked window is microseconds, not seconds.
 *
 * Storage is capped at [MAX_EVENTS] entries and [MAX_FILE_SIZE_BYTES] bytes. When either
 * cap is exceeded after a write, the oldest events (the head of the file) are dropped in
 * chunks of [DROP_CHUNK_SIZE] until both caps are satisfied again.
 *
 * This class is not thread-safe on its own; wrap with [SynchronizedPendingEventDataSource].
 */
internal class FileBasedPendingEventDataSource(
    storageDir: File,
    moshi: Moshi = Serializer.moshi,
    @IntRange(5, 10)
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
) : PendingEventDataSource {

    companion object {
        const val DEFAULT_BATCH_SIZE = 10
        private const val MAX_EVENTS = 1000
        private const val MAX_FILE_SIZE_BYTES = 2_000_000L // 2 MB
        private const val DROP_CHUNK_SIZE = 100
        private const val FILE_NAME = "stream_video_analytics_pending_events.ndjson"
    }

    private val logger by taggedLogger("FileBasedPendingEventDataSource")

    private val file = File(storageDir, FILE_NAME)
    private val adapter: JsonAdapter<ClientEvent> = moshi.adapter(ClientEvent::class.java)

    override fun save(events: List<ClientEvent>) {
        if (events.isEmpty()) return
        try {
            if (file.parentFile?.exists() == false) file.parentFile?.mkdirs()
            val lines = events.joinToString(separator = "\n", postfix = "\n") { adapter.toJson(it) }
            file.appendText(lines)
            dropOldestIfOverCaps()
        } catch (e: Exception) {
            logger.w { "[save] Failed to persist ${events.size} event(s): ${e.message}" }
        }
    }

    /**
     * Oldest events sit at the head of the append-only file, so they are the ones
     * sacrificed when the caps are exceeded. Dropping proceeds in chunks of
     * [DROP_CHUNK_SIZE] until both [MAX_EVENTS] and [MAX_FILE_SIZE_BYTES] hold again.
     */
    private fun dropOldestIfOverCaps() {
        val lines = file.readLines().filter { it.isNotBlank() }

        if (file.length() <= MAX_FILE_SIZE_BYTES && lines.size <= MAX_EVENTS) {
            return
        }

        val lineSizes = lines.map { it.toByteArray(Charsets.UTF_8).size + 1L }

        var remainingBytes = lineSizes.sum()
        var dropCount = 0

        while (
            dropCount < lines.size &&
            (lines.size - dropCount > MAX_EVENTS || remainingBytes > MAX_FILE_SIZE_BYTES)
        ) {
            val end = minOf(dropCount + DROP_CHUNK_SIZE, lines.size)

            for (i in dropCount until end) {
                remainingBytes -= lineSizes[i]
            }

            dropCount = end
        }

        val retainedLines = lines.subList(dropCount, lines.size)

        logger.w {
            "[dropOldestIfOverCaps] Dropped $dropCount oldest event(s), kept ${retainedLines.size}"
        }

        if (retainedLines.isEmpty()) {
            file.delete()
        } else {
            file.writeText(
                retainedLines.joinToString(
                    separator = "\n",
                    postfix = "\n",
                ),
            )
        }
    }

    override fun loadAndClear(): List<ClientEvent> {
        if (!file.exists() || file.length() == 0L) return emptyList()
        return try {
            val allLines = file.readLines().filter { it.isNotBlank() }
            val batch = allLines.take(batchSize)
            val parsed = batch.map {
                runCatching { adapter.fromJson(it) }.getOrNull()
            }
            if (parsed.any { it == null }) {
                logger.w { "[loadAndClear] Parse failure in pending batch; keeping file intact." }
                return emptyList()
            }
            val remaining = allLines.drop(batch.size)
            if (remaining.isEmpty()) {
                file.delete()
            } else {
                file.writeText(remaining.joinToString(separator = "\n", postfix = "\n"))
            }
            logger.d { "[loadAndClear] batch=${batch.size}, remaining=${remaining.size}" }
            parsed.filterNotNull()
        } catch (e: Exception) {
            logger.w { "[loadAndClear] Failed: ${e.message}" }
            emptyList()
        }
    }

    override fun isEmpty(): Boolean = !file.exists() || file.length() == 0L

    override fun clear() {
        if (file.exists()) {
            file.writeText("")
        }
    }
}
