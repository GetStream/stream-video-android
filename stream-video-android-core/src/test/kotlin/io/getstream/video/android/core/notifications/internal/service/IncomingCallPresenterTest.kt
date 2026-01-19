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
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import io.getstream.video.android.core.ClientState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.dispatchers.DefaultNotificationDispatcher
import io.getstream.video.android.model.StreamCallId
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class IncomingCallPresenterTest {

    private lateinit var context: Context
    private lateinit var serviceIntentBuilder: ServiceIntentBuilder
    private lateinit var presenter: IncomingCallPresenter
    private lateinit var callServiceConfig: CallServiceConfig
    private lateinit var notification: Notification
    private lateinit var streamVideoClient: StreamVideoClient

    private val callId = StreamCallId("default", "123", "default:123")
    private val serviceClass = CallService::class.java
    private val config = CallServiceConfig(serviceClass = serviceClass)

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        context = mockk(relaxed = true)
        serviceIntentBuilder = mockk(relaxed = true)
        callServiceConfig = CallServiceConfig(enableTelecom = true)
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

    @Test
    fun `returns FG_SERVICE when no active call`() {
        // given
        mockNoActiveCall()

        every {
            serviceIntentBuilder.buildStartIntent(any(), any())
        } returns Intent()

        // when
        val result = presenter.showIncomingCall(
            context = context,
            callId = callId,
            callDisplayName = "Test",
            callServiceConfiguration = config,
            notification = mockk(),
        )

        // then
        assertEquals(ShowIncomingCallResult.FG_SERVICE, result)

        verify {
            ContextCompat.startForegroundService(context, any())
        }
    }

    @Test
    fun `returns ONLY_NOTIFICATION when active call and service already running`() {
        // given
        mockActiveCall()
        every { serviceIntentBuilder.isServiceRunning(any(), any()) } returns true
        mockNotificationPermission(granted = true)

        val dispatcher = mockk<DefaultNotificationDispatcher>(relaxed = true)
        every {
            StreamVideo.instanceOrNull()?.getStreamNotificationDispatcher()
        } returns dispatcher

        // when
        val result = presenter.showIncomingCall(
            context,
            callId,
            "Test",
            config,
            mockk(),
        )

        // then
        assertEquals(ShowIncomingCallResult.ONLY_NOTIFICATION, result)

        verify {
            dispatcher.notify(any(), any(), any())
        }
    }

    @Test
    fun `returns SERVICE when active call and service not running`() {
        // given
        mockActiveCall()
        every { serviceIntentBuilder.isServiceRunning(any(), any()) } returns false

        every {
            serviceIntentBuilder.buildStartIntent(any(), any())
        } returns Intent()

        // when
        val result = presenter.showIncomingCall(
            context,
            callId,
            "Test",
            config,
            mockk(),
        )

        // then
        assertEquals(ShowIncomingCallResult.SERVICE, result)

        verify {
            context.startService(any())
        }
    }

    @Test
    fun `returns ONLY_NOTIFICATION on exception but has notification`() {
        // given
        mockNoActiveCall()
        mockNotificationPermission(granted = true)

        every {
            serviceIntentBuilder.buildStartIntent(any(), any())
        } throws RuntimeException("Boom")

        val dispatcher = mockk<DefaultNotificationDispatcher>(relaxed = true)
        every {
            StreamVideo.instanceOrNull()?.getStreamNotificationDispatcher()
        } returns dispatcher

        // when
        val result = presenter.showIncomingCall(
            context,
            callId,
            "Test",
            config,
            mockk(),
        )

        // then
        assertEquals(ShowIncomingCallResult.ONLY_NOTIFICATION, result)

        verify {
            dispatcher.notify(any(), any(), any())
        }
    }

    @Test
    fun `returns ERROR when notification permission missing`() {
        // given
        mockNoActiveCall()
        mockNotificationPermission(granted = false)

        every {
            serviceIntentBuilder.buildStartIntent(any(), any())
        } throws RuntimeException("Boom")

        // when
        val result = presenter.showIncomingCall(
            context,
            callId,
            "Test",
            config,
            mockk(),
        )

        // then
        assertEquals(ShowIncomingCallResult.ERROR, result)
    }

    // ---------- helpers ----------

    private fun mockNoActiveCall() {
        val state = mockk<ClientState> {
            every { activeCall.value } returns null
        }
        every { StreamVideo.instanceOrNull()?.state } returns state
    }

    private fun mockActiveCall() {
        val state = mockk<ClientState> {
            every { activeCall.value } returns mockk()
        }
        every { StreamVideo.instanceOrNull()?.state } returns state
    }

    private fun mockNotificationPermission(granted: Boolean) {
        every {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        } returns if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
    }
}
