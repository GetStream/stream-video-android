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

package io.getstream.video.android.core.notifications.internal

import android.app.Notification
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.notifications.NotificationUpdateComparator
import io.getstream.video.android.core.utils.BUILD_VERSION_CODES_CINNAMON_BUN
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationUpdateDeduplicatorTest {
    private val call = mockk<Call>(relaxed = true)
    private val existing = mockk<Notification>()
    private val updated = mockk<Notification>()
    private val comparator = mockk<NotificationUpdateComparator>()
    private val deduplicator = NotificationUpdateDeduplicator(comparator)

    @Test
    fun `equivalent unaccepted incoming update with same id is duplicate`() {
        val state = RingingState.Incoming()
        every { comparator.areEquivalent(call, state, existing, updated) } returns true
        assertTrue(
            deduplicator.isDuplicate(
                call,
                state,
                1,
                existing,
                1,
                updated,
                BUILD_VERSION_CODES_CINNAMON_BUN,
            ),
        )
    }

    @Test
    fun `accepted incoming update is never duplicate`() {
        assertFalse(
            deduplicator.isDuplicate(
                call,
                RingingState.Incoming(acceptedByMe = true),
                1,
                existing,
                1,
                updated,
                BUILD_VERSION_CODES_CINNAMON_BUN,
            ),
        )
        verify(exactly = 0) { comparator.areEquivalent(any(), any(), any(), any()) }
    }

    @Test
    fun `equivalent update below Android 17 is not duplicate`() {
        assertFalse(
            deduplicator.isDuplicate(
                call,
                RingingState.Incoming(),
                1,
                existing,
                1,
                updated,
                BUILD_VERSION_CODES_CINNAMON_BUN - 1,
            ),
        )
        verify(exactly = 0) { comparator.areEquivalent(any(), any(), any(), any()) }
    }
}
