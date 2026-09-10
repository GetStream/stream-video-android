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

package io.getstream.video.android.core.rtc

import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.CallSettingsResponse
import io.getstream.android.video.generated.models.JoinCallResponse
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
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
import io.getstream.video.android.core.call.components.CallApiClient
import io.getstream.video.android.core.call.components.CallJoinCoordinator
import io.getstream.video.android.core.call.components.CallLifecycleManager
import io.getstream.video.android.core.call.components.CallMediaManager
import io.getstream.video.android.core.call.components.CallReconnector
import io.getstream.video.android.core.call.components.CallSessionManager
import io.getstream.video.android.core.call.components.ClientCallRegistry
import io.getstream.video.android.core.call.components.RtcSessionFactory
import io.getstream.video.android.core.call.components.SessionMonitor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import stream.video.sfu.models.WebsocketReconnectStrategy

/**
 * Tests the initial-join handling of failed SFU connect attempts in
 * [CallJoinCoordinator.joinInternal] (reached publicly through `Call._join`).
 *
 * The failure cause decides how the join orchestrates recovery:
 * - [SfuConnectFailureCause.SocketStateObservationTimeout] starts a REJOIN
 *   because no reconnect loop was started by stateJob.
 * - [SfuConnectFailureCause.RecoverableSocketFailure] waits for the reconnect
 *   loop already started by stateJob.
 * - [SfuConnectFailureCause.TerminalSocketFailure] fails immediately.
 */
class JoinRecoverableFailureTest {

    @get:Rule
    val dispatcherRule = DispatcherRule()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var clientImpl: StreamVideoClient
    private lateinit var state: CallState
    private lateinit var sessionManager: CallSessionManager
    private lateinit var apiClient: CallApiClient
    private lateinit var reconnector: CallReconnector
    private lateinit var mockSession: RtcSession
    private lateinit var mockJoinResponse: JoinCallResponse
    private lateinit var connectionFlow: MutableStateFlow<RealtimeConnection>

    @Before
    fun setup() {
        clientImpl = mockk(relaxed = true)
        state = mockk(relaxed = true)
        reconnector = mockk(relaxed = true)
        mockSession = mockk(relaxed = true)
        mockJoinResponse = mockk(relaxed = true)

        sessionManager = CallSessionManager()
        apiClient = mockk(relaxed = true)
        connectionFlow = MutableStateFlow(RealtimeConnection.InProgress)

        every { state._connection } returns connectionFlow
        every { state.connection } returns connectionFlow
        every { state.settings } returns MutableStateFlow<CallSettingsResponse?>(null)
        every { state.e2eeEnabled } returns MutableStateFlow(false)
        coEvery { clientImpl.getCachedLocation() } returns Success("test-location")
        coEvery {
            apiClient.joinRequest(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        } returns Success(mockJoinResponse)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun coordinator() = CallJoinCoordinator(
        clientImpl = clientImpl,
        state = state,
        callAnalytics = mockk<CallAnalytics>(relaxed = true),
        type = "default",
        id = "test-call",
        scope = testScope,
        sessionManager = sessionManager,
        sessionFactory = RtcSessionFactory { _, _, _, _, _, _, _ -> mockSession },
        media = mockk<CallMediaManager>(relaxed = true),
        lifecycle = mockk<CallLifecycleManager>(relaxed = true),
        apiClient = apiClient,
        reconnector = reconnector,
        sessionMonitor = mockk<SessionMonitor>(relaxed = true),
        callRegistry = mockk<ClientCallRegistry>(relaxed = true),
        hasRequiredPermissions = { true },
    )

    private suspend fun join() = coordinator().joinInternal(
        joinAnalyticsModel = JoinAnalyticsModel(0, JoinReason.FirstAttempt),
    )

    @Test
    fun `recoverable socket failure awaits the existing reconnect loop`() = runTest(
        testDispatcher,
    ) {
        coEvery { mockSession.connectInternal(any(), any()) } returns
            SfuConnectionResult.Failure(
                Exception("SFU socket disconnected"),
                cause = SfuConnectFailureCause.RecoverableSocketFailure,
            )

        val deferred = async { join() }
        advanceUntilIdle()
        assertThat(deferred.isCompleted).isFalse()

        // stateJob owns the loop here; the join must not start its own.
        coVerify(exactly = 0) { reconnector.reconnect(any(), any()) }

        // The reconnect loop gives up.
        connectionFlow.value = RealtimeConnection.ReconnectingFailed
        advanceUntilIdle()

        assertThat(deferred.await()).isInstanceOf(Failure::class.java)
    }

    @Test
    fun `socket state observation timeout starts a REJOIN itself`() = runTest(
        testDispatcher,
    ) {
        coEvery { mockSession.connectInternal(any(), any()) } returns
            SfuConnectionResult.Failure(
                Exception("SFU connection timed out"),
                cause = SfuConnectFailureCause.SocketStateObservationTimeout,
            )

        val deferred = async { join() }
        advanceUntilIdle()

        // Nothing else would drive recovery, so the join must trigger a REJOIN.
        coVerify {
            reconnector.reconnect(
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
                any(),
            )
        }
        assertThat(deferred.isCompleted).isFalse()

        connectionFlow.value = RealtimeConnection.ReconnectingFailed
        advanceUntilIdle()

        assertThat(deferred.await()).isInstanceOf(Failure::class.java)
    }

    @Test
    fun `terminal socket failure fails immediately without awaiting reconnect`() = runTest(
        testDispatcher,
    ) {
        coEvery { mockSession.connectInternal(any(), any()) } returns
            SfuConnectionResult.Failure(
                Exception("permanent auth error"),
                cause = SfuConnectFailureCause.TerminalSocketFailure,
            )

        val result = join()

        assertThat(result).isInstanceOf(Failure::class.java)
        coVerify(exactly = 0) { reconnector.reconnect(any(), any()) }
    }
}
