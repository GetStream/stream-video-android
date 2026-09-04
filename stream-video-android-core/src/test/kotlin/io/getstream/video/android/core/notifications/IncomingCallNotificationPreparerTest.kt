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

package io.getstream.video.android.core.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import io.getstream.video.android.core.IncomingRingtoneOwner
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.sounds.RingingCallVibrationConfig
import io.getstream.video.android.core.sounds.RingingConfig
import io.getstream.video.android.core.sounds.Sounds
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class IncomingCallNotificationPreparerTest {

    private lateinit var context: Context
    private lateinit var streamVideo: StreamVideoClient
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var preparer: IncomingCallNotificationPreparer
    private val soundUri = Uri.parse("android.resource://test/ringtone")
    private val vibrationPattern = longArrayOf(0L, 300L, 200L, 300L)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        streamVideo = mockk()
        notificationManager = mockk(relaxed = true)
        val ringingConfig = mockk<RingingConfig> {
            every { incomingCallSoundUri } returns soundUri
        }
        every { streamVideo.context } returns context
        every { streamVideo.sounds } returns mockk<Sounds> {
            every { this@mockk.ringingConfig } returns ringingConfig
        }
        every { streamVideo.vibrationConfig } returns mockk<RingingCallVibrationConfig> {
            every { enabled } returns true
            every { vibratePattern } returns vibrationPattern
        }
        preparer = IncomingCallNotificationPreparer(streamVideo, notificationManager)
    }

    @Test
    fun `legacy owner returns original notification without creating ringing channel`() {
        val notification = notification("incoming")

        val result = preparer.prepare(
            notification,
            IncomingRingtoneOwner.Legacy,
            RingingState.Incoming(),
        )

        assertSame(notification, result)
        verify(exactly = 0) {
            notificationManager.createNotificationChannel(any<NotificationChannelCompat>())
        }
    }

    @Test
    fun `notification owner creates ringing channel using existing channel ID`() {
        val existingChannel = NotificationChannelCompat.Builder(
            "incoming-ringing",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
            .setName("Existing incoming calls")
            .setDescription("Existing description")
            .build()
        every {
            notificationManager.getNotificationChannelCompat("incoming-ringing")
        } returns existingChannel
        val createdChannel = slot<NotificationChannelCompat>()
        every {
            notificationManager.createNotificationChannel(capture(createdChannel))
        } just runs

        val result = preparer.prepare(
            notification("incoming-ringing"),
            IncomingRingtoneOwner.Notification,
            RingingState.Incoming(),
        )

        assertEquals("incoming-ringing", NotificationCompat.getChannelId(result))
        assertEquals("incoming-ringing", createdChannel.captured.id)
        assertEquals("Existing incoming calls", createdChannel.captured.name)
        assertEquals(soundUri, createdChannel.captured.sound)
        assertEquals(
            AudioAttributes.USAGE_NOTIFICATION_RINGTONE,
            createdChannel.captured.audioAttributes?.usage,
        )
        assertEquals(vibrationPattern.toList(), createdChannel.captured.vibrationPattern?.toList())
        assertEquals(Notification.FLAG_INSISTENT, result.flags and Notification.FLAG_INSISTENT)
    }

    @Test
    fun `notification owner requires a channel ID`() {
        assertFailsWith<IllegalArgumentException> {
            preparer.prepare(
                Notification(),
                IncomingRingtoneOwner.Notification,
                RingingState.Incoming(),
            )
        }
    }

    private fun notification(channelId: String): Notification =
        NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .build()
}
