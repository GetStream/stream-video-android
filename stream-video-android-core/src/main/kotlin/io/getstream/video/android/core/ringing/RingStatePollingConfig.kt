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

package io.getstream.video.android.core.ringing

/**
 * When the caller falls back to reading the ring state instead of waiting for it.
 *
 * Polling is switched off by passing a null config, never by zeroing a timing: every value here
 * must be usable, and a non-positive one is rejected rather than quietly disabling the fallback.
 *
 * @param startAfterMs how long a ring must go without a participant's ring status changing before
 * polling begins. Kept well above the coordinator's websocket ping interval so a merely slow
 * answer does not poll.
 * @param intervalMs the gap between reads once polling has begun. Measured from the end of one
 * read to the start of the next, so a slow read widens the gap rather than being absorbed by it.
 */
public data class RingStatePollingConfig(
    val startAfterMs: Long = DEFAULT_START_AFTER_MS,
    val intervalMs: Long = DEFAULT_INTERVAL_MS,
) {
    init {
        // A non-positive interval turns the read loop into a spin against a rate limited
        // endpoint, for as long as the ring lasts. Fail where the value is set, not in the field.
        require(intervalMs > 0) { "intervalMs must be positive, was $intervalMs" }
        require(startAfterMs >= 0) { "startAfterMs cannot be negative, was $startAfterMs" }
    }

    public companion object {
        public const val DEFAULT_START_AFTER_MS: Long = 15_000
        public const val DEFAULT_INTERVAL_MS: Long = 5_000

        /**
         * The ring window assumed when the call's ring settings are not known yet.
         *
         * Matches the default `missed_call_timeout_ms`, so a ring whose settings have not
         * arrived is polled for as long as a default ring lasts rather than for the ceiling.
         *
         * Not configurable: it only applies before a call's ring settings arrive, which is a
         * state an integrator can neither observe nor reason about.
         */
        internal const val DEFAULT_RING_WINDOW_MS: Long = 30_000

        /**
         * The longest a single ring may be polled for, whatever the call's ring settings say.
         *
         * The polling window is otherwise the app's own `auto_cancel_timeout_ms`, which the app
         * chooses and can set to minutes. One read every [intervalMs] for that whole window,
         * multiplied by every ringing caller, is real load on a shard — and the endpoint is rate
         * limited per app, so it would be the customer's own allowance being spent.
         *
         * Deliberately not configurable: a ceiling an app can raise is a default, not a ceiling.
         */
        internal const val MAX_DURATION_MS: Long = 60_000
    }
}
