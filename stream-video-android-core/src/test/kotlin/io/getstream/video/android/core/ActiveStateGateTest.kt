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

    private data class TestData(
        val call: Call,
        val publisherState: MutableStateFlow<PeerConnection.PeerConnectionState?>,
    )

    private fun fakeCall(
        publisherState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
            MutableStateFlow(PeerConnection.PeerConnectionState.NEW),
    ): TestData {
        val publisher = mockk<Publisher>(relaxed = true) {
            every { state } returns publisherState
        }
        val session = mockk<RtcSession> {
            every { this@mockk.publisher } returns MutableStateFlow(publisher)
        }
        val call = mockk<Call> {
            every { this@mockk.session } returns MutableStateFlow(session)
        }
        return TestData(call, publisherState)
    }

    // ── 1. Non-ringing previous states ────────────────────────────────────────

    @Test
    fun `transitions immediately when no previous incoming or outgoing state`() =
        runTest(testDispatcher) {
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = emptySet(),
            )
            val (call, _) = fakeCall()
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(
                currentRingingState = RingingState.Idle,
                call = call,
                interceptor = null,
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
            val (call, _) = fakeCall()
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(
                currentRingingState = RingingState.Active,
                call = call,
                interceptor = null,
                onReady = { transitioned += Unit },
            )

            assertTrue(transitioned.isEmpty())
        }

    @Test
    fun `transitions when publisher connects for Outgoing previous state`() =
        runTest(testDispatcher) {
            val pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val (call, _) = fakeCall(pubState)

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
                interceptor = null,
                onReady = { transitioned += Unit },
            )

            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1)
        }

    // ── 4. Timeout ────────────────────────────────────────────────────────────

    @Test
    fun `still calls onReady after timeout even if publisher never connects`() =
        runTest(testDispatcher) {
            val (call, _) = fakeCall()

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
                interceptor = null,
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
            val (call, _) = fakeCall(pubState)

            val incomingRingingState = RingingState.Incoming(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(incomingRingingState),
                timeoutMs = 5_000L,
            )
            val transitioned = mutableListOf<Unit>()
            val action = { transitioned += Unit }

            sut.awaitAndTransition(
                incomingRingingState,
                call,
                null,
                onReady = action,
            )
            sut.awaitAndTransition(
                incomingRingingState,
                call,
                null,
                onReady = action,
            )

            pubState.value = PeerConnection.PeerConnectionState.CONNECTED

            assertTrue(transitioned.size == 1)
        }

    // ── 6. cleanup() ─────────────────────────────────────────────────────────

    @Test
    fun `cleanup cancels the observer job and onReady is never called`() =
        runTest(testDispatcher) {
            val (call, pubState) = fakeCall()

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
                interceptor = null,
                onReady = { transitioned += Unit },
            )

            sut.cleanup()
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED

            assertTrue(transitioned.isEmpty())
        }

    @Test
    fun `cleanup allows a new observer to be started afterwards`() =
        runTest(testDispatcher) {
            val pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val (call, _) = fakeCall(pubState)

            val incomingRingingState = RingingState.Incoming(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(incomingRingingState),
                timeoutMs = 5_000L,
            )
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(incomingRingingState, call, null) {
                transitioned += Unit
            }
            sut.cleanup()
            sut.awaitAndTransition(incomingRingingState, call, null) {
                transitioned += Unit
            }

            pubState.value = PeerConnection.PeerConnectionState.CONNECTED

            assertTrue(transitioned.size == 1)
        }

    // ── 7. Null session / late-arriving session ───────────────────────────────

    @Test
    fun `waits for non-null session before observing peer connections`() =
        runTest(testDispatcher) {
            val sessionFlow = MutableStateFlow<RtcSession?>(null)
            val pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)

            val publisher = mockk<Publisher> { every { state } returns pubState }
            val session = mockk<RtcSession> {
                every { this@mockk.publisher } returns MutableStateFlow(publisher)
            }
            val call = mockk<Call> { every { this@mockk.session } returns sessionFlow }

            val incomingRingingState = RingingState.Incoming(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(incomingRingingState),
                timeoutMs = 5_000L,
            )
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(incomingRingingState, call, null) {
                transitioned += Unit
            }

            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
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
            val (call, _) = fakeCall(pubState)

            val incomingRingingState = RingingState.Incoming(false)
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = setOf(incomingRingingState),
                timeoutMs = 5_000L,
            )
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(incomingRingingState, call, null) {
                transitioned += Unit
            }

            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            sut.cleanup()

            advanceUntilIdle()

            assertTrue(transitioned.isEmpty())
        }

    // ── Strategy tests ────────────────────────────────────────────────────────

    private fun runStrategyTest(
        strategy: TransitionToRingingStateStrategy,
        pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
            MutableStateFlow(PeerConnection.PeerConnectionState.NEW),
        block: suspend TestScope.(
            call: Call,
            sut: ActiveStateGate,
            transitioned: MutableList<Unit>,
            pubState: MutableStateFlow<PeerConnection.PeerConnectionState?>,
        ) -> Unit,
    ) = runTest(testDispatcher) {
        val testData = fakeCall(pubState)
        val incoming = RingingState.Incoming(false)
        val sut = ActiveStateGate(
            coroutineScope = this,
            previousRingingStates = setOf(incoming),
            strategy = strategy,
            timeoutMs = 5_000L,
        )
        val transitioned = mutableListOf<Unit>()
        sut.awaitAndTransition(incoming, testData.call, null) {
            transitioned += Unit
        }
        block(testData.call, sut, transitioned, pubState)
    }

    @Test
    fun `LEGACY_BEHAVIOUR – transition still fires without timeout fallback`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.LEGACY_BEHAVIOUR,
        ) { _, _, transitioned, _ ->
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `PUBLISHER_CONNECTED – transitions when publisher connects`() =
        runStrategyTest(
            TransitionToRingingStateStrategy.PUBLISHER_CONNECTED,
        ) { _, _, transitioned, pubState ->
            assertTrue(transitioned.isEmpty())

            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            assertTrue(transitioned.size == 1)
        }

    // ── RingingCallActivationInterceptor tests ──────────────────────────────────────

    @Test
    fun `interceptor is called before onReady for ringing call`() =
        runTest(testDispatcher) {
            val pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val (call, _) = fakeCall(pubState)

            val interceptorCalled = mutableListOf<Unit>()
            val interceptor = object : RingingCallActivationInterceptor {
                override suspend fun callReadyToActivateWithTimeout(call: Call) {
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
            val (call, _) = fakeCall(pubState)

            val interceptor = object : RingingCallActivationInterceptor {
                override suspend fun callReadyToActivateWithTimeout(call: Call) {
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
            advanceTimeBy(10L)

            assertTrue(transitioned.isEmpty())

            advanceTimeBy(1_100L)
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `interceptor exceeding 5s timeout still proceeds to Active`() =
        runTest(StandardTestDispatcher()) {
            val pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val (call, _) = fakeCall(pubState)

            val interceptor = object : RingingCallActivationInterceptor {
                override suspend fun callReadyToActivateWithTimeout(call: Call) {
                    delay(Long.MAX_VALUE)
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
            advanceTimeBy(200L)

            assertTrue(transitioned.isEmpty())

            advanceTimeBy(5_100L)
            assertTrue(transitioned.size == 1)
        }

    @Test
    fun `interceptor throwing exception still proceeds to Active`() =
        runTest(testDispatcher) {
            val pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val (call, _) = fakeCall(pubState)

            val interceptor = object : RingingCallActivationInterceptor {
                override suspend fun callReadyToActivateWithTimeout(call: Call) {
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
    fun `interceptor and publisher wait run in parallel`() =
        runTest(StandardTestDispatcher()) {
            val pubState: MutableStateFlow<PeerConnection.PeerConnectionState?> =
                MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
            val (call, _) = fakeCall(pubState)

            val interceptor = object : RingingCallActivationInterceptor {
                override suspend fun callReadyToActivateWithTimeout(call: Call) {
                    delay(2_000L)
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

            advanceTimeBy(2_000L)
            pubState.value = PeerConnection.PeerConnectionState.CONNECTED
            advanceTimeBy(50L)

            assertEquals(1, transitioned.size)
        }

    @Test
    fun `interceptor is NOT called for non-ringing Active transitions`() =
        runTest(testDispatcher) {
            val interceptorCalled = mutableListOf<Unit>()
            val interceptor = object : RingingCallActivationInterceptor {
                override suspend fun callReadyToActivateWithTimeout(call: Call) {
                    interceptorCalled += Unit
                }
            }

            val (call, _) = fakeCall()
            val sut = ActiveStateGate(
                coroutineScope = this,
                previousRingingStates = emptySet(),
            )
            val transitioned = mutableListOf<Unit>()

            sut.awaitAndTransition(RingingState.Idle, call, interceptor) { transitioned += Unit }

            assertTrue(interceptorCalled.isEmpty())
            assertEquals(1, transitioned.size)
        }
}
