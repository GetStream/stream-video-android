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

import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.connection.Publisher
import io.getstream.video.android.core.call.connection.Subscriber
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.webrtc.PeerConnection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActiveStateGateTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── TestData now exposes firstRtpPacketArrived ────────────────────────────

    private data class TestData(
        val call: Call,
        val publisherState: MutableStateFlow<PeerConnection.PeerConnectionState?>,
        val subscriberState: MutableStateFlow<PeerConnection.PeerConnectionState?>,
        val firstRtpPacketArrived: MutableStateFlow<Boolean>,
    )

    private fun fakeCall(
        publisherState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
            MutableStateFlow(PeerConnection.PeerConnectionState.NEW),
        subscriberState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
            MutableStateFlow(PeerConnection.PeerConnectionState.NEW),
        firstRtpPacketArrived: MutableStateFlow<Boolean> = MutableStateFlow(false),
    ): TestData {
        val publisher = mockk<Publisher>(relaxed = true) {
            every { state } returns publisherState
        }
        val subscriber = mockk<Subscriber>(relaxed = true) {
            every { state } returns subscriberState
        }
        val session = mockk<RtcSession> {
            every { this@mockk.publisher } returns MutableStateFlow(publisher)
            every { this@mockk.subscriber } returns MutableStateFlow(subscriber)
        }
        val call = mockk<Call> {
            every { this@mockk.session } returns MutableStateFlow(session)
        }
        return TestData(call, publisherState, subscriberState, firstRtpPacketArrived)
    }

    // ── 1. Non-ringing previous states ────────────────────────────────────────

    @Test
    fun `transitions immediately when no previous incoming or outgoing state`() =
        runTest(testDispatcher) {
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = emptySet(),
            )
            val (call, _, _, _) = fakeCall()
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(
                currentRingingState = RingingState.Idle,
                call = call,
                onReady = { transitioned += Unit },
            )

            assertTrue(transitioned.size == 1)
        }

    // ── 2. Already Active ─────────────────────────────────────────────────────

    @Test
    fun `does NOT observe peer connection when already in Active state`() =
        runTest(testDispatcher) {
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(RingingState.Incoming(false)),
            )
            val (call, _, _, _) = fakeCall()
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(
                currentRingingState = RingingState.Active,
                call = call,
                onReady = { transitioned += Unit },
            )

            assertTrue(transitioned.isEmpty())
        }

    @Test
    fun `transitions when both peers connect for Outgoing previous state`() =
        runTest(testDispatcher) {
            val pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val subState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val (call, _, _, _) = fakeCall(pubState, subState)

            val outgoingRingingState = RingingState.Outgoing(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(outgoingRingingState),
                timeoutMs = 5_000L,
            )
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(
                currentRingingState = outgoingRingingState,
                call = call,
                onReady = { transitioned += Unit },
            )

            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            subState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1)
        }

    // ── 4. Timeout ────────────────────────────────────────────────────────────

    @Test
    fun `still calls onReady after timeout even if peers never connect`() =
        runTest(testDispatcher) {
            val (call, _, _, _) = fakeCall()

            val incomingRingingState = RingingState.Incoming(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(incomingRingingState),
                timeoutMs = 100L,
            )
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(
                currentRingingState = incomingRingingState,
                call = call,
                onReady = { transitioned += Unit },
            )

            assertTrue(transitioned.isEmpty())
            advanceTimeBy(200L)
            assertTrue(transitioned.size == 1)
        }

    // ── 5. Duplicate observer guard ───────────────────────────────────────────

    @Test
    fun `calling awaitAndTransition twice does not start a second observer`() =
        runTest(testDispatcher) {
            val pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val subState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val (call, _, _, _) = fakeCall(pubState, subState)

            val incomingRingingState = RingingState.Incoming(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(incomingRingingState),
                timeoutMs = 5_000L,
            )
            val transitioned = mutableListOf<Unit>()
            val action = { transitioned += Unit }

            sut.awaitAndTransition(incomingRingingState, call, onReady = action)
            sut.awaitAndTransition(incomingRingingState, call, onReady = action)

            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            subState.value = PeerConnection.PeerConnectionState.CONNECTED

            assertTrue(transitioned.size == 1)
        }

    // ── 6. cleanup() ─────────────────────────────────────────────────────────

    @Test
    fun `cleanup cancels the observer job and onReady is never called`() =
        runTest(testDispatcher) {
            val (call, pubState, subState, _) = fakeCall()

            val incomingRingingState = RingingState.Incoming(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(incomingRingingState),
                timeoutMs = 5_000L,
            )
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(
                currentRingingState = incomingRingingState,
                call = call,
                onReady = { transitioned += Unit },
            )

            sut.cleanup()

            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            subState.value = PeerConnection.PeerConnectionState.CONNECTED

            assertTrue(transitioned.isEmpty())
        }

    @Test
    fun `cleanup allows a new observer to be started afterwards`() =
        runTest(testDispatcher) {
            val pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val subState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val (call, _, _, _) = fakeCall(pubState, subState)

            val incomingRingingState = RingingState.Incoming(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(incomingRingingState),
                timeoutMs = 5_000L,
            )
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(incomingRingingState, call) { transitioned += Unit }
            sut.cleanup()
            sut.awaitAndTransition(incomingRingingState, call) { transitioned += Unit }

            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            subState.value = PeerConnection.PeerConnectionState.CONNECTED

            assertTrue(transitioned.size == 1)
        }

    // ── 7. Null session / late-arriving session ───────────────────────────────

    @Test
    fun `waits for non-null session before observing peer connections`() =
        runTest(testDispatcher) {
            val sessionFlow = MutableStateFlow<RtcSession?>(null)
            val pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val subState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)

            val publisher = mockk<Publisher> { every { state } returns pubState }
            val subscriber = mockk<Subscriber>(relaxed = true) { every { state } returns subState }
            val session = mockk<RtcSession> {
                every { this@mockk.publisher } returns MutableStateFlow(publisher)
                every { this@mockk.subscriber } returns MutableStateFlow(subscriber)
            }
            val call = mockk<Call> { every { this@mockk.session } returns sessionFlow }

            val incomingRingingState = RingingState.Incoming(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(incomingRingingState),
                timeoutMs = 5_000L,
            )
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(incomingRingingState, call) { transitioned += Unit }

            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            subState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.isEmpty())

            sessionFlow.value = session
            assertTrue(transitioned.size == 1)
        }

    // ── isActive guard tests ──────────────────────────────────────────────────

    @Test
    fun `onReady is NOT called if cleanup happens while waiting for peer connection`() =
        runTest(StandardTestDispatcher()) {
            val pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val subState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val (call, _, _, _) = fakeCall(pubState, subState)

            val incomingRingingState = RingingState.Incoming(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(incomingRingingState),
                timeoutMs = 5_000L,
            )
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(incomingRingingState, call) { transitioned += Unit }

            // Peers connect but cleanup cancels before the coroutine resumes
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            subState.value = PeerConnection.PeerConnectionState.CONNECTED
            sut.cleanup() // cancels before isActive check runs

            // Now let any pending coroutine work drain — onReady must not fire
            advanceUntilIdle()

            assertTrue(transitioned.isEmpty())
        }

    // ── Strategy tests ────────────────────────────────────────────────────────

    private fun runStrategyTest(
        strategy: TransitionToRingingStateStrategy,
        pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
            MutableStateFlow(PeerConnection.PeerConnectionState.NEW),
        subState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
            MutableStateFlow(PeerConnection.PeerConnectionState.NEW),
        firstRtpPacketArrived: MutableStateFlow<Boolean> = MutableStateFlow(false),
        block: suspend TestScope.(
            call: Call,
            sut: ActiveStateGate,
            transitioned: MutableList<Unit>,
            pubState: MutableStateFlow<PeerConnection.PeerConnectionState?>,
            subState: MutableStateFlow<PeerConnection.PeerConnectionState?>,
            firstRtpPacketArrived: MutableStateFlow<Boolean>,
        ) -> Unit,
    ) = runTest(testDispatcher) {
        val testData = fakeCall(pubState, subState, firstRtpPacketArrived)
        val incoming = RingingState.Incoming(false)
        val sut = ActiveStateGate(
            coroutineScope = this,
            previousRingingStates = setOf(incoming),
            strategy = strategy,
            timeoutMs = 5_000L,
        )
        val transitioned = mutableListOf<Unit>()
        sut.awaitAndTransition(incoming, testData.call) { transitioned += Unit }
        block(testData.call, sut, transitioned, pubState, subState, firstRtpPacketArrived)
    }

    @Test
    fun `LEGACY_BEHAVIOUR – transition still fires without timeout fallback`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.LEGACY_BEHAVIOUR,
        ) { _, _, transitioned, _, _, _ ->
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `PUBLISHER_CONNECTED – subscriber alone is not enough`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.PUBLISHER_CONNECTED,
        ) { _, _, transitioned, pubState, subState, _ ->
            subState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.isEmpty())

            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1)
        }

    // ── RingingCallJoinInterceptor tests ──────────────────────────────────────

    @Test
    fun `interceptor is called before onReady for ringing call`() =
        runTest(testDispatcher) {
            val pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val (call, _, _, _) = fakeCall(pubState)

            val interceptorCalled = mutableListOf<Unit>()
            val interceptor = object : RingingCallJoinInterceptor {
                override suspend fun callReadyToJoinWithTimeout(call: Call) {
                    interceptorCalled += Unit
                }
            }

            val incoming = RingingState.Incoming(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(incoming),
                timeoutMs = 5_000L,
            )
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(incoming, call, interceptor) { transitioned += Unit }
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED

            assertEquals(1, interceptorCalled.size)
            assertEquals(1, transitioned.size)
        }

    @Test
    fun `delaying interceptor postpones onReady`() =
        runTest(StandardTestDispatcher()) {
            val pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val (call, _, _, _) = fakeCall(pubState)

            val interceptor = object : RingingCallJoinInterceptor {
                override suspend fun callReadyToJoinWithTimeout(call: Call) {
                    delay(1_000L)
                }
            }

            val incoming = RingingState.Incoming(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(incoming),
                timeoutMs = 5_000L,
            )
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(incoming, call, interceptor) { transitioned += Unit }
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            // Advance just enough for the peer connection detection to process,
            // but not enough to drain the interceptor's delay(1_000L)
            advanceTimeBy(10L)

            // onReady not yet called — interceptor is still suspending
            assertTrue(transitioned.isEmpty())

            advanceTimeBy(1_100L)
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `interceptor exceeding 5s timeout still proceeds to Active`() =
        runTest(StandardTestDispatcher()) {
            val pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val (call, _, _, _) = fakeCall(pubState)

            val interceptor = object : RingingCallJoinInterceptor {
                override suspend fun callReadyToJoinWithTimeout(call: Call) {
                    delay(Long.MAX_VALUE) // never returns on its own
                }
            }

            val incoming = RingingState.Incoming(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(incoming),
                timeoutMs = 100L,
            )
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(incoming, call, interceptor) { transitioned += Unit }
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            // Advance past the 100ms peer connection timeout so the interceptor starts,
            // but not past the 5s interceptor timeout
            advanceTimeBy(200L)

            assertTrue(transitioned.isEmpty())

            advanceTimeBy(5_100L) // past 5s interceptor timeout
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `interceptor throwing exception still proceeds to Active`() =
        runTest(testDispatcher) {
            val pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val (call, _, _, _) = fakeCall(pubState)

            val interceptor = object : RingingCallJoinInterceptor {
                override suspend fun callReadyToJoinWithTimeout(call: Call) {
                    throw RuntimeException("interceptor error")
                }
            }

            val incoming = RingingState.Incoming(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(incoming),
                timeoutMs = 5_000L,
            )
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(incoming, call, interceptor) { transitioned += Unit }
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED

            assertEquals(1, transitioned.size)
        }

    @Test
    fun `interceptor is NOT called for non-ringing Active transitions`() =
        runTest(testDispatcher) {
            val interceptorCalled = mutableListOf<Unit>()
            val interceptor = object : RingingCallJoinInterceptor {
                override suspend fun callReadyToJoinWithTimeout(call: Call) {
                    interceptorCalled += Unit
                }
            }

            val (call, _, _, _) = fakeCall()
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = emptySet(), // no incoming/outgoing history
            )
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(RingingState.Idle, call, interceptor) { transitioned += Unit }

            assertTrue(interceptorCalled.isEmpty())
            assertEquals(1, transitioned.size) // onReady called directly
        }
}
