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

import android.os.Build
import android.os.Looper
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class DebouncerTest {

    private lateinit var debouncer: Debouncer
    private val mainLooper = Looper.getMainLooper()

    @Before
    fun setup() {
        debouncer = Debouncer()
    }

    @Test
    fun `action executes after delay`() {
        var executed = false

        debouncer.submit(1_000) {
            executed = true
        }

        // Advance time less than delay
        Shadows.shadowOf(mainLooper).idleFor(999, TimeUnit.MILLISECONDS)
        assertFalse(executed)

        // Advance to full delay
        Shadows.shadowOf(mainLooper).idleFor(1, TimeUnit.MILLISECONDS)
        assertTrue(executed)
    }

    @Test
    fun `previous action is cancelled when new submit happens`() {
        var firstExecuted = false
        var secondExecuted = false

        debouncer.submit(1_000) {
            firstExecuted = true
        }

        // Submit again before delay expires
        debouncer.submit(1_000) {
            secondExecuted = true
        }

        Shadows.shadowOf(mainLooper).idleFor(1_000, TimeUnit.MILLISECONDS)

        assertFalse(firstExecuted)
        assertTrue(secondExecuted)
    }

    @Test
    fun `only last submitted action runs`() {
        var executedCount = 0

        repeat(5) {
            debouncer.submit(1_000) {
                executedCount++
            }
        }

        Shadows.shadowOf(mainLooper).idleFor(1_000, TimeUnit.MILLISECONDS)

        assertEquals(1, executedCount)
    }
}
