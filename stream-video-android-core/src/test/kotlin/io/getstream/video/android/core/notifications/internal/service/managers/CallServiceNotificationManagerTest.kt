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

package io.getstream.video.android.core.notifications.internal.service.managers

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationConfig
import io.getstream.video.android.core.notifications.dispatchers.NotificationDispatcher
import io.getstream.video.android.core.notifications.handlers.CompatibilityStreamNotificationHandler
import io.getstream.video.android.core.notifications.internal.StreamNotificationManager
import io.getstream.video.android.model.StreamCallId
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class CallServiceNotificationManagerTest {
    private lateinit var sut: CallServiceNotificationManager
    private lateinit var context: Context

    private val service: Service = mockk(relaxed = true)
    private val notification: Notification = mockk()
    private val callId = StreamCallId("default", "call-123")

    private val notificationDispatcher: NotificationDispatcher = mockk(relaxed = true)
    private val notificationManagerCompat: NotificationManagerCompat = mockk(relaxed = true)

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sut = CallServiceNotificationManager()

        mockkStatic(ActivityCompat::class)
        mockkStatic(ContextCompat::class)
        mockkStatic(NotificationManagerCompat::class)
        mockkObject(StreamVideo.Companion)

        every { NotificationManagerCompat.from(service) } returns notificationManagerCompat
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `justNotify dispatches notification when permission is granted`() {
        every {
            ContextCompat.checkSelfPermission(any(), Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_GRANTED

        val streamVideo = mockk<StreamVideo>(relaxed = true)
        every { StreamVideo.instanceOrNull() } returns streamVideo
        every { streamVideo.getStreamNotificationDispatcher() } returns notificationDispatcher

        sut.justNotify(
            service = service,
            callId = callId,
            notificationId = 1001,
            notification = notification,
        )

        verify {
            notificationDispatcher.notify(callId, 1001, notification)
        }
    }

    @Test
    fun `justNotify does nothing when permission is denied`() {
        every {
            ContextCompat.checkSelfPermission(any(), Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_DENIED

        val streamVideo = mockk<StreamVideo>(relaxed = true)
        every { StreamVideo.instanceOrNull() } returns streamVideo
        every { streamVideo.getStreamNotificationDispatcher() } returns notificationDispatcher

        sut.justNotify(
            service = service,
            callId = callId,
            notificationId = 1001,
            notification = notification,
        )

        verify { notificationDispatcher wasNot Called }
    }

    @Test
    fun `justNotify is safe when StreamVideo instance is null`() {
        every {
            ContextCompat.checkSelfPermission(any(), any())
        } returns PackageManager.PERMISSION_GRANTED

        every { StreamVideo.instanceOrNull() } returns null

        sut.justNotify(service, callId, 1001, notification)

        // Should not crash
    }

    @Test
    fun `cancelNotifications cancels call notifications`() {
        val streamVideoClient = mockk<StreamVideoClient> {}

        every { StreamVideo.instanceOrNull() } returns streamVideoClient

        every { streamVideoClient.scope } returns TestScope()

        val call = Call(streamVideoClient, callId.type, callId.id, mockk())
        every { streamVideoClient.call(callId.type, callId.id) } returns call
        every { StreamVideo.instanceOrNull() } returns streamVideoClient

        sut.cancelNotifications(service, callId)

        verify {
            notificationManagerCompat.cancel(callId.hashCode())
            call.state.notificationId?.let {
                notificationManagerCompat.cancel(it)
            }
        }
    }

    @Test
    fun `cancelNotifications clears media session`() {
        val handler = mockk<CompatibilityStreamNotificationHandler>(relaxed = true)

        val notificationConfig = mockk<NotificationConfig> {
            every { notificationHandler } returns handler
        }

        val streamNotificationManager = mockk<StreamNotificationManager> {
            every { this@mockk.notificationConfig } returns notificationConfig
        }

        val streamVideoClient = mockk<StreamVideoClient> {
            every { this@mockk.streamNotificationManager } returns streamNotificationManager
        }

        every { streamVideoClient.scope } returns TestScope()

        val call = Call(streamVideoClient, callId.type, callId.id, mockk())
        every { streamVideoClient.call(callId.type, callId.id) } returns call
        every { StreamVideo.instanceOrNull() } returns streamVideoClient

        sut.cancelNotifications(service, callId)

        verify {
            handler.clearMediaSession(callId)
        }
    }

    @Test
    fun `cancelNotifications is safe when StreamVideo is null`() {
        every { StreamVideo.instanceOrNull() } returns null

        sut.cancelNotifications(service, callId)

        verify {
            notificationManagerCompat.cancel(any())
        }
    }
}
