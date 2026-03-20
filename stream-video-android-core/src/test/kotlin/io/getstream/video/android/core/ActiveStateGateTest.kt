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
            every { firstRtpPacketArrivedWithinTimeout } returns firstRtpPacketArrived
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

    // ── 3. BOTH_PEER_CONNECTED – happy path ───────────────────────────────────

    @Test
    fun `transitions when both publisher and subscriber become CONNECTED`() =
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

            sut.awaitAndTransition(
                currentRingingState = incomingRingingState,
                call = call,
                onReady = { transitioned += Unit },
            )

            assertTrue(transitioned.isEmpty())

            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.isEmpty())

            subState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1)
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

            sut.awaitAndTransition(incomingRingingState, call, action)
            sut.awaitAndTransition(incomingRingingState, call, action)

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

    @Test
    fun `onReady is NOT called if cleanup happens while waiting for timeout`() =
        runTest(testDispatcher) {
            // NONE strategy uses emptyFlow — it will always wait for the full timeout
            val (call, _, _, _) = fakeCall()

            val incomingRingingState = RingingState.Incoming(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(incomingRingingState),
                strategy = TransitionToRingingStateStrategy.NONE,
                timeoutMs = 5_000L,
            )
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(incomingRingingState, call) { transitioned += Unit }

            // Cancel mid-timeout — onReady must not fire when timeout eventually elapses
            sut.cleanup()
            advanceTimeBy(6_000L)

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
    fun `NONE – emptyFlow times out, transition still fires via timeout fallback`() =
        runStrategyTest(TransitionToRingingStateStrategy.NONE) { _, _, transitioned, _, _, _ ->
            assertTrue(transitioned.isEmpty())
            advanceTimeBy(6_000L)
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

    @Test
    fun `SUBSCRIBER_CONNECTED – publisher alone is not enough`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.SUBSCRIBER_CONNECTED,
        ) { _, _, transitioned, pubState, subState, _ ->
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.isEmpty())

            subState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `ANY_PEER_CONNECTED – publisher alone is sufficient`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.ANY_PEER_CONNECTED,
        ) { _, _, transitioned, pubState, _, _ ->
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `ANY_PEER_CONNECTED – subscriber alone is sufficient`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.ANY_PEER_CONNECTED,
        ) { _, _, transitioned, _, subState, _ ->
            subState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `BOTH_PEER_CONNECTED – one peer alone is not enough`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.BOTH_PEER_CONNECTED,
        ) { _, _, transitioned, pubState, subState, _ ->
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.isEmpty())

            subState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `BOTH_PEER_CONNECTED – fires exactly once, reconnection does not re-trigger`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.BOTH_PEER_CONNECTED,
        ) { _, _, transitioned, pubState, subState, _ ->
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            subState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1)

            pubState.value = PeerConnection.PeerConnectionState.DISCONNECTED
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `FIST_PACKET_RECEIVED – peer connection state alone does not trigger transition`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.FIST_PACKET_RECEIVED,
        ) { _, _, transitioned, pubState, subState, _ ->
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            subState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.isEmpty())
        }

    @Test
    fun `FIST_PACKET_RECEIVED – transitions as soon as firstRtpPacketArrivedWithinTimeout emits true`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.FIST_PACKET_RECEIVED,
        ) { _, _, transitioned, _, _, firstRtpPacketArrived ->
            assertTrue(transitioned.isEmpty())

            firstRtpPacketArrived.value = true
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `FIST_PACKET_RECEIVED – times out and still calls onReady if no RTP packet ever arrives`() =
        runStrategyTest(
            strategy = TransitionToRingingStateStrategy.FIST_PACKET_RECEIVED,
            firstRtpPacketArrived = MutableStateFlow(false),
        ) { _, _, transitioned, _, _, _ ->
            assertTrue(transitioned.isEmpty())
            advanceTimeBy(6_000L)
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `FIST_PACKET_RECEIVED – fires exactly once even if flow emits true multiple times`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.FIST_PACKET_RECEIVED,
        ) { _, _, transitioned, _, _, firstRtpPacketArrived ->
            firstRtpPacketArrived.value = true
            firstRtpPacketArrived.value = false
            firstRtpPacketArrived.value = true
            assertTrue(transitioned.size == 1)
        }
}
