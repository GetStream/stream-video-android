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

import android.app.ActivityManager
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.ClientState
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.core.notifications.internal.service.observers.CallServiceRingingStateObserver
import io.getstream.video.android.core.notifications.internal.telecom.TelecomCallController
import io.getstream.video.android.core.notifications.internal.telecom.TelecomHelper
import io.getstream.video.android.core.notifications.internal.telecom.TelecomPermissions
import io.getstream.video.android.core.notifications.internal.telecom.jetpack.JetpackTelecomRepository
import io.getstream.video.android.core.utils.BUILD_VERSION_CODES_CINNAMON_BUN
import io.getstream.video.android.model.StreamCallId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import kotlin.test.Test

/**
 * Focus on verifying key behaviors:
 * Whether Telecom integration starts under correct conditions.
 * Whether it’s skipped when conditions fail.
 * Whether service launchers are called correctly for showIncomingCall() and showOutgoingCall().
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class ServiceLauncherTest {
    private lateinit var context: Context
    private lateinit var telecomPermissions: TelecomPermissions
    private lateinit var telecomHelper: TelecomHelper
    private lateinit var incomingCallPresenter: IncomingCallPresenter
    private lateinit var streamVideo: StreamVideoClient
    private lateinit var serviceLauncher: ServiceLauncher
    private lateinit var notification: Notification
    private lateinit var callServiceConfig: CallServiceConfig
    private lateinit var callId: StreamCallId
    private lateinit var jetpackTelecomRepositoryProvider: JetpackTelecomRepositoryProvider
    private lateinit var jetpackTelecomRepository: JetpackTelecomRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        context = mockk(relaxed = true)
        telecomPermissions = mockk(relaxed = true)
        telecomHelper = mockk(relaxed = true)
        incomingCallPresenter = mockk(relaxed = true)
        streamVideo = mockk(relaxed = true)
        notification = mockk(relaxed = true)
        callServiceConfig = CallServiceConfig(enableTelecom = true)
        callId = StreamCallId("default", "123")
        jetpackTelecomRepositoryProvider = mockk(relaxed = true)
        jetpackTelecomRepository = mockk(relaxed = true)

        mockkStatic(ContextCompat::class)
        mockkObject(StreamVideo)
        mockkConstructor(JetpackTelecomRepository::class)
        mockkConstructor(JetpackTelecomRepositoryProvider::class)
        mockkConstructor(IncomingCallPresenter::class)
        mockkConstructor(CallServiceRingingStateObserver::class)
        mockkConstructor(TelecomCallController::class)
        mockkConstructor(TelecomPermissions::class)
        mockkConstructor(TelecomHelper::class)

        every {
            ContextCompat.checkSelfPermission(
                context,
                any(),
            )
        } returns PackageManager.PERMISSION_GRANTED
        every { anyConstructed<TelecomPermissions>().canUseTelecom(any(), any()) } returns true
        every { anyConstructed<TelecomHelper>().canUseJetpackTelecom() } returns true
        every {
            anyConstructed<IncomingCallPresenter>().showIncomingCall(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns ShowIncomingCallResult.FG_SERVICE
        every {
            anyConstructed<IncomingCallPresenter>().showIncomingCallNotification(
                any(),
                any(),
                any(),
            )
        } returns ShowIncomingCallResult.ONLY_NOTIFICATION
        every {
            anyConstructed<JetpackTelecomRepositoryProvider>().get(any())
        } returns jetpackTelecomRepository
        every { anyConstructed<TelecomCallController>().leaveCall(any()) } returns Unit
        every { anyConstructed<CallServiceRingingStateObserver>().observe(any()) } returns Unit

        every { StreamVideo.instanceOrNull() } returns streamVideo
        every { StreamVideo.instance() } returns streamVideo
        every { streamVideo.debugUseNotificationRingtoneForIncomingCalls } returns true
        every { jetpackTelecomRepositoryProvider.get(any()) } returns jetpackTelecomRepository
        coEvery {
            jetpackTelecomRepository.registerCall(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } coAnswers {
            arg<() -> Unit>(4).invoke()
        }

        serviceLauncher = ServiceLauncher(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // region showIncomingCall()

    @Test
    fun `showIncomingCall starts telecom registration when all conditions pass`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val call = mockk<Call>(relaxed = true)
        every { streamVideo.call(any(), any()) } returns call
        every { call.state } returns mockk(relaxed = true)
        every { call.scope } returns testScope

        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.getSystemService(
                context,
                TelecomManager::class.java,
            )
        } returns mockk()

        serviceLauncher.showIncomingCall(
            callId = callId,
            callDisplayName = "Test Caller",
            callServiceConfiguration = callServiceConfig,
            isVideo = true,
            payload = emptyMap(),
            streamVideo = streamVideo,
            notification = notification,
        )
        testScheduler.advanceUntilIdle()

        coVerify { jetpackTelecomRepository.registerCall(any(), any<Uri>(), true, any()) }
    }

    @Test
    fun `Android 17 incoming call registers Telecom and posts notification without starting service`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val callState = mockk<CallState>(relaxed = true)
        val call = mockk<Call>(relaxed = true) {
            every { state } returns callState
            every { scope } returns testScope
        }
        val clientState = mockk<ClientState>(relaxed = true)
        every { streamVideo.call(any(), any()) } returns call
        every { streamVideo.state } returns clientState
        serviceLauncher = createAndroid17ServiceLauncher()

        serviceLauncher.showIncomingCall(
            callId = callId,
            callDisplayName = "Test Caller",
            callServiceConfiguration = callServiceConfig,
            isVideo = true,
            payload = emptyMap(),
            streamVideo = streamVideo,
            notification = notification,
        )
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { ContextCompat.startForegroundService(any(), any()) }
        verify(exactly = 0) {
            anyConstructed<IncomingCallPresenter>().showIncomingCall(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
        verify {
            clientState.addRingingCall(call, any<RingingState.Incoming>())
            callState.updateRingingState()
            anyConstructed<IncomingCallPresenter>().showIncomingCallNotification(
                context,
                callId,
                notification,
            )
        }
        coVerify {
            jetpackTelecomRepository.registerCall(
                "Test Caller",
                any<Uri>(),
                true,
                true,
                any(),
            )
            streamVideo.connectIfNotAlreadyConnected()
        }
    }

    @Test
    fun `Android 17 connects WebSocket only after Telecom registration is confirmed`() = runTest {
        val onRegistered = slot<() -> Unit>()
        coEvery {
            jetpackTelecomRepository.registerCall(
                any(),
                any(),
                any(),
                any(),
                capture(onRegistered),
            )
        } returns Unit

        val callState = mockk<CallState>(relaxed = true)
        val call = mockk<Call>(relaxed = true) {
            every { state } returns callState
            every { scope } returns TestScope(StandardTestDispatcher(testScheduler))
        }
        every { streamVideo.call(any(), any()) } returns call
        every { streamVideo.state } returns mockk<ClientState>(relaxed = true)
        serviceLauncher = createAndroid17ServiceLauncher()

        serviceLauncher.showIncomingCall(
            callId = callId,
            callDisplayName = "Test Caller",
            callServiceConfiguration = callServiceConfig,
            isVideo = true,
            payload = emptyMap(),
            streamVideo = streamVideo,
            notification = notification,
        )
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) {
            anyConstructed<IncomingCallPresenter>().showIncomingCallNotification(
                any(),
                any(),
                any(),
            )
        }
        coVerify(exactly = 0) { streamVideo.connectIfNotAlreadyConnected() }

        onRegistered.captured.invoke()
        testScheduler.advanceUntilIdle()

        verify(exactly = 1) {
            anyConstructed<IncomingCallPresenter>().showIncomingCallNotification(
                context,
                callId,
                notification,
            )
        }
        coVerify(exactly = 1) { streamVideo.connectIfNotAlreadyConnected() }
        verify(exactly = 0) { ContextCompat.startForegroundService(any(), any()) }
        verify(exactly = 0) {
            anyConstructed<CallServiceRingingStateObserver>().observe(any())
        }
    }

    @Test
    fun `Android 17 notification ringtone disabled keeps coordinator and observes manual sound`() = runTest {
        val callState = mockk<CallState>(relaxed = true)
        val call = mockk<Call>(relaxed = true) {
            every { state } returns callState
            every { scope } returns TestScope(StandardTestDispatcher(testScheduler))
        }
        val clientState = mockk<ClientState>(relaxed = true)
        every { streamVideo.call(any(), any()) } returns call
        every { streamVideo.state } returns clientState
        every { streamVideo.debugUseNotificationRingtoneForIncomingCalls } returns false
        serviceLauncher = createAndroid17ServiceLauncher()

        serviceLauncher.showIncomingCall(
            callId = callId,
            callDisplayName = "Test Caller",
            callServiceConfiguration = callServiceConfig,
            isVideo = true,
            payload = emptyMap(),
            streamVideo = streamVideo,
            notification = notification,
        )
        testScheduler.advanceUntilIdle()

        verify {
            clientState.addRingingCall(call, any<RingingState.Incoming>())
            callState.updateRingingState()
            anyConstructed<IncomingCallPresenter>().showIncomingCallNotification(
                context,
                callId,
                notification,
            )
            anyConstructed<CallServiceRingingStateObserver>().observe(any())
        }
        verify(exactly = 0) {
            anyConstructed<IncomingCallPresenter>().showIncomingCall(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `Android 17 incoming removal cancels notification without starting service`() {
        val notificationManager = mockk<NotificationManagerCompat>(relaxed = true)
        mockkStatic(NotificationManagerCompat::class)
        every { NotificationManagerCompat.from(context) } returns notificationManager
        serviceLauncher = createAndroid17ServiceLauncher()

        serviceLauncher.removeIncomingCall(callId, callServiceConfig)

        verify {
            notificationManager.cancel(callId.getNotificationId(NotificationType.Incoming))
        }
        verify(exactly = 0) { context.startService(any()) }
    }

    @Test
    fun `Android 17 terminal cleanup unregisters Telecom when call service is disabled`() {
        val notificationManager = mockk<NotificationManagerCompat>(relaxed = true)
        val activityManager = mockk<ActivityManager>(relaxed = true)
        val call = mockk<Call>(relaxed = true) {
            every { type } returns callId.type
            every { cid } returns callId.cid
        }
        every { streamVideo.context } returns context
        every { streamVideo.callServiceConfigRegistry.get(callId.type) } returns
            callServiceConfig.copy(runCallServiceInForeground = false)
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        every { activityManager.getRunningServices(any()) } returns emptyList()
        mockkStatic(NotificationManagerCompat::class)
        every { NotificationManagerCompat.from(context) } returns notificationManager
        serviceLauncher = createAndroid17ServiceLauncher()

        serviceLauncher.stopService(call)

        verify {
            notificationManager.cancel(callId.getNotificationId(NotificationType.Incoming))
            anyConstructed<TelecomCallController>().leaveCall(call)
        }
        verify(exactly = 0) { context.stopService(any()) }
    }

    @Test
    fun `Android 17 falls back to pre Android 17 route when Telecom permissions fail`() = runTest {
        every { anyConstructed<TelecomPermissions>().canUseTelecom(any(), any()) } returns false
        serviceLauncher = createAndroid17ServiceLauncher()

        serviceLauncher.showIncomingCall(
            callId,
            "Test Caller",
            callServiceConfig,
            isVideo = false,
            payload = emptyMap(),
            streamVideo = streamVideo,
            notification = notification,
        )

        coVerify(exactly = 0) { jetpackTelecomRepository.registerCall(any(), any(), any(), any()) }
        verify(exactly = 1) {
            anyConstructed<IncomingCallPresenter>().showIncomingCall(
                context,
                callId,
                "Test Caller",
                callServiceConfig,
                notification,
            )
        }
    }

    //

    private fun createAndroid17ServiceLauncher(): ServiceLauncher {
        ReflectionHelpers.setStaticField(
            Build.VERSION::class.java,
            "SDK_INT",
            BUILD_VERSION_CODES_CINNAMON_BUN,
        )
        return ServiceLauncher(context)
    }

//    // endregion
//
//    // region showOutgoingCall()
//
    @Test
    fun `showOutgoingCall launches foreground service and registers telecom`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val call = mockk<Call>(relaxed = true)
        every { streamVideo.call(any(), any()) } returns call
        every { call.state } returns mockk(relaxed = true)
        every { call.scope } returns testScope

        every { streamVideo.callServiceConfigRegistry.get(any()) } returns callServiceConfig
        every { call.cid } returns "default:cid-123"
        every { call.isVideoEnabled() } returns true

        serviceLauncher.showOutgoingCall(call, "outgoing_call", streamVideo)

        verify { ContextCompat.startForegroundService(context, any<Intent>()) }

        testScheduler.advanceUntilIdle()

        coVerify {
            jetpackTelecomRepository.registerCall(
                any(),
                any(),
                false,
                true,
            )
        }
    }

    //
    @Test
    fun `showOutgoingCall skips telecom if permissions fail`() = runTest {
        val call = mockk<Call>(relaxed = true)
        every { streamVideo.callServiceConfigRegistry.get(any()) } returns callServiceConfig
        every { call.cid } returns "default:cid-123"
        every { call.isVideoEnabled() } returns true
        every { anyConstructed<TelecomPermissions>().canUseTelecom(any(), any()) } returns false

        serviceLauncher.showOutgoingCall(call, "outgoing_call", streamVideo)

        coVerify(exactly = 0) { jetpackTelecomRepository.registerCall(any(), any(), any(), any()) }
    }

    // endregion
}
