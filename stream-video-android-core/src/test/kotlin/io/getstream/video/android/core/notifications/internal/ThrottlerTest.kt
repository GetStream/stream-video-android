/*
 * Copyright (c) 2014-2025 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.notifications.internal

import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThrottlerTest {

    @Before
    fun setup() {
        Throttler.resetAll()
    }

    @Test
    fun `first throttle call executes immediately`() {
        var executed = false

        Throttler.throttleFirst("key", 1_000) {
            executed = true
        }

        assertTrue(executed)
    }

    @Test
    fun `second call within cooldown does not execute`() {
        var count = 0

        Throttler.throttleFirst("key", 10_000) {
            count++
        }

        Throttler.throttleFirst("key", 10_000) {
            count++
        }

        assertEquals(1, count)
    }

    @Test
    fun `reset allows execution again`() {
        var count = 0

        Throttler.throttleFirst("key", 10_000) {
            count++
        }

        Throttler.reset("key")

        Throttler.throttleFirst("key", 10_000) {
            count++
        }

        assertEquals(2, count)
    }

    @Test
    fun `resetAll clears all cooldowns`() {
        var count = 0

        Throttler.throttleFirst("key1", 10_000) { count++ }
        Throttler.throttleFirst("key2", 10_000) { count++ }

        Throttler.resetAll()

        Throttler.throttleFirst("key1", 10_000) { count++ }
        Throttler.throttleFirst("key2", 10_000) { count++ }

        assertEquals(4, count)
    }

    @Test
    fun `throttleFirst without key throttles same call site`() {
        var count = 0

        Throttler.throttleFirst(10_000) { count++ }
        Throttler.throttleFirst(10_000) { count++ }

        assertEquals(1, count)
    }
}
