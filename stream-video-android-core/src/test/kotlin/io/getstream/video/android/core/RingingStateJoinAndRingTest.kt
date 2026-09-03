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

package io.getstream.video.android.core

import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.video.android.core.base.TestBase
import io.getstream.video.android.core.base.toResponse
import io.getstream.video.android.core.events.JoinCallResponseEvent
import io.getstream.video.android.core.events.ParticipantCount
import io.getstream.video.android.core.utils.toResponse
import io.getstream.video.android.model.User
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertTrue

/**
 * Regression tests for the ringing state during the join-and-ring flow.
 *
 * In join-and-ring the SFU join response sets the ringing state to [RingingState.Outgoing]
 * directly, but the ring request that registers the call in `client.state.ringingCall` completes
 * later. A coordinator event landing in that window (e.g. `call.session_started`) recomputes the
 * ringing state with `hasRingingCall = false` and used to downgrade Outgoing back to Idle, leaving
 * the caller stuck on the loading UI (AND-1454).
 */
@RunWith(RobolectricTestRunner::class)
internal class RingingStateJoinAndRingTest : TestBase() {

    // Coroutines launched by CallState land here instead of the global uncaught handler,
    // where the test framework would attribute them to whichever runTest enters next.
    private val uncaughtExceptions = CopyOnWriteArrayList<Throwable>()
    private val scope = CoroutineScope(
        SupervisorJob() +
            dispatcherRule.testDispatcher +
            CoroutineExceptionHandler { _, e -> uncaughtExceptions += e },
    )

    private val user = User(id = "caller", createdAt = nowUtc, updatedAt = nowUtc)

    private val activeCall = MutableStateFlow<Call?>(null)
    private val ringingCall = MutableStateFlow<Call?>(null)

    private val clientState = mockk<ClientState>(relaxed = true) {
        every { activeCall } returns this@RingingStateJoinAndRingTest.activeCall
        every { ringingCall } returns this@RingingStateJoinAndRingTest.ringingCall
    }
    private val client = mockk<StreamVideoClient>(relaxed = true) {
        every { userId } returns this@RingingStateJoinAndRingTest.user.id
        every { state } returns clientState
    }
    private val call = mockk<Call>(relaxed = true) {
        every { type } returns "default"
        every { id } returns "join-and-ring-test"
        every { cid } returns "default:join-and-ring-test"
        // A real flow: SharedFlow.collect returns Nothing, so collecting the relaxed
        // mock would throw KotlinNothingValueException from CallState's sorter coroutine.
        every { events } returns MutableSharedFlow<VideoEvent>()
    }

    @After
    fun tearDownScope() {
        scope.cancel()
        assertTrue(
            uncaughtExceptions.isEmpty(),
            "CallState coroutines threw: $uncaughtExceptions",
        )
    }

    private fun callStateInJoinAndRingWindow(): CallState {
        val callState = CallState(client, call, user, scope)
        // The call was created by us; nobody accepted or rejected yet.
        callState.updateFromResponse(call.toResponse(user.toResponse()))
        // joinAndRing() toggled the flag and the join completed (active call registered),
        // but the ring request has not completed yet, so ringingCall is still null.
        callState.toggleJoinAndRingProgress(true)
        activeCall.value = call
        // The SFU join response transitions the ringing state to Outgoing directly.
        callState.handleEvent(
            JoinCallResponseEvent(
                callState = stream.video.sfu.models.CallState(),
                participantCount = ParticipantCount(total = 1, anonymous = 0),
                fastReconnectDeadlineSeconds = 0,
                isReconnected = false,
                publishOptions = emptyList(),
            ),
        )
        assertTrue(callState.ringingState.value is RingingState.Outgoing)
        return callState
    }

    @Test
    fun `a recompute before the SFU join sets Outgoing still yields Idle`() {
        val callState = CallState(client, call, user, scope)
        callState.updateFromResponse(call.toResponse(user.toResponse()))
        callState.toggleJoinAndRingProgress(true)
        activeCall.value = call

        // Join-and-ring is in progress but nothing set Outgoing yet, so the guard must not
        // apply and the state stays Idle (the UI legitimately shows the loading screen).
        callState.updateRingingState()

        assertTrue(callState.ringingState.value is RingingState.Idle)
    }

    @Test
    fun `an event arriving before the ring request completes keeps the Outgoing state`() {
        val callState = callStateInJoinAndRingWindow()

        // A coordinator event (e.g. call.session_started) recomputes the ringing state while
        // ringingCall is still null. It used to downgrade Outgoing -> Idle.
        callState.updateRingingState()

        assertTrue(callState.ringingState.value is RingingState.Outgoing)
    }

    @Test
    fun `recomputing after the ring request registers the ringing call yields Outgoing`() {
        val callState = callStateInJoinAndRingWindow()
        // Simulate the clobber the old code produced, so the recovery path is exercised even
        // if the guard above changes.
        callState.updateRingingState()

        // joinAndRing() registers the ringing call on ring success and recomputes.
        ringingCall.value = call
        callState.updateRingingState()

        val state = callState.ringingState.value
        assertTrue(state is RingingState.Outgoing && !state.acceptedByCallee)
    }
}
