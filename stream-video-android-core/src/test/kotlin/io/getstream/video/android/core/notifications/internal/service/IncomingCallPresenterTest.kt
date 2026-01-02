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

package io.getstream.video.android.core.notifications.internal.service

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.core.notifications.dispatchers.NotificationDispatcher
import io.getstream.video.android.model.StreamCallId
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class IncomingCallPresenterTest {

    private lateinit var context: Context
    private lateinit var serviceIntentBuilder: ServiceIntentBuilder
    private lateinit var presenter: IncomingCallPresenter
    private lateinit var callServiceConfig: CallServiceConfig
    private lateinit var callId: StreamCallId
    private lateinit var notification: Notification
    private lateinit var streamVideoClient: StreamVideoClient

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        serviceIntentBuilder = mockk(relaxed = true)
        callServiceConfig = CallServiceConfig(enableTelecom = true)
        callId = StreamCallId("default", "123")
        notification = mockk(relaxed = true)
        streamVideoClient = mockk(relaxed = true)

        mockkObject(StreamVideo)
        mockkStatic(ContextCompat::class)

        every { StreamVideo.instanceOrNull() } returns streamVideoClient
        every { StreamVideo.instance() } returns streamVideoClient

        presenter = IncomingCallPresenter(serviceIntentBuilder)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // region 1️⃣ Foreground service branch (no active call)

    @Test
    fun `when no active call should start foreground service and return FG_SERVICE`() {
        // Given no active call
        every { StreamVideo.instanceOrNull()?.state?.activeCall?.value } returns null
        every {
            ContextCompat.startForegroundService(context, any())
        } returns mockk(relaxed = true)

        // When
        val result = presenter.showIncomingCall(
            context = context,
            callId = callId,
            callDisplayName = "Caller",
            callServiceConfiguration = callServiceConfig,
            notification = notification,
        )

        // Then
        verify { ContextCompat.startForegroundService(context, any()) }
        Assert.assertEquals(ShowIncomingCallResult.FG_SERVICE, result)
    }

    // endregion

    // region 2️⃣ Normal service branch (active call exists)

    @Test
    fun `when active call exists should start normal service and return SERVICE`() {
        every { StreamVideo.instanceOrNull()?.state?.activeCall?.value } returns mockk(relaxed = true)

        val intent = mockk<android.content.Intent>(relaxed = true)
        every { serviceIntentBuilder.buildStartIntent(any(), any()) } returns intent

        val result = presenter.showIncomingCall(
            context = context,
            callId = callId,
            callDisplayName = "TestCaller",
            callServiceConfiguration = callServiceConfig,
            notification = notification,
        )

        verify { context.startService(any()) }
        Assert.assertEquals(ShowIncomingCallResult.SERVICE, result)
    }

    // endregion

    // region 3️⃣ Error branch (service start fails → fallback to notification)

    @Test
    fun `when service start fails and permission granted should show notification`() {
        every { streamVideoClient.state.activeCall.value } returns null

        val notificationDispatcher = mockk<NotificationDispatcher>(relaxed = true)
        every { streamVideoClient.getStreamNotificationDispatcher() } returns notificationDispatcher

        // Force exception inside safeCallWithResult
        every {
            ContextCompat.startForegroundService(context, any())
        } throws RuntimeException("service fail")

        // Mock permission granted
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_GRANTED

        val result = presenter.showIncomingCall(
            context,
            callId,
            "Caller",
            callServiceConfig,
            notification,
        )

        verify {
            notificationDispatcher.notify(
                callId,
                callId.getNotificationId(NotificationType.Incoming),
                notification,
            )
        }

        Assert.assertEquals(ShowIncomingCallResult.ONLY_NOTIFICATION, result)
    }

    // endregion

    // region 4️⃣ Error branch (service start fails, no permission)

    @Test
    fun `when service start fails and no permission should return ERROR`() {
        every { streamVideoClient.state.activeCall.value } returns null

        every {
            ContextCompat.startForegroundService(context, any())
        } throws RuntimeException("fail")

        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_DENIED

        val result = presenter.showIncomingCall(
            context,
            callId,
            "Caller",
            callServiceConfig,
            notification,
        )

        verify(exactly = 0) {
            streamVideoClient.getStreamNotificationDispatcher().notify(any(), any(), any())
        }

        Assert.assertEquals(ShowIncomingCallResult.ERROR, result)
    }
}
