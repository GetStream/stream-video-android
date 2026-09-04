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

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.ClientState
import io.getstream.video.android.core.IncomingRingtoneOwner
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.internal.service.CallServiceConfig
import io.getstream.video.android.core.notifications.internal.service.IncomingCallPresenter
import io.getstream.video.android.core.notifications.internal.service.IncomingCallRequest
import io.getstream.video.android.core.notifications.internal.service.JetpackTelecomRepositoryProvider
import io.getstream.video.android.core.notifications.internal.service.ShowIncomingCallResult
import io.getstream.video.android.core.notifications.internal.service.models.ServiceRoute
import io.getstream.video.android.core.notifications.internal.service.observers.CallRejectionObserver
import io.getstream.video.android.core.notifications.internal.service.observers.CallServiceEventObserver
import io.getstream.video.android.core.notifications.internal.telecom.TelecomCallController
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
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class Android17IncomingCallCoordinatorTest {

    private val context = mockk<Context>(relaxed = true)
    private val client = mockk<StreamVideoClient>(relaxed = true)
    private val presenter = mockk<IncomingCallPresenter>()
    private val telecomPermissions = mockk<TelecomPermissions>()
    private val telecomHelper = mockk<TelecomHelper>()
    private val repositoryProvider = mockk<JetpackTelecomRepositoryProvider>()
    private val fallbackCoordinator = mockk<PreAndroid17IncomingCallCoordinator>(relaxed = true)
    private val telecomCallController = mockk<TelecomCallController>(relaxed = true)
    private val repository = mockk<JetpackTelecomRepository>()
    private val call = mockk<Call>()
    private val callState = mockk<CallState>(relaxed = true)
    private val clientState = mockk<ClientState>(relaxed = true)
    private val notification = mockk<Notification>()
    private val callId = StreamCallId("default", "call-id")
    private lateinit var callScope: TestScope
    private lateinit var coordinator: Android17IncomingCallCoordinator

    @Before
    fun setup() {
        mockkStatic(ContextCompat::class)
        mockkConstructor(CallRejectionObserver::class)
        mockkConstructor(CallServiceEventObserver::class)
        every { anyConstructed<CallRejectionObserver>().observe() } just runs
        every { anyConstructed<CallServiceEventObserver>().observe(any(), any()) } just runs
        callScope = TestScope(StandardTestDispatcher())
        every { telecomPermissions.canUseTelecom(context) } returns true
        every { telecomHelper.canUseJetpackTelecom() } returns true
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } returns PackageManager.PERMISSION_GRANTED
        every { client.call(callId.type, callId.id) } returns call
        every { client.telecomConfig } returns TelecomConfig("stream")
        every { client.state } returns clientState
        every { client.enableCallNotificationUpdates } returns false
        every { call.state } returns callState
        every { call.scope } returns callScope
        every { callState.jetpackTelecomRepository } returns null
        every { repositoryProvider.get(callId) } returns repository
        every { presenter.showIncomingCallNotification(any(), any(), any()) } returns
            ShowIncomingCallResult.ONLY_NOTIFICATION
        coEvery { client.connectIfNotAlreadyConnected() } just runs
        coordinator = Android17IncomingCallCoordinator(
            context,
            client,
            presenter,
            telecomPermissions,
            telecomHelper,
            repositoryProvider,
            fallbackCoordinator,
            telecomCallController,
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `falls back with notification ringtone when Telecom is unavailable`() {
        every { telecomPermissions.canUseTelecom(context) } returns false
        val request = request()

        coordinator.showIncomingCall(request)

        verify {
            fallbackCoordinator.showIncomingCall(
                request,
                IncomingRingtoneOwner.Notification,
            )
        }
        coVerify(exactly = 0) { repository.registerCall(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `registers Telecom before creating and posting ringing notification`() {
        val events = mutableListOf<String>()
        val request = request { owner ->
            assertEquals(IncomingRingtoneOwner.Notification, owner)
            events += "notification"
        }
        coEvery {
            repository.registerCall(any(), any(), true, true, any(), any())
        } coAnswers {
            events += "registered"
            arg<() -> Unit>(4).invoke()
        }

        coordinator.showIncomingCall(request)
        callScope.advanceUntilIdle()

        assertEquals(listOf("registered", "notification"), events)
        verify { callState.updateServiceRoute(ServiceRoute.TELECOM) }
        verify { callState.jetpackTelecomRepository = repository }
        verify { presenter.showIncomingCallNotification(context, callId, notification) }
        verify { clientState.addRingingCall(call, any()) }
        coVerify { client.connectIfNotAlreadyConnected() }
    }

    @Test
    fun `Telecom registration failure falls back to CallService`() {
        val request = request()
        val failure = IllegalStateException("registration failed")
        coEvery {
            repository.registerCall(any(), any(), any(), any(), any(), any())
        } coAnswers {
            arg<(Exception) -> Unit>(5).invoke(failure)
        }

        coordinator.showIncomingCall(request)
        callScope.advanceUntilIdle()

        verify {
            fallbackCoordinator.showIncomingCall(
                request,
                IncomingRingtoneOwner.Notification,
            )
        }
    }

    private fun request(
        onNotificationRequested: (IncomingRingtoneOwner) -> Unit = {},
    ): IncomingCallRequest = IncomingCallRequest(
        callId = callId,
        callDisplayName = "Caller",
        callServiceConfiguration = CallServiceConfig(enableTelecom = true),
        isVideo = true,
        payload = emptyMap(),
        notificationProvider = { owner ->
            onNotificationRequested(owner)
            notification
        },
    )
}
