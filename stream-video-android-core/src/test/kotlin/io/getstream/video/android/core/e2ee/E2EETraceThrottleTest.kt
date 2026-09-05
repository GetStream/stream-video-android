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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the rate limit on encryption event traces. Native can report a decryption failure per
 * frame, so an unthrottled listener would fill the trace buffer between two stats uploads.
 */
class E2EETraceThrottleTest {

    private var now = 0L
    private val throttle = E2EETraceThrottle(intervalMs = 2_000L) { now }

    private fun event(
        type: E2EEEventType = E2EEEventType.DECRYPTION_FAILED,
        userId: String? = "alice",
        trackType: E2EETrackType? = E2EETrackType.VIDEO,
    ) = E2EEEvent(
        type = type,
        name = type.name,
        userId = userId,
        trackType = trackType,
        keyIndex = null,
        version = null,
        reason = null,
        keyState = null,
        encodePerformance = emptyList(),
        decodePerformance = emptyList(),
    )

    @Test
    fun `admits the first event with nothing suppressed`() {
        assertEquals(0, throttle.admit(event()))
    }

    @Test
    fun `suppresses repeats inside the window`() {
        throttle.admit(event())

        now += 500
        assertNull(throttle.admit(event()))
        now += 500
        assertNull(throttle.admit(event()))
    }

    @Test
    fun `carries the suppressed count on the next admitted event`() {
        throttle.admit(event())
        repeat(5) {
            now += 100
            throttle.admit(event())
        }

        now = 2_000
        assertEquals(5, throttle.admit(event()))
    }

    @Test
    fun `starts counting again after an event gets through`() {
        throttle.admit(event())
        now += 100
        throttle.admit(event())

        now = 2_000
        assertEquals(1, throttle.admit(event()))

        // Nothing was suppressed during the second window, so the third trace reports none.
        now = 4_000
        assertEquals(0, throttle.admit(event()))
    }

    @Test
    fun `admits the event again once the window has elapsed`() {
        throttle.admit(event())

        now = 1_999
        assertNull(throttle.admit(event()))
        now = 2_000
        assertEquals(1, throttle.admit(event()))
    }

    @Test
    fun `throttles each event type separately`() {
        assertEquals(0, throttle.admit(event(type = E2EEEventType.DECRYPTION_FAILED)))
        assertEquals(0, throttle.admit(event(type = E2EEEventType.MISSING_KEY)))
    }

    @Test
    fun `throttles each track separately`() {
        assertEquals(0, throttle.admit(event(trackType = E2EETrackType.VIDEO)))
        assertEquals(0, throttle.admit(event(trackType = E2EETrackType.AUDIO)))
    }

    @Test
    fun `throttles each participant separately`() {
        // One participant on the wrong key must not hide another going quiet at the same moment.
        assertEquals(0, throttle.admit(event(userId = "alice")))
        assertEquals(0, throttle.admit(event(userId = "bob")))
    }

    @Test
    fun `admits again after being cleared`() {
        throttle.admit(event())

        throttle.clear()

        assertEquals(0, throttle.admit(event()))
    }
}
