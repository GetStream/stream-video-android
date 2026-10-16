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

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RejoinRateLimiterTest {

    private var now = 1_000_000L
    private val limiter = RejoinRateLimiter(maxAttempts = 3, windowMs = 10_000L, now = { now })

    @Test
    fun `allows attempts up to the limit inside the window`() {
        repeat(3) { assertThat(limiter.tryRegister()).isTrue() }
    }

    @Test
    fun `rejects the attempt past the limit inside the window`() {
        repeat(3) { limiter.tryRegister() }
        now += 9_999L

        assertThat(limiter.tryRegister()).isFalse()
    }

    @Test
    fun `a rejected attempt does not consume budget`() {
        repeat(3) { limiter.tryRegister() }
        now += 5_000L
        assertThat(limiter.tryRegister()).isFalse()

        // The accepted attempts have aged out; the rejected one, had it been recorded, would not.
        now += 5_001L

        repeat(3) { assertThat(limiter.tryRegister()).isTrue() }
    }

    @Test
    fun `an attempt exactly one window old still counts`() {
        repeat(3) { limiter.tryRegister() }
        now += 10_000L

        assertThat(limiter.tryRegister()).isFalse()
    }

    @Test
    fun `attempts older than the window stop counting`() {
        repeat(3) {
            limiter.tryRegister()
            now += 1_000L
        }
        now += 8_000L

        assertThat(limiter.tryRegister()).isTrue()
        assertThat(limiter.tryRegister()).isFalse()
    }

    @Test
    fun `reset restores the full budget`() {
        repeat(3) { limiter.tryRegister() }

        limiter.reset()

        repeat(3) { assertThat(limiter.tryRegister()).isTrue() }
    }

    @Test
    fun `defaults are ten attempts per two minutes`() {
        assertThat(RejoinRateLimiter.MAX_ATTEMPTS).isEqualTo(10)
        assertThat(RejoinRateLimiter.WINDOW_MS).isEqualTo(120_000L)
    }
}
