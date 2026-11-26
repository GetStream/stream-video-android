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

package io.getstream.video.android.core.call.hls

import android.os.SystemClock
import io.getstream.android.video.generated.models.HLSPlaybackStats
import io.getstream.android.video.generated.models.HLSPlayerEvent
import io.getstream.log.taggedLogger
import java.util.Collections
import java.util.IdentityHashMap

/** Clocks for HLS stats: [elapsedMs] measures durations, [wallMs] stamps reports and events. */
internal interface HlsStatsClock {
    fun elapsedMs(): Long
    fun wallMs(): Long

    object System : HlsStatsClock {
        override fun elapsedMs(): Long = SystemClock.elapsedRealtime()
        override fun wallMs(): Long = java.lang.System.currentTimeMillis()
    }
}

/**
 * Turns player states into the counters and events of the HLS stats reports, using the stall
 * definition shared by every SDK: a stall starts when [HlsPlaybackState.Playing] moves to
 * [HlsPlaybackState.Buffering] and ends when playback resumes. Loading before the first frame,
 * seeking and pausing are not stalls. Watch time counts [HlsPlaybackState.Playing] only.
 * [HlsPlaybackState.Ended] records a `stream_ended` event: the broadcast stopped and the viewer has
 * watched all of it.
 *
 * Events stay buffered until a report carrying them is accepted, see [acknowledgeEvents].
 * Thread-safe: the player reports on the main thread while reports are built elsewhere.
 */
internal class HlsStatsCollector(private val clock: HlsStatsClock) {

    companion object {
        const val EVENT_STALL_START = "stall_start"
        const val EVENT_STALL_END = "stall_end"
        const val EVENT_ERROR = "error"
        const val EVENT_STREAM_ENDED = "stream_ended"

        /** Oldest events are dropped beyond this, so a long outage cannot grow the buffer forever. */
        const val MAX_BUFFERED_EVENTS = 1_000
    }

    private val logger by taggedLogger("Video:HlsStatsCollector")
    private val lock = Any()

    private var state: HlsPlaybackState? = null
    private var hasPlayed = false
    private var finished = false
    private var playingSinceMs: Long? = null
    private var watchMs = 0L
    private var stallSinceMs: Long? = null
    private var stallCount = 0
    private var stallMs = 0L
    private var maxStallMs = 0L
    private var lastErrorCode: String? = null
    private var lastMetrics: HlsPlaybackMetrics? = null
    private val events = ArrayDeque<HLSPlayerEvent>()

    /** The code of the last playback error, or null if playback never failed. */
    val errorCode: String? get() = synchronized(lock) { lastErrorCode }

    /** Applies a state change. Returns true when this is the first time playback plays. */
    fun onState(newState: HlsPlaybackState): Boolean = synchronized(lock) {
        if (finished || newState == state) return false
        val now = clock.elapsedMs()
        playingSinceMs?.let { watchMs += now - it }
        playingSinceMs = null
        var firstPlay = false
        when (newState) {
            HlsPlaybackState.Playing -> {
                endStall(now)
                playingSinceMs = now
                firstPlay = !hasPlayed
                hasPlayed = true
            }
            HlsPlaybackState.Buffering -> if (state == HlsPlaybackState.Playing) startStall(now)
            HlsPlaybackState.Loading,
            HlsPlaybackState.Seeking,
            HlsPlaybackState.Paused,
            -> endStall(now)
            HlsPlaybackState.Ended -> {
                endStall(now)
                addEvent(EVENT_STREAM_ENDED)
            }
        }
        state = newState
        firstPlay
    }

    /**
     * Records a playback error. The player stops on an error and reports no state until it plays
     * again, so watch time and an open stall end here. The same code reported again in a row is
     * recorded once.
     */
    fun onError(code: String) = synchronized(lock) {
        if (finished) return
        val now = clock.elapsedMs()
        playingSinceMs?.let { watchMs += now - it }
        playingSinceMs = null
        endStall(now)
        state = null
        if (code == lastErrorCode) return
        lastErrorCode = code
        addEvent(EVENT_ERROR, mapOf("code" to code))
    }

    /**
     * Cumulative stats since playback started, with [metrics] as the current readings. Null
     * [metrics] reuses the last readings, for example once the player is released.
     */
    fun stats(metrics: HlsPlaybackMetrics?): HLSPlaybackStats = synchronized(lock) {
        if (metrics != null) lastMetrics = metrics
        val now = clock.elapsedMs()
        val openStallMs = stallSinceMs?.let { now - it } ?: 0L
        val readings = lastMetrics
        HLSPlaybackStats(
            watchDurationMs = watchMs + (playingSinceMs?.let { now - it } ?: 0L),
            stallCount = stallCount,
            stallDurationMs = stallMs + openStallMs,
            maxStallDurationMs = maxOf(maxStallMs, openStallMs),
            droppedFrames = readings?.droppedFrames?.coerceAtLeast(0) ?: 0,
            bitrateKbps = readings?.bitrateKbps?.coerceAtLeast(0) ?: 0,
            bufferLengthMs = readings?.bufferLengthMs.toIntMs(),
            liveLatencyMs = readings?.liveLatencyMs.toIntMs(),
            width = readings?.width?.coerceAtLeast(0) ?: 0,
            height = readings?.height?.coerceAtLeast(0) ?: 0,
        )
    }

    /** The oldest buffered events, at most [max]. They stay buffered until acknowledged. */
    fun pendingEvents(max: Int): List<HLSPlayerEvent> = synchronized(lock) { events.take(max) }

    /**
     * Drops [sent] after a report carrying them was handled. Events already dropped because the
     * buffer was full are skipped.
     */
    fun acknowledgeEvents(sent: List<HLSPlayerEvent>) = synchronized(lock) {
        val handled = sent.toIdentitySet()
        while (events.isNotEmpty() && events.first() in handled) events.removeFirst()
    }

    private fun List<HLSPlayerEvent>.toIdentitySet(): Set<HLSPlayerEvent> =
        Collections.newSetFromMap(
            IdentityHashMap<HLSPlayerEvent, Boolean>(),
        ).also { it.addAll(this) }

    /** Stops counting: ends watch time and closes an open stall. Later states are ignored. */
    fun finish() = synchronized(lock) {
        if (finished) return
        val now = clock.elapsedMs()
        playingSinceMs?.let { watchMs += now - it }
        playingSinceMs = null
        endStall(now)
        finished = true
    }

    private fun startStall(now: Long) {
        stallSinceMs = now
        stallCount++
        addEvent(EVENT_STALL_START)
    }

    private fun endStall(now: Long) {
        val since = stallSinceMs ?: return
        val durationMs = (now - since).coerceAtLeast(0)
        stallMs += durationMs
        maxStallMs = maxOf(maxStallMs, durationMs)
        stallSinceMs = null
        addEvent(EVENT_STALL_END, mapOf("duration_ms" to durationMs))
    }

    private fun addEvent(type: String, data: Map<String, Any>? = null) {
        if (events.size >= MAX_BUFFERED_EVENTS) {
            events.removeFirst()
            logger.w { "[addEvent] Event buffer full, dropped the oldest event" }
        }
        events.addLast(HLSPlayerEvent(type = type, timestamp = clock.wallMs(), data = data))
    }

    private fun Long?.toIntMs(): Int = (this ?: 0L).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
}
