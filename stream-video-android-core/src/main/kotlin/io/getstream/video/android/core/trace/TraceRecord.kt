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

import java.util.Collections

/**
 * A single trace item captured by [Tracer].
 *
 * @property tag A short identifier that categorises the trace entry.
 * @property id  Optional identifier propagated from the owning [Tracer].
 * @property data Arbitrary payload supplied by the caller.
 * @property timestamp Epoch-millis when the entry was recorded.
 */
data class TraceRecord(
    val tag: String,
    val id: String?,
    val data: Any?,
    val timestamp: Long,
)

/**
 * Serialize the [TraceRecord].
 *
 * @return an array of the values
 */
fun TraceRecord.serialize(): Array<Any?> = arrayOf(tag, id, data, timestamp)

/**
 * An append-only, thread-safe trace buffer that can be snapshotted and rolled back.
 *
 * All state mutations are guarded by [lock], ensuring atomicity across threads.
 */
class Tracer(private val id: String?) {

    private val buffer: MutableList<TraceRecord> = Collections.synchronizedList(mutableListOf())
    private var enabled: Boolean = true

    /**
     * Enables or disables tracing.
     *
     * Switching state clears any existing buffered entries.
     */
    fun setEnabled(enabled: Boolean) = synchronized(buffer) {
        if (this.enabled == enabled) return
        this.enabled = enabled
        buffer.clear()
    }

    /**
     * Records a trace entry when tracing is enabled.
     *
     * The lambda form matches the original TypeScript API and can be stored or passed
     * around as a function reference.
     */
    fun trace(tag: String, data: Any?) = synchronized(buffer) {
        if (!enabled) return@synchronized
        buffer.add(
            TraceRecord(
                tag = tag,
                id = id,
                data = data,
                timestamp = System.currentTimeMillis(),
            ),
        )
    }

    /**
     * Returns a snapshot of the current buffer and clears it.
     *
     * The returned [TraceSlice.rollback] reinserts the snapshot at the front
     * of the buffer, preserving original ordering.
     */
    fun take(): TraceSlice {
        val snapshot: List<TraceRecord> = synchronized(buffer) {
            val copy = buffer.toList()
            buffer.clear()
            copy
        }
        return TraceSlice(
            snapshot = snapshot,
            rollback = {
                synchronized(buffer) {
                    buffer.addAll(0, snapshot)
                }
            },
        )
    }

    /** Permanently discards all buffered trace entries. */
    fun dispose() = synchronized(buffer) {
        buffer.clear()
    }
}

/**
 * Immutable view of a drained trace buffer together with a [rollback] action.
 *
 * @property snapshot The list of entries captured at the time of [Tracer.take].
 * @property rollback Restores the snapshot to the head of the originating buffer.
 */
data class TraceSlice(
    val snapshot: List<TraceRecord>,
    val rollback: () -> Unit,
)
