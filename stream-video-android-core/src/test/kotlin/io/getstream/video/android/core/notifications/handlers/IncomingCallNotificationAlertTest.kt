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

package io.getstream.video.android.core.notifications.handlers

import android.app.Notification
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.utils.BUILD_VERSION_CODES_CINNAMON_BUN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class IncomingCallNotificationAlertTest {

    @Test
    fun `Android 17 notification owns incoming ringtone when configuration is disabled`() {
        val notificationOwnsRingtone = shouldNotificationOwnIncomingRingtone(
            notificationRingtoneEnabled = false,
            sdkInt = BUILD_VERSION_CODES_CINNAMON_BUN,
        )

        assertEquals(true, notificationOwnsRingtone)
    }

    @Test
    fun `pre Android 17 respects notification ringtone configuration`() {
        val notificationOwnsRingtone = shouldNotificationOwnIncomingRingtone(
            notificationRingtoneEnabled = false,
            sdkInt = BUILD_VERSION_CODES_CINNAMON_BUN - 1,
        )

        assertEquals(false, notificationOwnsRingtone)
    }

    @Test
    fun `Telecom first makes notification own ringtone below Android 17`() {
        val notificationOwnsRingtone = shouldNotificationOwnIncomingRingtone(
            notificationRingtoneEnabled = false,
            telecomFirstEnabled = true,
            sdkInt = BUILD_VERSION_CODES_CINNAMON_BUN - 1,
        )

        assertEquals(true, notificationOwnsRingtone)
    }

    @Test
    fun `ringing notification loops and can alert again`() {
        val flags = incomingCallNotificationFlags(
            currentFlags = Notification.FLAG_ONLY_ALERT_ONCE,
            ringingState = RingingState.Incoming(acceptedByMe = false),
        )

        assertNotEquals(0, flags and Notification.FLAG_INSISTENT)
        assertEquals(0, flags and Notification.FLAG_ONLY_ALERT_ONCE)
    }

    @Test
    fun `accepted notification stops looping and alerts only once`() {
        val flags = incomingCallNotificationFlags(
            currentFlags = Notification.FLAG_INSISTENT,
            ringingState = RingingState.Incoming(acceptedByMe = true),
        )

        assertEquals(0, flags and Notification.FLAG_INSISTENT)
        assertNotEquals(0, flags and Notification.FLAG_ONLY_ALERT_ONCE)
    }
}
