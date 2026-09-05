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

package io.getstream.video.android.core.e2ee

import java.util.concurrent.ConcurrentHashMap

/**
 * Rate-limits encryption event traces, per event kind and track.
 *
 * Native reports failures such as [E2EEEventType.DECRYPTION_FAILED] per undecodable frame, while
 * the trace buffer carrying them to the SFU only drains on the stats interval. Unthrottled, one
 * badly-keyed track would fill that buffer with near-identical entries. Repeats are counted rather
 * than dropped, so their volume still reaches the SFU on the next trace that gets through.
 */
internal class E2EETraceThrottle(
    private val intervalMs: Long,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {

    private class Window(val emittedAt: Long, var suppressed: Int)

    private val windows = ConcurrentHashMap<String, Window>()

    /**
     * Decides whether [event] should be traced.
     *
     * @return the number of events suppressed since the last one that got through, or `null` when
     * this event is itself suppressed.
     */
    fun admit(event: E2EEEvent): Int? {
        val key = "${event.type}:${event.userId}:${event.trackType}"
        val now = currentTimeMillis()
        var suppressed: Int? = null
        // compute() is atomic per key, so concurrent events for one track cannot both be admitted
        // or lose a suppressed count.
        windows.compute(key) { _, window ->
            if (window == null || now - window.emittedAt >= intervalMs) {
                suppressed = window?.suppressed ?: 0
                Window(now, 0)
            } else {
                window.suppressed++
                window
            }
        }
        return suppressed
    }

    fun clear() = windows.clear()
}
