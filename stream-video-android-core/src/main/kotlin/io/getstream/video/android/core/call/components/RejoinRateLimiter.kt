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

package io.getstream.video.android.core.call.components

/**
 * Bounds how many REJOIN and MIGRATE attempts a call may make inside a rolling time window.
 *
 * Both strategies issue a new backend join, so a flapping network that keeps escalating to
 * REJOIN would otherwise drive an unbounded stream of joins. Once [maxAttempts] have been
 * registered inside [windowMs], [tryRegister] refuses further attempts until older ones age out.
 * Attempts spaced further apart than the window are always allowed.
 */
internal class RejoinRateLimiter(
    private val maxAttempts: Int = MAX_ATTEMPTS,
    private val windowMs: Long = WINDOW_MS,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val attempts = ArrayDeque<Long>()

    /** Records an attempt and returns `true`, or returns `false` if the window is already full. */
    @Synchronized
    fun tryRegister(): Boolean {
        val timestamp = now()
        val cutoff = timestamp - windowMs
        while (attempts.isNotEmpty() && attempts.first() < cutoff) {
            attempts.removeFirst()
        }
        if (attempts.size >= maxAttempts) return false
        attempts.addLast(timestamp)
        return true
    }

    /** Clears all recorded attempts, giving a fresh join a full budget. */
    @Synchronized
    fun reset() {
        attempts.clear()
    }

    companion object {
        const val MAX_ATTEMPTS = 10
        const val WINDOW_MS = 120_000L
    }
}
