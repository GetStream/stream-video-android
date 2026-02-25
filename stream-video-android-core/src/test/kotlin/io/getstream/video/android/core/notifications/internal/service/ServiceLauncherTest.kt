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

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.internal.telecom.TelecomHelper
import io.getstream.video.android.core.notifications.internal.telecom.TelecomPermissions
import io.getstream.video.android.core.notifications.internal.telecom.jetpack.JetpackTelecomRepository
import io.getstream.video.android.model.StreamCallId
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
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
            anyConstructed<JetpackTelecomRepositoryProvider>().get(any())
        } returns jetpackTelecomRepository

        every { StreamVideo.instanceOrNull() } returns streamVideo
        every { StreamVideo.instance() } returns streamVideo
        every { jetpackTelecomRepositoryProvider.get(any()) } returns jetpackTelecomRepository

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
            context = context,
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
    fun `showIncomingCall skips telecom when permissions fail`() = runTest {
        every { anyConstructed<TelecomPermissions>().canUseTelecom(any(), any()) } returns false

        serviceLauncher.showIncomingCall(
            context,
            callId,
            "Test Caller",
            callServiceConfig,
            isVideo = false,
            payload = emptyMap(),
            streamVideo = streamVideo,
            notification = notification,
        )

        coVerify(exactly = 0) { jetpackTelecomRepository.registerCall(any(), any(), any(), any()) }
    }

    //
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

        serviceLauncher.showOutgoingCall(
            call,
            CallService.Companion.Trigger.OutgoingCall,
            streamVideo,
        )

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

        serviceLauncher.showOutgoingCall(
            call,
            CallService.Companion.Trigger.OutgoingCall,
            streamVideo,
        )

        coVerify(exactly = 0) { jetpackTelecomRepository.registerCall(any(), any(), any(), any()) }
    }

    // endregion
}
