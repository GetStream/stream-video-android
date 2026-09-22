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

import android.app.NotificationManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class RingingNotificationChannelTest {

    @Test
    fun `ringing channel contains ringtone sound attributes and vibration`() {
        val notificationManager = mockk<NotificationManagerCompat>()
        val createdChannel = slot<NotificationChannelCompat>()
        every {
            notificationManager.createNotificationChannel(capture(createdChannel))
        } just runs
        val soundUri = Uri.parse("android.resource://test/ringtone")
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val vibrationPattern = longArrayOf(0L, 300L, 200L, 300L)
        val channelInfo = StreamNotificationChannelInfo(
            id = "incoming-ringing",
            name = "Incoming calls",
            description = "Incoming call alerts",
            importance = NotificationManager.IMPORTANCE_HIGH,
        )

        channelInfo.createRingingChannel(
            manager = notificationManager,
            soundUri = soundUri,
            audioAttributes = audioAttributes,
            vibrationPattern = vibrationPattern,
        )

        assertEquals("incoming-ringing", createdChannel.captured.id)
        assertEquals(soundUri, createdChannel.captured.sound)
        assertEquals(
            AudioAttributes.USAGE_NOTIFICATION_RINGTONE,
            createdChannel.captured.audioAttributes?.usage,
        )
        assertEquals(vibrationPattern.toList(), createdChannel.captured.vibrationPattern?.toList())
    }
}
