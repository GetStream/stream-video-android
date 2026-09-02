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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
        every { state.e2eeEnabled } returns MutableStateFlow(false)
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
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
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
    fun `first join passes e2ee from call state`() = runTest(testDispatcher) {
        every { state.e2eeEnabled } returns MutableStateFlow(true)
        stubJoinCall(Success(mockJoinResponse))
        coEvery { mockSession.connectInternal() } returns SfuConnectionResult.Success

        coordinator().join()
        advanceUntilIdle()

        coVerify {
            apiClient.joinRequest(
                create = any(),
                location = any(),
                migratingFrom = any(),
                migratingFromList = any(),
                ring = any(),
                notify = any(),
                hintHighScaleLivestreamPublisher = any(),
                joinAnalyticsModel = any(),
                e2ee = true,
            )
        }
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
                any(),
            )
        }
    }

    @Test
    fun `join fails when the call is already joined`() = runTest(testDispatcher) {
        sessionFlow.value = mockk(relaxed = true)

        val result = coordinator().joinInternal(
            joinAnalyticsModel = JoinAnalyticsModel(0, JoinReason.FirstAttempt),
        )

        assertThat(result).isInstanceOf(Failure::class.java)
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
