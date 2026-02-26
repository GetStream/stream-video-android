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
import io.getstream.video.android.core.notifications.NotificationType
import io.getstream.video.android.core.notifications.internal.service.CallService
import io.getstream.video.android.core.notifications.internal.service.permissions.ForegroundServicePermissionManager
import io.getstream.video.android.model.StreamCallId
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
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
    private lateinit var observer: CallServiceNotificationUpdateObserver

    private val context: Context = mockk(relaxed = true)
    private val notification: Notification = mockk()

    // StateFlows
    private val ringingStateFlow = MutableStateFlow<RingingState>(RingingState.Idle)
    private val membersFlow = MutableStateFlow(emptyList<MemberState>())
    private val testNotificationIdFlow: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val remoteParticipantsFlow = MutableStateFlow(emptyList<ParticipantState>())
    private val backstageFlow = MutableStateFlow(false)

    // Captured callback
    private var startArgs: Quadruple<Int, Notification, CallService.Companion.Trigger, Int>? = null

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)

        callState = mockk {
            every { ringingState } returns ringingStateFlow
            every { members } returns membersFlow
            every { remoteParticipants } returns remoteParticipantsFlow
            every { backstage } returns backstageFlow
            every { notificationIdFlow } returns testNotificationIdFlow
        }

        call = mockk {
            every { id } returns "call-1"
            every { type } returns "default"
            every { cid } returns "default:call-1"
            every { state } returns callState
        }

        streamState = mockk(relaxed = true)

        streamVideo = mockk {
            every { state } returns streamState
            coEvery { onCallNotificationUpdate(call) } returns notification
            every { streamNotificationManager } returns mockk {
                every { notificationConfig } returns mockk {
                    every { notificationUpdateTriggers(call) } returns null
                }
            }
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
            },
        )
    }

    @After
    fun tearDown() {
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
//        advanceUntilIdle()

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
        assertEquals(CallService.Companion.Trigger.IncomingCall, args.third)
        assertEquals(42, args.fourth)
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
        assertEquals(CallService.Companion.Trigger.OutgoingCall, args.third)
    }

    @Test
    fun `active ringing state starts ongoing foreground notification`() = runTest {
        observer.observe(context)
        advanceUntilIdle()

        ringingStateFlow.value = RingingState.Active
        advanceUntilIdle()
        advanceTimeBy(100L)

        val args = startArgs!!
        assertEquals(
            StreamCallId("default", "call-1")
                .getNotificationId(NotificationType.Ongoing),
            args.first,
        )
        assertEquals(CallService.Companion.Trigger.OnGoingCall, args.third)
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
