package io.getstream.video.android.core.trace

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
    val timestamp: Long
)

/**
 * An append-only, thread-safe trace buffer that can be snapshotted and rolled back.
 *
 * All state mutations are guarded by [lock], ensuring atomicity across threads.
 */
class Tracer(private val id: String?) {

    private val buffer: MutableList<TraceRecord> = mutableListOf()
    private var enabled: Boolean = true
    private val lock = ReentrantLock()

    /**
     * Enables or disables tracing.
     *
     * Switching state clears any existing buffered entries.
     */
    fun setEnabled(enabled: Boolean) {
        lock.withLock {
            if (this.enabled == enabled) return
            this.enabled = enabled
            buffer.clear()
        }
    }

    /**
     * Records a trace entry when tracing is enabled.
     *
     * The lambda form matches the original TypeScript API and can be stored or passed
     * around as a function reference.
     */
    fun trace(tag: String, data: Any?) {
        lock.withLock {
            if (!enabled) return@withLock
            buffer.add(
                TraceRecord(
                    tag = tag,
                    id = id,
                    data = data,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Returns a snapshot of the current buffer and clears it.
     *
     * The returned [TraceSlice.rollback] reinserts the snapshot at the front
     * of the buffer, preserving original ordering.
     */
    fun take(): TraceSlice {
        val snapshot: List<TraceRecord> = lock.withLock {
            val copy = buffer.toList()
            buffer.clear()
            copy
        }
        return TraceSlice(
            snapshot = snapshot,
            rollback = {
                lock.withLock { buffer.addAll(0, snapshot) }
            }
        )
    }

    /** Permanently discards all buffered trace entries. */
    fun dispose() {
        lock.withLock { buffer.clear() }
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
    val rollback: () -> Unit
)
