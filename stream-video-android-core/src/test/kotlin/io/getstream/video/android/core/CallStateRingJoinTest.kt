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

import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.GetCallRingStateResponse
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.result.Result
import io.getstream.video.android.core.base.TestBase
import io.getstream.video.android.core.base.toResponse
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.ringing.RingJoinSource
import io.getstream.video.android.core.utils.toResponse
import io.getstream.video.android.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertTrue

/**
 * What an outgoing ring does with an accept it learned by polling.
 *
 * The join itself is the mocked boundary: these cover the decision to join, and what the ring's
 * two watchdogs do around it. A join that fails must leave the ring exactly as armed as it found
 * it, or an accepted ring ends up with no timer and no reader and nothing can resolve it.
 */
@RunWith(RobolectricTestRunner::class)
internal class CallStateRingJoinTest : TestBase() {

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
        every { activeCall } returns this@CallStateRingJoinTest.activeCall
        every { ringingCall } returns this@CallStateRingJoinTest.ringingCall
    }
    private val client = mockk<StreamVideoClient>(relaxed = true) {
        every { userId } returns this@CallStateRingJoinTest.user.id
        every { state } returns clientState
    }
    private val call = mockk<Call>(relaxed = true) {
        every { type } returns "default"
        every { id } returns "ring-join-test"
        every { cid } returns "default:ring-join-test"
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

    private val at: OffsetDateTime = OffsetDateTime.of(2026, 9, 16, 12, 0, 0, 0, ZoneOffset.UTC)

    private fun ringState(acceptedBy: Map<String, OffsetDateTime>) = GetCallRingStateResponse(
        callCid = "default:ring-join-test",
        createdByUserId = user.id,
        duration = "0ms",
        sessionId = "session-1",
        acceptedBy = acceptedBy,
        rejectedBy = emptyMap(),
        missedBy = emptyMap(),
    )

    /** An outgoing ring we started, registered as ringing, with nobody having answered yet. */
    private fun outgoingRing(): CallState {
        ringingCall.value = call
        val callState = CallState(client, call, user, scope)
        callState.updateFromResponse(call.toResponse(user.toResponse()))
        assertThat(callState.ringingState.value)
            .isEqualTo(RingingState.Outgoing(acceptedByCallee = false))
        return callState
    }

    private fun joinReturns(result: Result<RtcSession>) {
        coEvery { call.join(any(), any(), any(), any(), any(), any()) } returns result
    }

    @Test
    fun `a polled accept on our own ring joins the call`() {
        joinReturns(Result.Success(mockk(relaxed = true)))
        val callState = outgoingRing()

        callState.updateFromRingState(ringState(acceptedBy = mapOf("callee" to at)))

        assertThat(callState.ringingState.value)
            .isEqualTo(RingingState.Outgoing(acceptedByCallee = true))
        verify { call.setJoinSource(RingJoinSource.PollApi) }
        coVerify { call.join(any(), any(), any(), any(), any(), any()) }
    }

    /**
     * The regression this pairs with: seeing the accept used to disarm both watchdogs, so a join
     * that then failed left the ring with nothing running to resolve it.
     */
    @Test
    fun `a failed join leaves the ring watchdogs armed`() {
        joinReturns(Result.Failure(io.getstream.result.Error.GenericError("no sfu")))
        val callState = outgoingRing()

        callState.updateFromRingState(ringState(acceptedBy = mapOf("callee" to at)))

        assertThat(callState.ringingState.value)
            .isEqualTo(RingingState.Outgoing(acceptedByCallee = true))
        verify(exactly = 0) { call.stopRingStatePolling() }
    }

    @Test
    fun `an accept by ourselves on another device does not start a join`() {
        val callState = outgoingRing()

        callState.updateFromRingState(ringState(acceptedBy = mapOf(user.id to at)))

        coVerify(exactly = 0) { call.join(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a ring nobody answered does not start a join`() {
        val callState = outgoingRing()

        callState.updateFromRingState(ringState(acceptedBy = emptyMap()))

        assertThat(callState.ringingState.value)
            .isEqualTo(RingingState.Outgoing(acceptedByCallee = false))
        coVerify(exactly = 0) { call.join(any(), any(), any(), any(), any(), any()) }
    }
}
