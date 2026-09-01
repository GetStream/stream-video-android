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

package io.getstream.video.android.core.call.components

import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.CallSettingsResponse
import io.getstream.android.video.generated.models.JoinCallResponse
import io.getstream.android.video.generated.models.RingCallRequest
import io.getstream.android.video.generated.models.RingCallResponse
import io.getstream.result.Error
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.video.android.core.CallJoinInterceptor
import io.getstream.video.android.core.CallLeaveReason
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.analytics.call.CallAnalytics
import io.getstream.video.android.core.analytics.call.observer.model.JoinAnalyticsModel
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.base.DispatcherRule
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.SfuConnectFailureCause
import io.getstream.video.android.core.call.SfuConnectionResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFailsWith

/**
 * Tests the join orchestration in [CallJoinCoordinator]: the [join] retry loop, join-and-ring,
 * the coordinator's own join request, and permanent-vs-transient error handling. The coordinator
 * is constructed directly with mocked collaborators so the sibling fan-out (media / lifecycle /
 * apiClient / reconnector / sessionMonitor) and the client-state registrations (via
 * [ClientCallRegistry]) can be stubbed and verified precisely.
 */
class CallJoinCoordinatorTest {

    @get:Rule
    val dispatcherRule = DispatcherRule()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var clientImpl: StreamVideoClient
    private lateinit var state: CallState
    private lateinit var callAnalytics: CallAnalytics
    private lateinit var sessionManager: CallSessionManager
    private lateinit var media: CallMediaManager
    private lateinit var lifecycle: CallLifecycleManager
    private lateinit var apiClient: CallApiClient
    private lateinit var reconnector: CallReconnector
    private lateinit var sessionMonitor: SessionMonitor
    private lateinit var callRegistry: ClientCallRegistry

    private lateinit var sessionFlow: MutableStateFlow<RtcSession?>
    private lateinit var connectionFlow: MutableStateFlow<RealtimeConnection>
    private lateinit var mockSession: RtcSession
    private lateinit var mockJoinResponse: JoinCallResponse

    @Before
    fun setup() {
        clientImpl = mockk(relaxed = true)
        state = mockk(relaxed = true)
        callAnalytics = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        media = mockk(relaxed = true)
        lifecycle = mockk(relaxed = true)
        apiClient = mockk(relaxed = true)
        reconnector = mockk(relaxed = true)
        sessionMonitor = mockk(relaxed = true)
        callRegistry = mockk(relaxed = true)
        mockSession = mockk(relaxed = true)
        mockJoinResponse = mockk(relaxed = true)

        sessionFlow = MutableStateFlow(null)
        connectionFlow = MutableStateFlow(RealtimeConnection.InProgress)

        every { sessionManager.session } returns sessionFlow
        every { sessionManager.setActiveSession(any()) } answers { sessionFlow.value = firstArg() }
        every { state._connection } returns connectionFlow
        every { state.connection } returns connectionFlow
        every { state.settings } returns MutableStateFlow<CallSettingsResponse?>(null)
        coEvery { clientImpl.getCachedLocation() } returns Success("test-location")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun coordinator() = CallJoinCoordinator(
        clientImpl = clientImpl,
        state = state,
        callAnalytics = callAnalytics,
        type = "default",
        id = "test-call",
        scope = testScope,
        sessionManager = sessionManager,
        sessionFactory = RtcSessionFactory { _, _, _, _, _, _, _ -> mockSession },
        media = media,
        lifecycle = lifecycle,
        apiClient = apiClient,
        reconnector = reconnector,
        sessionMonitor = sessionMonitor,
        callRegistry = callRegistry,
        hasRequiredPermissions = { true },
    )

    private fun stubJoinCall(result: io.getstream.result.Result<JoinCallResponse>) {
        coEvery {
            apiClient.joinRequest(
                any(), any(), any(), any(), any(), any(), any(), any(),
            )
        } returns result
    }

    @Test
    fun `join succeeds and returns the connected session`() = runTest(testDispatcher) {
        stubJoinCall(Success(mockJoinResponse))
        coEvery { mockSession.connectInternal() } returns SfuConnectionResult.Success

        val result = coordinator().join()
        advanceUntilIdle()

        assertThat(result).isInstanceOf(Success::class.java)
        assertThat((result as Success).value).isSameInstanceAs(mockSession)
        verify { sessionMonitor.monitorSession(mockJoinResponse) }
    }

    @Test
    fun `join fails permanently on a terminal SFU failure without retrying`() = runTest(
        testDispatcher,
    ) {
        stubJoinCall(Success(mockJoinResponse))
        coEvery { mockSession.connectInternal() } returns SfuConnectionResult.Failure(
            Exception("permanent auth error"),
            cause = SfuConnectFailureCause.TerminalSocketFailure,
        )

        val result = coordinator().join()
        advanceUntilIdle()

        assertThat(result).isInstanceOf(Failure::class.java)
        assertThat(connectionFlow.value).isInstanceOf(RealtimeConnection.Failed::class.java)
    }

    @Test
    fun `join retries transient errors and gives up after three attempts`() = runTest(
        testDispatcher,
    ) {
        // "Unable to resolve host" is treated as transient, so the loop retries.
        stubJoinCall(
            Failure(Error.ThrowableError("Unable to resolve host \"sfu\"", Exception("dns"))),
        )

        val result = coordinator().join()
        advanceUntilIdle()

        assertThat(result).isInstanceOf(Failure::class.java)
        // The join request is issued once per retry (3 attempts total).
        coVerify(exactly = 3) {
            apiClient.joinRequest(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `join returns the existing session when the call is already joined`() = runTest(
        testDispatcher,
    ) {
        val existing = mockk<RtcSession>(relaxed = true)
        sessionFlow.value = existing
        connectionFlow.value = RealtimeConnection.Connected

        val result = coordinator().join()
        advanceUntilIdle()

        assertThat(result).isInstanceOf(Success::class.java)
        assertThat((result as Success).value).isSameInstanceAs(existing)
        assertThat(sessionFlow.value).isSameInstanceAs(existing)
        assertThat(connectionFlow.value).isEqualTo(RealtimeConnection.Connected)
        verify(exactly = 0) { sessionManager.setActiveSession(null) }
        verify(exactly = 1) { callAnalytics.joinAnalytics.onJoinFunctionStart() }
        coVerify(exactly = 0) {
            apiClient.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        }
        // The single outer gate must also stop a second RtcSession from being installed.
        verify(exactly = 0) { sessionManager.setActiveSession(mockSession) }
        verify { existing.sfuTracer.trace("join-already-joined", any()) }
    }

    @Test
    fun `join reports JoinInitiated even when the call scope is already cancelled`() = runTest(
        testDispatcher,
    ) {
        testScope.cancel()

        assertFailsWith<CancellationException> { coordinator().join() }

        val joinAnalytics = callAnalytics.joinAnalytics
        verify(exactly = 1) { joinAnalytics.onJoinFunctionStart() }
        coVerify(exactly = 0) {
            apiClient.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `joinInternal returns the existing session when the call is already joined`() = runTest(
        testDispatcher,
    ) {
        val existing = mockk<RtcSession>(relaxed = true)
        sessionFlow.value = existing

        val result = coordinator().joinInternal(
            joinAnalyticsModel = JoinAnalyticsModel(0, JoinReason.FirstAttempt),
        )
        advanceUntilIdle()

        assertThat(result).isInstanceOf(Success::class.java)
        assertThat((result as Success).value).isSameInstanceAs(existing)
        coVerify(exactly = 0) {
            apiClient.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        }
        verify { existing.sfuTracer.trace("join-already-joined", any()) }
        // The guard must not tear down the live session's SFU observers: nothing on this path
        // re-registers them, so cancelling here would silently stop event monitoring.
        verify(exactly = 0) { sessionMonitor.cancelSfuObservers() }
    }

    @Test
    fun `join fails when the location cannot be resolved`() = runTest(testDispatcher) {
        coEvery { clientImpl.getCachedLocation() } returns
            Failure(Error.GenericError("no location"))

        val result = coordinator().joinInternal(
            joinAnalyticsModel = JoinAnalyticsModel(0, JoinReason.FirstAttempt),
        )

        assertThat(result).isInstanceOf(Failure::class.java)
    }

    @Test
    fun `isPermanentError treats host-resolution failures as transient`() = runTest(
        testDispatcher,
    ) {
        val coordinator = coordinator()
        val transient = Error.ThrowableError("Unable to resolve host", Exception("dns"))
        val permanent = Error.GenericError("server error")

        assertThat(coordinator.isPermanentError(transient)).isFalse()
        assertThat(coordinator.isPermanentError(permanent)).isTrue()
    }

    @Test
    fun `concurrent joins issue a single coordinator join and share one session`() = runTest(
        testDispatcher,
    ) {
        stubJoinCall(Success(mockJoinResponse))
        // Suspends until released, so all callers are inside join at the same time —
        // which is exactly the window the old session.value check failed to cover.
        val connectGate = CompletableDeferred<Unit>()
        coEvery { mockSession.connectInternal() } coAnswers {
            connectGate.await()
            SfuConnectionResult.Success
        }
        val coordinator = coordinator()

        val joins = (1..5).map {
            async { coordinator.join() }
        }
        advanceUntilIdle()
        connectGate.complete(Unit)
        val results = joins.awaitAll()
        advanceUntilIdle()

        results.forEach { assertThat(it).isInstanceOf(Success::class.java) }
        assertThat(results.map { (it as Success).value }.distinct()).hasSize(1)
        // One join request and one SFU connect for five callers.
        coVerify(exactly = 1) {
            apiClient.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 1) { mockSession.connectInternal() }
        verify(exactly = 1) { sessionManager.setActiveSession(mockSession) }
        verify(exactly = 4) { mockSession.sfuTracer.trace("join-coalesced", any()) }
        val joinAnalytics = callAnalytics.joinAnalytics
        verify(exactly = 5) { joinAnalytics.onJoinFunctionStart() }
    }

    @Test
    fun `concurrent joins run the join setup exactly once`() = runTest(testDispatcher) {
        stubJoinCall(Success(mockJoinResponse))
        val connectGate = CompletableDeferred<Unit>()
        coEvery { mockSession.connectInternal() } coAnswers {
            connectGate.await()
            SfuConnectionResult.Success
        }
        val coordinator = coordinator()
        val interceptor = mockk<CallJoinInterceptor>(relaxed = true)

        // The interceptor-carrying caller goes first, then a bare join() like the auto-join in
        // CallState — which used to overwrite the interceptor with null.
        val first = async { coordinator.join(callJoinInterceptor = interceptor) }
        advanceUntilIdle()
        val second = async { coordinator.join() }
        advanceUntilIdle()
        connectGate.complete(Unit)
        val results = listOf(first, second).awaitAll()
        advanceUntilIdle()

        results.forEach { assertThat(it).isInstanceOf(Success::class.java) }
        val joinAnalytics = callAnalytics.joinAnalytics
        verify(exactly = 2) { joinAnalytics.onJoinFunctionStart() }
        verify(exactly = 1) { callAnalytics.mediaPermissionObserver.mediaPermissionStatus() }
        verify(exactly = 1) { lifecycle.resetLeaveGuard() }
        verify { state.callJoinInterceptor = interceptor }
        verify(exactly = 0) { state.callJoinInterceptor = null }
    }

    @Test
    fun `cancelled first waiter yields the next still-active interceptor`() = runTest(
        testDispatcher,
    ) {
        stubJoinCall(Success(mockJoinResponse))
        val connectGate = CompletableDeferred<Unit>()
        coEvery { mockSession.connectInternal() } coAnswers {
            connectGate.await()
            SfuConnectionResult.Success
        }
        val coordinator = coordinator()
        val firstInterceptor = mockk<CallJoinInterceptor>(relaxed = true)
        val secondInterceptor = mockk<CallJoinInterceptor>(relaxed = true)

        val first = async { coordinator.join(callJoinInterceptor = firstInterceptor) }
        advanceUntilIdle()
        first.cancel()
        advanceUntilIdle()
        assertFailsWith<CancellationException> { first.await() }

        val second = async { coordinator.join(callJoinInterceptor = secondInterceptor) }
        advanceUntilIdle()
        verify { state.callJoinInterceptor = secondInterceptor }

        connectGate.complete(Unit)
        assertThat(second.await()).isInstanceOf(Success::class.java)
    }

    @Test
    fun `active first waiter keeps its interceptor when a later join coalesces`() = runTest(
        testDispatcher,
    ) {
        stubJoinCall(Success(mockJoinResponse))
        val connectGate = CompletableDeferred<Unit>()
        coEvery { mockSession.connectInternal() } coAnswers {
            connectGate.await()
            SfuConnectionResult.Success
        }
        val coordinator = coordinator()
        val firstInterceptor = mockk<CallJoinInterceptor>(relaxed = true)
        val secondInterceptor = mockk<CallJoinInterceptor>(relaxed = true)

        val first = async { coordinator.join(callJoinInterceptor = firstInterceptor) }
        advanceUntilIdle()
        val second = async { coordinator.join(callJoinInterceptor = secondInterceptor) }
        advanceUntilIdle()

        verify { state.callJoinInterceptor = firstInterceptor }

        connectGate.complete(Unit)
        listOf(first, second).awaitAll()
        advanceUntilIdle()
        verify { state.callJoinInterceptor = firstInterceptor }
    }

    @Test
    fun `leader installs interceptor before awaiting the guest token`() = runTest(
        testDispatcher,
    ) {
        stubJoinCall(Success(mockJoinResponse))
        coEvery { mockSession.connectInternal() } returns SfuConnectionResult.Success
        val guestGate = CompletableDeferred<Unit>()
        every { clientImpl.guestUserJob } returns testScope.async { guestGate.await() }
        val interceptor = mockk<CallJoinInterceptor>(relaxed = true)
        val coordinator = coordinator()

        val join = async { coordinator.join(callJoinInterceptor = interceptor) }
        advanceUntilIdle()

        verify(exactly = 1) { state.callJoinInterceptor = interceptor }
        coVerify(exactly = 0) {
            apiClient.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        }

        guestGate.complete(Unit)
        advanceUntilIdle()
        assertThat(join.await()).isInstanceOf(Success::class.java)
    }

    @Test
    fun `a join after the previous one finished starts a fresh attempt`() = runTest(
        testDispatcher,
    ) {
        stubJoinCall(Success(mockJoinResponse))
        coEvery { mockSession.connectInternal() } returns SfuConnectionResult.Success
        val coordinator = coordinator()

        coordinator.join()
        advanceUntilIdle()
        // The completed in-flight join must not be reused, otherwise a later join() would
        // replay a stale result instead of starting again.
        sessionFlow.value = null
        coordinator.join()
        advanceUntilIdle()

        coVerify(exactly = 2) {
            apiClient.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `cancelling one waiter leaves the shared join running for others`() = runTest(
        testDispatcher,
    ) {
        stubJoinCall(Success(mockJoinResponse))
        val connectGate = CompletableDeferred<Unit>()
        coEvery { mockSession.connectInternal() } coAnswers {
            connectGate.await()
            SfuConnectionResult.Success
        }
        val coordinator = coordinator()

        // Two different caller jobs (e.g. Activity A and Activity B / CallState auto-join).
        val first = async { coordinator.join() }
        advanceUntilIdle()
        val second = async { coordinator.join() }
        advanceUntilIdle()

        first.cancel()
        advanceUntilIdle()
        assertFailsWith<CancellationException> { first.await() }

        // Shared call-scoped join must still be alive for the second waiter.
        connectGate.complete(Unit)
        val secondResult = second.await()
        advanceUntilIdle()

        assertThat(secondResult).isInstanceOf(Success::class.java)
        coVerify(exactly = 1) {
            apiClient.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 1) { mockSession.connectInternal() }
    }

    @Test
    fun `cancelling the last waiter does not abort the shared join`() = runTest(testDispatcher) {
        val joinRequestGate = CompletableDeferred<Unit>()
        coEvery {
            apiClient.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            joinRequestGate.await()
            Success(mockJoinResponse)
        }
        coEvery { mockSession.connectInternal() } returns SfuConnectionResult.Success
        val coordinator = coordinator()

        val first = async { coordinator.join() }
        advanceUntilIdle()
        val second = async { coordinator.join() }
        advanceUntilIdle()

        first.cancel()
        second.cancel()
        advanceUntilIdle()
        assertFailsWith<CancellationException> { first.await() }
        assertFailsWith<CancellationException> { second.await() }

        joinRequestGate.complete(Unit)
        advanceUntilIdle()

        // Call-scoped join keeps running after the UI waiters drop (incoming accept can
        // finish/recreate the Activity). Leave still aborts it by cancelling the call scope.
        assertThat(sessionFlow.value).isSameInstanceAs(mockSession)
        verify { sessionManager.setActiveSession(mockSession) }

        val retry = coordinator.join()
        advanceUntilIdle()
        assertThat(retry).isInstanceOf(Success::class.java)
        assertThat((retry as Success).value).isSameInstanceAs(mockSession)
        coVerify(exactly = 1) {
            apiClient.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `cancelling the sole waiter does not abort the call-scoped join`() = runTest(
        testDispatcher,
    ) {
        val joinRequestGate = CompletableDeferred<Unit>()
        coEvery {
            apiClient.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            joinRequestGate.await()
            Success(mockJoinResponse)
        }
        coEvery { mockSession.connectInternal() } returns SfuConnectionResult.Success
        val coordinator = coordinator()

        val join = async { coordinator.join() }
        advanceUntilIdle()
        join.cancel()
        advanceUntilIdle()
        assertFailsWith<CancellationException> { join.await() }

        joinRequestGate.complete(Unit)
        advanceUntilIdle()

        assertThat(sessionFlow.value).isSameInstanceAs(mockSession)
        verify { sessionManager.setActiveSession(mockSession) }
    }

    @Test
    fun `cancelling after setActiveSession lets the in-flight join finish`() = runTest(
        testDispatcher,
    ) {
        stubJoinCall(Success(mockJoinResponse))
        val connectGate = CompletableDeferred<Unit>()
        coEvery { mockSession.connectInternal() } coAnswers {
            connectGate.await()
            SfuConnectionResult.Success
        }
        val coordinator = coordinator()

        val join = async { coordinator.join() }
        advanceUntilIdle()
        assertThat(sessionFlow.value).isSameInstanceAs(mockSession)
        assertThat(connectionFlow.value).isInstanceOf(RealtimeConnection.Joined::class.java)

        join.cancel()
        advanceUntilIdle()
        assertFailsWith<CancellationException> { join.await() }

        connectGate.complete(Unit)
        advanceUntilIdle()

        assertThat(sessionFlow.value).isSameInstanceAs(mockSession)
        assertThat(connectionFlow.value).isInstanceOf(RealtimeConnection.Joined::class.java)
        verify(exactly = 0) { mockSession.cleanup() }

        val retry = coordinator.join()
        advanceUntilIdle()
        assertThat(retry).isInstanceOf(Success::class.java)
        assertThat((retry as Success).value).isSameInstanceAs(mockSession)
        coVerify(exactly = 1) {
            apiClient.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `a session that cannot connect is cleaned up rather than left running`() = runTest(
        testDispatcher,
    ) {
        stubJoinCall(Success(mockJoinResponse))
        coEvery { mockSession.connectInternal() } returns SfuConnectionResult.Failure(
            Exception("permanent auth error"),
            cause = SfuConnectFailureCause.TerminalSocketFailure,
        )

        val result = coordinator().joinInternal(
            joinAnalyticsModel = JoinAnalyticsModel(0, JoinReason.FirstAttempt),
        )
        advanceUntilIdle()

        assertThat(result).isInstanceOf(Failure::class.java)
        verify { mockSession.cleanup() }
        assertThat(sessionFlow.value).isNull()
    }

    @Test
    fun `failed recovery tears down the join session and any reconnect replacement`() = runTest(
        testDispatcher,
    ) {
        stubJoinCall(Success(mockJoinResponse))
        val replacement = mockk<RtcSession>(relaxed = true)
        coEvery { mockSession.connectInternal() } coAnswers {
            // Reconnect swapped the active session before recovery settled as failed.
            sessionFlow.value = replacement
            connectionFlow.value = RealtimeConnection.ReconnectingFailed
            SfuConnectionResult.Failure(
                Exception("recoverable socket failure"),
                cause = SfuConnectFailureCause.RecoverableSocketFailure,
            )
        }

        val result = coordinator().joinInternal(
            joinAnalyticsModel = JoinAnalyticsModel(0, JoinReason.FirstAttempt),
        )
        advanceUntilIdle()

        assertThat(result).isInstanceOf(Failure::class.java)
        verify { mockSession.cleanup() }
        verify { replacement.cleanup() }
        assertThat(sessionFlow.value).isNull()
    }

    @Test
    fun `joinAndRing joins then rings the members`() = runTest(testDispatcher) {
        stubJoinCall(Success(mockJoinResponse))
        coEvery { mockSession.connectInternal() } returns SfuConnectionResult.Success
        coEvery { apiClient.ring(any<RingCallRequest>()) } returns
            Success(mockk<RingCallResponse>(relaxed = true))

        val result = coordinator().joinAndRing(members = listOf("u1"))
        advanceUntilIdle()

        assertThat(result).isInstanceOf(Success::class.java)
        coVerify { apiClient.ring(any<RingCallRequest>()) }
    }

    @Test
    fun `joinAndRing leaves the call when ringing fails`() = runTest(testDispatcher) {
        stubJoinCall(Success(mockJoinResponse))
        coEvery { mockSession.connectInternal() } returns SfuConnectionResult.Success
        coEvery { apiClient.ring(any<RingCallRequest>()) } returns
            Failure(Error.GenericError("ring failed"))

        val result = coordinator().joinAndRing(members = listOf("u1"))
        advanceUntilIdle()

        assertThat(result).isInstanceOf(Failure::class.java)
        coVerify { lifecycle.leave(any<CallLeaveReason>()) }
    }
}
