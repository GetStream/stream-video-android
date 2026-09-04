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

package io.getstream.video.android.core.notifications.internal.service.incomingcallcoordinator

import android.app.Notification
import android.content.Context
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.IncomingRingtoneOwner
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.handlers.shouldNotificationOwnIncomingRingtone
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.IncomingCallPresenter
import io.getstream.video.android.core.notifications.internal.service.IncomingCallRequest
import io.getstream.video.android.core.notifications.internal.service.JetpackTelecomRepositoryProvider
import io.getstream.video.android.core.notifications.internal.service.ServiceIntentBuilder
import io.getstream.video.android.core.notifications.internal.service.ShowIncomingCallResult
import io.getstream.video.android.core.notifications.internal.service.models.ServiceRoute
import io.getstream.video.android.core.notifications.internal.telecom.TelecomConfig
import io.getstream.video.android.core.notifications.internal.telecom.TelecomHelper
import io.getstream.video.android.core.notifications.internal.telecom.TelecomPermissions
import io.getstream.video.android.core.notifications.internal.telecom.jetpack.JetpackTelecomRepository
import io.getstream.video.android.model.StreamCallId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals

class PreAndroid17IncomingCallCoordinatorTest {

    private val context = mockk<Context>(relaxed = true)
    private val client = mockk<StreamVideoClient>(relaxed = true)
    private val presenter = mockk<IncomingCallPresenter>()
    private val serviceIntentBuilder = mockk<ServiceIntentBuilder>(relaxed = true)
    private val telecomPermissions = mockk<TelecomPermissions>()
    private val telecomHelper = mockk<TelecomHelper>()
    private val repositoryProvider = mockk<JetpackTelecomRepositoryProvider>()
    private val repository = mockk<JetpackTelecomRepository>()
    private val call = mockk<Call>()
    private val callState = mockk<CallState>(relaxed = true)
    private val notification = mockk<Notification>()
    private val callId = StreamCallId("default", "call-id")
    private lateinit var callScope: TestScope
    private lateinit var coordinator: PreAndroid17IncomingCallCoordinator

    @Before
    fun setup() {
        mockkStatic("io.getstream.video.android.core.notifications.handlers.ChannelInfoProviderKt")
        every { shouldNotificationOwnIncomingRingtone() } returns false
        callScope = TestScope(StandardTestDispatcher())
        every { client.call(callId.type, callId.id) } returns call
        every { client.telecomConfig } returns TelecomConfig("stream")
        every { call.state } returns callState
        every { call.scope } returns callScope
        every { callState.jetpackTelecomRepository } returns null
        every { presenter.showIncomingCall(any(), any(), any(), any(), any()) } returns
            ShowIncomingCallResult.ERROR
        every { telecomPermissions.canUseTelecom(any(), any()) } returns false
        every { telecomHelper.canUseJetpackTelecom() } returns false
        coordinator = PreAndroid17IncomingCallCoordinator(
            context,
            client,
            presenter,
            serviceIntentBuilder,
            telecomPermissions,
            telecomHelper,
            repositoryProvider,
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `pre Android 17 route requests legacy ringtone and marks CallService ownership`() {
        var selectedOwner: IncomingRingtoneOwner? = null

        coordinator.showIncomingCall(request { selectedOwner = it })

        assertEquals(IncomingRingtoneOwner.Legacy, selectedOwner)
        verify { callState.updateServiceRoute(ServiceRoute.LEGACY_CALL_SERVICE) }
        verify {
            presenter.showIncomingCall(
                context,
                callId,
                "Caller",
                any(),
                notification,
            )
        }
    }

    @Test
    fun `Android 17 fallback retains notification ringtone ownership`() {
        var selectedOwner: IncomingRingtoneOwner? = null

        coordinator.showIncomingCall(
            request = request { selectedOwner = it },
            ringtoneOwner = IncomingRingtoneOwner.Notification,
        )

        assertEquals(IncomingRingtoneOwner.Notification, selectedOwner)
        verify { callState.updateServiceRoute(ServiceRoute.LEGACY_CALL_SERVICE) }
    }

    @Test
    fun `foreground service path reuses existing Telecom repository`() = runTest {
        every { presenter.showIncomingCall(any(), any(), any(), any(), any()) } returns
            ShowIncomingCallResult.FG_SERVICE
        every { telecomPermissions.canUseTelecom(any(), context) } returns true
        every { telecomHelper.canUseJetpackTelecom() } returns true
        every { callState.jetpackTelecomRepository } returns repository
        coEvery { repository.registerCall(any(), any(), true, true) } just runs

        coordinator.showIncomingCall(request())
        callScope.advanceUntilIdle()

        verify(exactly = 0) { repositoryProvider.get(any()) }
        coVerify { repository.registerCall("Caller", any(), true, true) }
        verify {
            callState.updateNotification(
                callId.getNotificationId(
                    io.getstream.video.android.core.notifications.NotificationType.Incoming,
                ),
                notification,
            )
        }
    }

    private fun request(
        notificationProvider: (IncomingRingtoneOwner) -> Unit = {},
    ): IncomingCallRequest = IncomingCallRequest(
        callId = callId,
        callDisplayName = "Caller",
        callServiceConfiguration = CallServiceConfig(enableTelecom = true),
        isVideo = true,
        payload = emptyMap(),
        notificationProvider = { owner ->
            notificationProvider(owner)
            notification
        },
    )
}
