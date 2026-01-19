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

import io.getstream.android.video.generated.models.CallEndedEvent
import io.getstream.android.video.generated.models.LocalCallAcceptedPostEvent
import io.getstream.android.video.generated.models.LocalCallRejectedPostEvent
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.ClientState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.RingingState
import io.getstream.video.android.core.StreamVideoClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertTrue

class CallServiceEventObserverTest {

    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)

    private lateinit var call: Call
    private lateinit var streamVideo: StreamVideoClient
    private val streamState = mockk<ClientState>(relaxed = true)
    private lateinit var observer: CallServiceEventObserver

    private val eventsFlow = MutableSharedFlow<VideoEvent>(replay = 1)
    private val connectionFlow =
        MutableStateFlow<RealtimeConnection>(RealtimeConnection.Connected)

    private val ringingStateFlow =
        MutableStateFlow<RingingState>(RingingState.Idle)

    private val activeCallFlow = MutableStateFlow<Call?>(null)
    private val ringingCallFlow = MutableStateFlow<Call?>(null)

    private lateinit var onServiceStop: () -> Unit
    var onServiceStopInvoked = false

    private lateinit var onRemoveIncoming: () -> Unit
    var onRemoveIncomingInvoked = false

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)

        call = mockk(relaxed = true) {
            every { scope } returns testScope
            every { events } returns eventsFlow
            every { id } returns "call-1"
        }

        val callState = mockk<CallState>(relaxed = true) {
            every { ringingState } returns ringingStateFlow
            every { connection } returns connectionFlow
        }

        every { call.state } returns callState

        with(streamState) {
            every { activeCall } returns activeCallFlow
            every { ringingCall } returns ringingCallFlow
        }

        streamVideo = mockk(relaxed = true) {
            every { userId } returns "me"
            every { state } returns streamState
        }

        onServiceStop = {
            onServiceStopInvoked = true
        }
        onRemoveIncoming = {
            onRemoveIncomingInvoked = true
        }

        observer = CallServiceEventObserver(call, streamVideo, testScope)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `accepted by me on another device while ringing stops service`() = runTest {
        ringingStateFlow.value = RingingState.Incoming()

        observer.observe(onServiceStop, onRemoveIncoming)

        eventsFlow.emit(
            LocalCallAcceptedPostEvent(
                "",
                mockk(),
                mockk(relaxed = true),
                mockk(relaxed = true) {
                    every { id } returns "me"
                },
                "",
            ),
        )

        advanceUntilIdle()
        assertTrue(onServiceStopInvoked)
    }

    @Test
    fun `rejected by me with no active call stops service`() = runTest {
        activeCallFlow.value = null

        observer.observe(onServiceStop, onRemoveIncoming)

        eventsFlow.emit(
            LocalCallRejectedPostEvent(
                "",
                mockk(relaxed = true),
                mockk(relaxed = true) {
                    every { createdBy } returns mockk(relaxed = true)
                },
                mockk(relaxed = true) {
                    every { id } returns "me"
                },
                "",
                "",
            ),
        )

        advanceUntilIdle()
        assertTrue(onServiceStopInvoked)
    }

    @Test
    fun `rejected by caller with active call removes incoming`() = runTest {
        activeCallFlow.value = mockk()

        observer.observe(onServiceStop, onRemoveIncoming)

        eventsFlow.emit(
            LocalCallRejectedPostEvent(
                "",
                mockk(relaxed = true),
                mockk(relaxed = true) {
                    every { createdBy } returns mockk(relaxed = true)
                },
                mockk(relaxed = true),
                "",
                "",
            ),
        )

        advanceUntilIdle()
        assertFalse(onServiceStopInvoked)
        assertTrue(onRemoveIncomingInvoked)
    }

    @Test // next
    fun `call ended stops service`() = runTest {
        observer.observe(onServiceStop, onRemoveIncoming)

        eventsFlow.emit(CallEndedEvent("", mockk(), mockk(), ""))

        advanceUntilIdle()
        assertTrue(onServiceStopInvoked)
    }

    @Test
    fun `connection failure for ringing call cleans up`() = runTest {
        ringingCallFlow.value = call

        observer.observe(onServiceStop, onRemoveIncoming)

        connectionFlow.value = RealtimeConnection.Failed(Throwable("network"))

        advanceUntilIdle()

        verify {
            streamState.removeRingingCall(call)
            streamVideo.onCallCleanUp(call)
        }
    }
}
