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

package io.getstream.video.android.core.notifications.internal.service.observers

import android.app.Notification
import android.content.Context
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.ClientState
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.notifications.NotificationConfig
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.core.notifications.dispatchers.DefaultNotificationDispatcher
import io.getstream.video.android.core.notifications.internal.NotificationUpdateDeduplicator
import io.getstream.video.android.core.notifications.internal.StreamNotificationManager
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.core.notifications.internal.service.permissions.ForegroundServicePermissionManager
import io.getstream.video.android.core.utils.isAndroid17OrHigher
import io.getstream.video.android.model.StreamCallId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CallServiceNotificationUpdateObserverTest {

    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)

    private lateinit var call: Call
    private lateinit var callState: CallState
    private lateinit var streamVideo: StreamVideoClient
    private lateinit var streamState: ClientState
    private lateinit var permissionManager: ForegroundServicePermissionManager
    private lateinit var notificationConfig: NotificationConfig
    private lateinit var notificationUpdateDeduplicator: NotificationUpdateDeduplicator
    private lateinit var observer: CallServiceNotificationUpdateObserver

    private val context: Context = mockk(relaxed = true)
    private val notification: Notification = mockk()
    private val onStartService =
        mockk<(Int, Notification, String, Int) -> Unit>(relaxed = true)

    // StateFlows
    private val ringingStateFlow = MutableStateFlow<RingingState>(RingingState.Idle)
    private val membersFlow = MutableStateFlow(emptyList<MemberState>())
    private val testNotificationIdFlow: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val remoteParticipantsFlow = MutableStateFlow(emptyList<ParticipantState>())
    private val backstageFlow = MutableStateFlow(false)
    private val atomicNotification = AtomicReference<Notification?>(null)

    // Captured callback
    private var startArgs: Quadruple<Int, Notification, String, Int>? = null

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        mockkStatic("io.getstream.video.android.core.utils.AndroidVersionCodesKt")
        every { isAndroid17OrHigher() } returns false

        callState = mockk {
            every { ringingState } returns ringingStateFlow
            every { members } returns membersFlow
            every { remoteParticipants } returns remoteParticipantsFlow
            every { backstage } returns backstageFlow
            every { notificationIdFlow } returns testNotificationIdFlow
            every {
                atomicNotification
            } returns this@CallServiceNotificationUpdateObserverTest.atomicNotification
        }

        call = mockk {
            every { id } returns "call-1"
            every { type } returns "default"
            every { cid } returns "default:call-1"
            every { state } returns callState
        }

        streamState = mockk(relaxed = true)
        notificationUpdateDeduplicator = mockk(relaxed = true)
        notificationConfig = mockk {
            every { notificationUpdateTriggers(call) } returns null
            every { incomingRingingNotificationUpdateDelayMillis } returns 1_300L
        }
        val streamNotificationManager = mockk<StreamNotificationManager> {
            every { this@mockk.notificationConfig } returns
                this@CallServiceNotificationUpdateObserverTest.notificationConfig
            every { notificationUpdateDeduplicator } returns
                this@CallServiceNotificationUpdateObserverTest.notificationUpdateDeduplicator
        }

        streamVideo = mockk {
            every { state } returns streamState
            coEvery { onCallNotificationUpdate(call) } returns notification
            every { this@mockk.streamNotificationManager } returns streamNotificationManager
        }

        permissionManager = mockk {
            every { getServiceType(any(), any()) } returns 42
        }

        observer = CallServiceNotificationUpdateObserver(
            call = call,
            streamVideo = streamVideo,
            scope = testScope.backgroundScope,
            permissionManager = permissionManager,
            onStartService = { id, notif, trigger, type ->
                startArgs = Quadruple(id, notif, trigger, type)
                onStartService(id, notif, trigger, type)
            },
        )
    }

    @After
    fun tearDown() {
        unmockkStatic("io.getstream.video.android.core.utils.AndroidVersionCodesKt")
        Dispatchers.resetMain()
    }

    data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
    )

    @Test
    fun `incoming ringing state starts incoming foreground notification`() = runTest {
        observer.observe(context)

        ringingStateFlow.value = RingingState.Incoming()
        advanceUntilIdle()
        advanceTimeBy(100L)

        val args = startArgs!!
        assertEquals(
            StreamCallId("default", "call-1")
                .getNotificationId(NotificationType.Incoming),
            args.first,
        )
        assertEquals(notification, args.second)
        assertEquals(CallService.TRIGGER_INCOMING_CALL, args.third)
        assertEquals(42, args.fourth)
    }

    @Test
    fun `duplicate incoming notification update is skipped`() = runTest {
        every { isAndroid17OrHigher() } returns true
        every {
            notificationUpdateDeduplicator.isDuplicate(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns true
        testNotificationIdFlow.value = 123
        atomicNotification.set(notification)
        ringingStateFlow.value = RingingState.Incoming()

        observer.observe(context)
        runCurrent()

        assertNull(startArgs)
        verify(exactly = 1) {
            notificationUpdateDeduplicator.isDuplicate(
                call,
                any(),
                123,
                notification,
                123,
                notification,
            )
        }
        verify(exactly = 0) {
            notificationConfig.incomingRingingNotificationUpdateDelayMillis
        }
        verify(exactly = 0) { permissionManager.getServiceType(any(), any()) }
        verify(exactly = 0) { onStartService(any(), any(), any(), any()) }
    }

    @Test
    fun `non duplicate incoming notification update is delayed`() = runTest {
        every { isAndroid17OrHigher() } returns true
        testNotificationIdFlow.value = 123
        atomicNotification.set(notification)
        ringingStateFlow.value = RingingState.Incoming()

        observer.observe(context)
        runCurrent()

        assertNull(startArgs)

        advanceTimeBy(1_299L)
        assertNull(startArgs)

        advanceTimeBy(1L)
        runCurrent()
        assertNotNull(startArgs)
    }

    @Test
    fun `null notification id does not disable delay for later incoming update`() = runTest {
        every { isAndroid17OrHigher() } returns true
        observer.observe(context)
        runCurrent()

        ringingStateFlow.value = RingingState.Incoming()
        advanceTimeBy(100L)
        runCurrent()

        assertNotNull(startArgs)

        startArgs = null
        testNotificationIdFlow.value = 123
        membersFlow.value = listOf(mockk(relaxed = true))

        advanceTimeBy(1_299L)
        assertNull(startArgs)

        advanceTimeBy(1L)
        runCurrent()
        assertNotNull(startArgs)
    }

    @Test
    fun `outgoing ringing state starts outgoing foreground notification`() = runTest {
        observer.observe(context)
        advanceUntilIdle()

        ringingStateFlow.value = RingingState.Outgoing()
        advanceUntilIdle()
        advanceTimeBy(100L)

        val args = startArgs!!
        assertEquals(
            StreamCallId("default", "call-1")
                .getNotificationId(NotificationType.Outgoing),
            args.first,
        )
        assertEquals(CallService.TRIGGER_OUTGOING_CALL, args.third)
    }

    @Test
    fun `active ringing state dispatches ongoing call notification`() = runTest {
        val notificationDispatcher = mockk<DefaultNotificationDispatcher>(relaxed = true)
        val mockNotification = mockk<Notification>()

        every { streamVideo.getStreamNotificationDispatcher() } returns notificationDispatcher
        coEvery { streamVideo.onCallNotificationUpdate(call) } returns mockNotification

        observer.observe(context)

        advanceUntilIdle()

        ringingStateFlow.value = RingingState.Active

        advanceUntilIdle()
        advanceTimeBy(100)

        val streamCallId = StreamCallId("default", "call-1")

        verify {
            notificationDispatcher.notify(
                streamCallId,
                streamCallId.getNotificationId(NotificationType.Ongoing),
                mockNotification,
            )
        }
    }

    @Test
    fun `no notification generated does not start foreground service`() = runTest {
        coEvery { streamVideo.onCallNotificationUpdate(call) } returns null

        observer.observe(context)
        advanceUntilIdle()

        ringingStateFlow.value = RingingState.Incoming()
        advanceUntilIdle()
        advanceTimeBy(100L)

        assertNull(startArgs)
    }
}
