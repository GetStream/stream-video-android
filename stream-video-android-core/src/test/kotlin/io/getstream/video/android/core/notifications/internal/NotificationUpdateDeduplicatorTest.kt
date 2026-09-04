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
import android.os.Build
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.notifications.NotificationUpdateComparator
import io.getstream.video.android.core.utils.BUILD_VERSION_CODES_CINNAMON_BUN
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.util.ReflectionHelpers

class NotificationUpdateDeduplicatorTest {

    private var originalSdkInt: Int = 0

    private val call = mockk<Call>(relaxed = true)
    private val existing = mockk<Notification>()
    private val updated = mockk<Notification>()
    private val comparator = mockk<NotificationUpdateComparator>()
    private val deduplicator = NotificationUpdateDeduplicator(comparator)

    @Before
    fun setUp() {
        originalSdkInt = Build.VERSION.SDK_INT
        setSdkInt(BUILD_VERSION_CODES_CINNAMON_BUN)
    }

    @After
    fun tearDown() {
        setSdkInt(originalSdkInt)
    }

    @Test
    fun `equivalent unaccepted incoming update with the same id is duplicate`() {
        val ringingState = RingingState.Incoming()
        every { comparator.areEquivalent(call, ringingState, existing, updated) } returns true

        val result = deduplicator.isDuplicate(
            call = call,
            ringingState = ringingState,
            existingNotificationId = 1,
            existingNotification = existing,
            updatedNotificationId = 1,
            updatedNotification = updated,
        )

        assertTrue(result)
    }

    @Test
    fun `different notification ids are not duplicate`() {
        val result = deduplicator.isDuplicate(
            call = call,
            ringingState = RingingState.Incoming(),
            existingNotificationId = 1,
            existingNotification = existing,
            updatedNotificationId = 2,
            updatedNotification = updated,
        )

        assertFalse(result)
        verify(exactly = 0) { comparator.areEquivalent(any(), any(), any(), any()) }
    }

    @Test
    fun `accepted incoming update is not duplicate`() {
        val result = deduplicator.isDuplicate(
            call = call,
            ringingState = RingingState.Incoming(acceptedByMe = true),
            existingNotificationId = 1,
            existingNotification = existing,
            updatedNotificationId = 1,
            updatedNotification = updated,
        )

        assertFalse(result)
        verify(exactly = 0) { comparator.areEquivalent(any(), any(), any(), any()) }
    }

    @Test
    fun `comparison failure publishes the update`() {
        val ringingState = RingingState.Incoming()
        every {
            comparator.areEquivalent(call, ringingState, existing, updated)
        } throws IllegalStateException("comparison failed")

        val result = deduplicator.isDuplicate(
            call = call,
            ringingState = ringingState,
            existingNotificationId = 1,
            existingNotification = existing,
            updatedNotificationId = 1,
            updatedNotification = updated,
        )

        assertFalse(result)
    }

    @Test
    fun `equivalent update below Android 17 is not duplicate`() {
        setSdkInt(BUILD_VERSION_CODES_CINNAMON_BUN - 1)
        val ringingState = RingingState.Incoming()

        val result = deduplicator.isDuplicate(
            call = call,
            ringingState = ringingState,
            existingNotificationId = 1,
            existingNotification = existing,
            updatedNotificationId = 1,
            updatedNotification = updated,
        )

        assertFalse(result)
        verify(exactly = 0) { comparator.areEquivalent(any(), any(), any(), any()) }
    }

    private fun setSdkInt(sdkInt: Int) {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdkInt)
    }
}
