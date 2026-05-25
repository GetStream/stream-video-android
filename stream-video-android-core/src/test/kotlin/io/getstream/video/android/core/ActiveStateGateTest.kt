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
import io.getstream.webrtc.PeerConnection
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
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
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActiveStateGateTest {
    private val testDispatcher = StandardTestDispatcher()

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
            every { leave(any()) } just runs
        }
        return TestData(call, publisherState)
    }

    @Test
    fun `should ignore when current state already active`() = runTest(testDispatcher) {
        val data = fakeCall()
        var readyCalled = false
        val gate = ActiveStateGate(
            coroutineScope = this,
            previousRingingStates = emptySet(),
        )
        gate.awaitAndTransition(
            currentRingingState = RingingState.Active,
            call = data.call,
            interceptor = null,
        ) {
            readyCalled = true
        }
        advanceUntilIdle()
        assertFalse(readyCalled)
    }

    @Test
    fun `should invoke immediately for non ringing call`() = runTest(testDispatcher) {
        val data = fakeCall()
        var readyCalled = false
        val gate = ActiveStateGate(
            coroutineScope = this,
            previousRingingStates = emptySet(),
        )

        gate.awaitAndTransition(
            currentRingingState = RingingState.Idle,
            call = data.call,
            interceptor = null,
        ) {
            readyCalled = true
        }
        advanceUntilIdle()
        assertTrue(readyCalled)
    }

    @Test
    fun `should wait for publisher connection before transition`() = runTest(testDispatcher) {
        val data = fakeCall()
        var readyCalled = false
        val gate = ActiveStateGate(
            coroutineScope = this,
            previousRingingStates = setOf(RingingState.Incoming(false)),
        )

        gate.awaitAndTransition(
            currentRingingState = RingingState.Idle,
            call = data.call,
            interceptor = null,
        ) { readyCalled = true }

        runCurrent()
        assertFalse(readyCalled)
        data.publisherState.value = PeerConnection.PeerConnectionState.CONNECTED
        advanceUntilIdle()
        assertTrue(readyCalled)
    }

    @Test
    fun `should proceed after peer connection timeout`() = runTest(testDispatcher) {
        val data = fakeCall()
        var readyCalled = false
        val gate = ActiveStateGate(
            coroutineScope = this,
            previousRingingStates = setOf(RingingState.Incoming()),
            peerConnectionObserverTimeoutMs = 100,
        )

        gate.awaitAndTransition(
            currentRingingState = RingingState.Idle,
            call = data.call,
            interceptor = null,
        ) { readyCalled = true }

        advanceTimeBy(101)
        advanceUntilIdle()
        assertTrue(readyCalled)
    }

    @Test
    fun `should invoke interceptor before transition`() = runTest {
        val data = fakeCall()
        val events = mutableListOf<String>()
        val interceptor = object : CallJoinInterceptor {
            override suspend fun callReadyToJoin(call: Call) {
                events += "interceptor"
            }
        }

        val gate = ActiveStateGate(
            coroutineScope = this,
            previousRingingStates = emptySet(),
        )

        gate.awaitAndTransition(
            currentRingingState = RingingState.Idle,
            call = data.call,
            interceptor = interceptor,
        ) { events += "ready" }

        advanceUntilIdle()
        assertEquals(
            listOf("interceptor", "ready"),
            events,
        )
    }

    @Test
    fun `should proceed when interceptor throws generic exception`() = runTest(testDispatcher) {
        val data = fakeCall()
        var readyCalled = false
        val interceptor = object : CallJoinInterceptor {
            override suspend fun callReadyToJoin(call: Call) {
                error("boom")
            }
        }

        val gate = ActiveStateGate(
            coroutineScope = this,
            previousRingingStates = emptySet(),
        )

        gate.awaitAndTransition(
            currentRingingState = RingingState.Idle,
            call = data.call,
            interceptor = interceptor,
        ) { readyCalled = true }

        advanceUntilIdle()
        assertTrue(readyCalled)
    }

    @Test
    fun `should not proceed when interceptor rejects`() = runTest(testDispatcher) {
        val data = fakeCall()
        var readyCalled = false
        val interceptor = object : CallJoinInterceptor {
            override suspend fun callReadyToJoin(call: Call) {
                throw CallJoinInterceptionException("blocked")
            }
        }

        val gate = ActiveStateGate(
            coroutineScope = this,
            previousRingingStates = emptySet(),
        )

        gate.awaitAndTransition(
            currentRingingState = RingingState.Idle,
            call = data.call,
            interceptor = interceptor,
        ) { readyCalled = true }

        advanceUntilIdle()
        assertFalse(readyCalled)
        coVerify { data.call.leave(any()) }
    }

    @Test
    fun `should proceed when interceptor times out`() = runTest(testDispatcher) {
        val data = fakeCall()
        var readyCalled = false
        val interceptor = object : CallJoinInterceptor {
            override suspend fun callReadyToJoin(call: Call) {
                delay(10_000)
            }
        }
        val interceptorTimeoutMs = 5_000L
        val gate = ActiveStateGate(
            coroutineScope = this,
            interceptorTimeoutMs = interceptorTimeoutMs,
            previousRingingStates = emptySet(),
        )

        gate.awaitAndTransition(
            currentRingingState = RingingState.Idle,
            call = data.call,
            interceptor = interceptor,
        ) { readyCalled = true }

        advanceTimeBy(interceptorTimeoutMs + 1)
        advanceUntilIdle()
        assertTrue(readyCalled)
    }

    @Test
    fun `should ignore duplicate gate launches`() = runTest(testDispatcher) {
        val data = fakeCall()
        var readyInvocationCount = 0
        val gate = ActiveStateGate(
            coroutineScope = this,
            previousRingingStates = setOf(RingingState.Incoming()),
        )

        repeat(2) {
            gate.awaitAndTransition(
                currentRingingState = RingingState.Idle,
                call = data.call,
                interceptor = null,
            ) { readyInvocationCount++ }
        }
        data.publisherState.value = PeerConnection.PeerConnectionState.CONNECTED
        advanceUntilIdle()
        assertEquals(1, readyInvocationCount)
    }

    @Test
    fun `cleanup should cancel ongoing gate`() = runTest(testDispatcher) {
        val data = fakeCall()
        var readyCalled = false
        val gate = ActiveStateGate(
            coroutineScope = this,
            previousRingingStates = setOf(RingingState.Incoming()),
        )

        gate.awaitAndTransition(
            currentRingingState = RingingState.Idle,
            call = data.call,
            interceptor = null,
        ) { readyCalled = true }

        gate.cleanup()
        data.publisherState.value = PeerConnection.PeerConnectionState.CONNECTED

        advanceUntilIdle()
        assertFalse(readyCalled)

        gate.awaitAndTransition(
            currentRingingState = RingingState.Idle,
            call = data.call,
            interceptor = null,
        ) { readyCalled = true }

        advanceUntilIdle()
        assertTrue(readyCalled)
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
            peerConnectionObserverTimeoutMs = 5_000L,
        )
        val transitioned = mutableListOf<Unit>()
        sut.awaitAndTransition(incoming, testData.call, null) {
            transitioned += Unit
        }
        block(testData.call, sut, transitioned, pubState)
    }

    @Test
    fun `legacy behaviour should invoke immediately when interceptor is null`() = runTest {
        val data = fakeCall()
        var readyCalled = false
        val gate = ActiveStateGate(
            coroutineScope = this,
            previousRingingStates = emptySet(),
            strategy = TransitionToRingingStateStrategy.LEGACY_BEHAVIOUR,
        )

        gate.awaitAndTransition(
            currentRingingState = RingingState.Idle,
            call = data.call,
            interceptor = null,
        ) { readyCalled = true }

        runCurrent()
        assertTrue(readyCalled)
    }

    @Test
    fun `legacy behaviour should invoke interceptor before transition`() = runTest {
        val data = fakeCall()
        val events = mutableListOf<String>()
        val interceptor = object : CallJoinInterceptor {
            override suspend fun callReadyToJoin(call: Call) {
                events += "interceptor"
            }
        }

        val gate = ActiveStateGate(
            coroutineScope = this,
            previousRingingStates = emptySet(),
            strategy = TransitionToRingingStateStrategy.LEGACY_BEHAVIOUR,
        )
        gate.awaitAndTransition(
            currentRingingState = RingingState.Idle,
            call = data.call,
            interceptor = interceptor,
        ) { events += "ready" }

        advanceUntilIdle()
        assertEquals(
            listOf("interceptor", "ready"),
            events,
        )
    }

    @Test
    fun `legacy behaviour should not proceed when interceptor rejects`() = runTest {
        val data = fakeCall()
        var readyCalled = false
        val interceptor = object : CallJoinInterceptor {
            override suspend fun callReadyToJoin(call: Call) {
                throw CallJoinInterceptionException("blocked")
            }
        }

        val gate = ActiveStateGate(
            coroutineScope = this,
            previousRingingStates = emptySet(),
            strategy = TransitionToRingingStateStrategy.LEGACY_BEHAVIOUR,
        )

        gate.awaitAndTransition(
            currentRingingState = RingingState.Idle,
            call = data.call,
            interceptor = interceptor,
        ) { readyCalled = true }

        advanceUntilIdle()
        assertFalse(readyCalled)
        coVerify { data.call.leave(any()) }
    }

    @Test
    fun `legacy behaviour should ignore duplicate interceptor launches`() = runTest {
        val data = fakeCall()
        var interceptorInvocationCount = 0
        val interceptor = object : CallJoinInterceptor {
            override suspend fun callReadyToJoin(call: Call) {
                interceptorInvocationCount++
                delay(1000)
            }
        }

        val gate = ActiveStateGate(
            coroutineScope = this,
            previousRingingStates = emptySet(),
            strategy = TransitionToRingingStateStrategy.LEGACY_BEHAVIOUR,
        )

        repeat(2) {
            gate.awaitAndTransition(
                currentRingingState = RingingState.Idle,
                call = data.call,
                interceptor = interceptor,
            ) {}
        }

        runCurrent()
        assertEquals(1, interceptorInvocationCount)
    }

    @Test
    fun `cleanup should cancel gate during interceptor execution`() = runTest {
        val data = fakeCall()
        var readyCalled = false
        val interceptorStarted = CompletableDeferred<Unit>()
        val interceptorCancelled = CompletableDeferred<Unit>()

        val interceptor = object : CallJoinInterceptor {
            override suspend fun callReadyToJoin(call: Call) {
                interceptorStarted.complete(Unit)
                try {
                    awaitCancellation()
                } catch (e: CancellationException) {
                    interceptorCancelled.complete(Unit)
                    throw e
                }
            }
        }

        val gate = ActiveStateGate(
            coroutineScope = this,
            previousRingingStates = emptySet(),
        )

        gate.awaitAndTransition(
            currentRingingState = RingingState.Idle,
            call = data.call,
            interceptor = interceptor,
        ) { readyCalled = true }

        interceptorStarted.await()
        gate.cleanup()
        interceptorCancelled.await()
        runCurrent()
        assertFalse(readyCalled)
    }
}
