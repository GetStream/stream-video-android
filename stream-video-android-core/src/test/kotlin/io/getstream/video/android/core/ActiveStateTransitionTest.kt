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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.webrtc.PeerConnection
import kotlin.test.Test
import kotlin.test.assertTrue

class ActiveStateTransitionTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Builds a [Call] fake whose session/publisher/subscriber are backed by
     * [MutableStateFlow]s so tests can push state changes imperatively.
     */
    private fun fakeCall(
        publisherState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
            MutableStateFlow(PeerConnection.PeerConnectionState.NEW),
        subscriberState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
            MutableStateFlow(PeerConnection.PeerConnectionState.NEW),
    ): TestData {
        val publisher = mockk<Publisher> {
            every { state } returns publisherState
        }
        val subscriber = mockk<Subscriber> {
            every { state } returns subscriberState
        }
        val session = mockk<RtcSession> {
            every { this@mockk.publisher } returns MutableStateFlow(publisher)
            every { this@mockk.subscriber } returns MutableStateFlow(subscriber)
        }
        val call = mockk<Call> {
            every { this@mockk.session } returns MutableStateFlow(session)
        }
        return TestData(call, publisherState, subscriberState)
    }

    private data class TestData(
        val call: Call,
        val publisherState: MutableStateFlow<PeerConnection.PeerConnectionState?>,
        val subscriberState: MutableStateFlow<PeerConnection.PeerConnectionState?>,
    )

    // ── 1. Non-ringing previous states ────────────────────────────────────────

    @Test
    fun `transitions immediately when no previous incoming or outgoing state`() =
        runTest(testDispatcher) {
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = emptySet(), // no Incoming / Outgoing
            )
            val (call, _, _) = fakeCall()
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
            val (call, _, _) = fakeCall()
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(
                currentRingingState = RingingState.Active, // already active
                call = call,
                onReady = { transitioned += Unit },
            )

            // callback must NOT fire at all (peer connection logic is skipped)
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
            val (call, _, _) = fakeCall(pubState, subState)

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

            // Neither connected yet → no transition
            assertTrue(transitioned.isEmpty())

            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.isEmpty()) // subscriber still not connected

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
            val (call, _, _) = fakeCall(pubState, subState)

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
    fun `still calls transitionToActiveState after timeout even if peers never connect`() =
        runTest(testDispatcher) {
            val (call, _, _) = fakeCall() // peers stay in NEW state forever

            val incomingRingingState = RingingState.Incoming(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(incomingRingingState),
                timeoutMs = 100L, // short timeout to keep test fast
            )
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(
                currentRingingState = incomingRingingState,
                call = call,
                onReady = { transitioned += Unit },
            )

            assertTrue(transitioned.isEmpty())

            // Advance virtual time past the timeout
            advanceTimeBy(200L)

            assertTrue(transitioned.size == 1)
        }

    // ── 5. Duplicate observer guard ───────────────────────────────────────────

    @Test
    fun `calling transitionToActiveState twice does not start a second observer`() =
        runTest(testDispatcher) {
            val pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val subState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val (call, _, _) = fakeCall(pubState, subState)

            val incomingRingingState = RingingState.Incoming(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(incomingRingingState),
                timeoutMs = 5_000L,
            )
            val transitioned = mutableListOf<Unit>()
            val action = { transitioned += Unit }

            sut.awaitAndTransition(incomingRingingState, call, action)
            sut.awaitAndTransition(incomingRingingState, call, action) // second call

            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            subState.value = PeerConnection.PeerConnectionState.CONNECTED

            // Must fire exactly once, not twice
            assertTrue(transitioned.size == 1)
        }

    // ── 6. cleanup() ─────────────────────────────────────────────────────────

    @Test
    fun `cleanup cancels the observer job and transition is never called`() =
        runTest(testDispatcher) {
            val (call, pubState, subState) = fakeCall()

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

            // Connect both peers after cleanup
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
            val (call, _, _) = fakeCall(pubState, subState)

            val incomingRingingState = RingingState.Incoming(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(incomingRingingState),
                timeoutMs = 5_000L,
            )
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(incomingRingingState, call) { transitioned += Unit }
            sut.cleanup()

            // Start a fresh observation after cleanup
            sut.awaitAndTransition(incomingRingingState, call) { transitioned += Unit }

            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            subState.value = PeerConnection.PeerConnectionState.CONNECTED

            assertTrue(transitioned.size == 1)
        }

    // ── 7. Null session / late-arriving session ───────────────────────────────

    @Test
    fun `waits for non-null session before observing peer connections`() =
        runTest(testDispatcher) {
            val sessionFlow = MutableStateFlow<RtcSession?>(null) // null initially
            val pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val subState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)

            val publisher = mockk<Publisher> { every { state } returns pubState }
            val subscriber = mockk<Subscriber> { every { state } returns subState }
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

            // Connect peers while session is still null → nothing should happen
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            subState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.isEmpty())

            // Now the session arrives
            sessionFlow.value = session
            assertTrue(transitioned.size == 1)
        }

    // ── Strategy tests ────────────────────────────────────────────────────────
    // These call observePeerConnection() directly to test each strategy in isolation.

    private fun runStrategyTest(
        strategy: TransitionToRingingStateStrategy,
        pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
            MutableStateFlow(PeerConnection.PeerConnectionState.NEW),
        subState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
            MutableStateFlow(PeerConnection.PeerConnectionState.NEW),
        block: suspend TestScope.(
            sut: ActiveStateGate,
            transitioned: MutableList<Unit>,
            pubState: MutableStateFlow<PeerConnection.PeerConnectionState?>,
            subState: MutableStateFlow<PeerConnection.PeerConnectionState?>,
        ) -> Unit,
    ) = runTest(testDispatcher) {
        val (call, _, _) = fakeCall(pubState, subState)
        val incoming = RingingState.Incoming(false)
        val sut = ActiveStateGate(
            coroutineScope = this,
            previousRingingStates = setOf(incoming),
            strategy = strategy,
            timeoutMs = 5_000L,
        )
        val transitioned = mutableListOf<Unit>()
        // RingingState.Ringing ensures the observer branch is always entered
        sut.awaitAndTransition(incoming, call) { transitioned += Unit }
        block(sut, transitioned, pubState, subState)
    }

    @Test
    fun `NONE – emptyFlow times out, transition still fires via timeout fallback`() =
        runStrategyTest(TransitionToRingingStateStrategy.NONE) { _, transitioned, _, _ ->
            assertTrue(transitioned.isEmpty())
            advanceTimeBy(6_000L)
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `PUBLISHER_CONNECTED – subscriber alone is not enough`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.PUBLISHER_CONNECTED,
        ) { _, transitioned, pubState, subState ->
            subState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.isEmpty())

            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `SUBSCRIBER_CONNECTED – publisher alone is not enough`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.SUBSCRIBER_CONNECTED,
        ) { _, transitioned, pubState, subState ->
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.isEmpty())

            subState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `ANY_PEER_CONNECTED – publisher alone is sufficient`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.ANY_PEER_CONNECTED,
        ) { _, transitioned, pubState, _ ->
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `ANY_PEER_CONNECTED – subscriber alone is sufficient`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.ANY_PEER_CONNECTED,
        ) { _, transitioned, _, subState ->
            subState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `BOTH_PEER_CONNECTED – one peer alone is not enough`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.BOTH_PEER_CONNECTED,
        ) { _, transitioned, pubState, subState ->
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.isEmpty())

            subState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `BOTH_PEER_CONNECTED – fires exactly once, reconnection does not re-trigger`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.BOTH_PEER_CONNECTED,
        ) { _, transitioned, pubState, subState ->
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            subState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1)

            // Simulate drop + reconnect after first() already completed
            pubState.value = PeerConnection.PeerConnectionState.DISCONNECTED
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1) // still exactly 1
        }

    @Test
    fun `FIST_PACKET_RECEIVED – publisher alone is not enough (mirrors SUBSCRIBER_CONNECTED)`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.FIST_PACKET_RECEIVED,
        ) { _, transitioned, pubState, subState ->
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.isEmpty())

            subState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1)
        }
}
