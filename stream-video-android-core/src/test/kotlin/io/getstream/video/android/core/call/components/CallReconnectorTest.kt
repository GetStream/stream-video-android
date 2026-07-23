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
import io.getstream.android.video.generated.models.JoinCallResponse
import io.getstream.result.Result.Success
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallLeaveReason
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.SfuConnectionResult
import io.getstream.video.android.core.call.connection.Publisher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import stream.video.sfu.models.WebsocketReconnectStrategy

/**
 * Tests the reconnect state-machine branches of [CallReconnector] that do not require a
 * live [RtcSession]: early exits, the DISCONNECT strategy, and the precondition guards
 * for REJOIN / MIGRATE. The happy-path reconnect flows are covered by the RTC tests.
 */
class CallReconnectorTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var clientImpl: StreamVideoClient
    private lateinit var state: CallState
    private lateinit var connectionFlow: MutableStateFlow<RealtimeConnection>
    private lateinit var sessionFlow: MutableStateFlow<RtcSession?>
    private lateinit var call: Call

    @Before
    fun setup() {
        clientImpl = mockk(relaxed = true)
        state = mockk(relaxed = true)
        connectionFlow = MutableStateFlow(RealtimeConnection.Reconnecting)
        sessionFlow = MutableStateFlow(null)
        call = mockk(relaxed = true)

        every { clientImpl.leaveAfterDisconnectSeconds } returns 120L

        every { call.type } returns "default"
        every { call.id } returns "call-id"
        every { call.clientImpl } returns clientImpl
        every { call.scope } returns testScope
        every { call.state } returns state
        every { call.session } returns sessionFlow
        every { call.isDestroyed } returns false
        every { call.isNetworkConnected() } returns true
        every { call.reconnectDeadlineMillis } returns 60_000
        every { call.location } returns null
        every { state.connection } returns connectionFlow
        every { state._connection } returns connectionFlow
    }

    private fun reconnector() = CallReconnector(call)

    @Test
    fun `reconnect is skipped when the call is destroyed`() = runTest(testDispatcher) {
        every { call.isDestroyed } returns true

        reconnector().reconnect(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
            "test",
        )
        advanceUntilIdle()

        // No leave / failure driven when we bail out immediately.
        verify(exactly = 0) { call.leave(any<CallLeaveReason>()) }
    }

    @Test
    fun `reconnect is skipped when already disconnected`() = runTest(testDispatcher) {
        connectionFlow.value = RealtimeConnection.Disconnected

        reconnector().reconnect(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
            "test",
        )
        advanceUntilIdle()

        verify(exactly = 0) { call.leave(any<CallLeaveReason>()) }
    }

    @Test
    fun `disconnect strategy leaves the call`() = runTest(testDispatcher) {
        reconnector().reconnect(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_DISCONNECT,
            "server-disconnect",
        )
        advanceUntilIdle()

        verify { call.leave(any<CallLeaveReason>()) }
    }

    @Test
    fun `rejoin without a location gives up and leaves`() = runTest(testDispatcher) {
        every { call.location } returns null

        reconnector().reconnect(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
            "rejoin",
        )
        advanceUntilIdle()

        assertThat(connectionFlow.value)
            .isInstanceOf(RealtimeConnection.ReconnectingFailed::class.java)
        verify { call.leave(any<CallLeaveReason>()) }
    }

    @Test
    fun `fast reconnect without a session gives up and leaves`() = runTest(testDispatcher) {
        sessionFlow.value = null

        reconnector().reconnect(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_FAST,
            "fast",
        )
        advanceUntilIdle()

        assertThat(connectionFlow.value)
            .isInstanceOf(RealtimeConnection.ReconnectingFailed::class.java)
    }

    @Test
    fun `failed sfu id bookkeeping is exposed as a snapshot`() {
        val reconnector = reconnector()
        assertThat(reconnector.getFailedSfuIdsSnapshot()).isEmpty()
        reconnector.clearFailedSfuIds()
        assertThat(reconnector.getFailedSfuIdsSnapshot()).isEmpty()
    }

    @Test
    fun `strategy helpers forward to reconnect without throwing`() = runTest(testDispatcher) {
        val reconnector = reconnector()
        reconnector.fastReconnect("helper")
        reconnector.rejoin("helper")
        reconnector.migrate()
        advanceUntilIdle()
    }

    @Test
    fun `rejoin swaps in the new session and monitors it on success`() = runTest(testDispatcher) {
        val oldSession = mockk<RtcSession>(relaxed = true)
        val newSession = mockk<RtcSession>(relaxed = true)
        val joinResponse = mockk<JoinCallResponse>(relaxed = true)
        prepareRejoinOrMigrate(oldSession, newSession, joinResponse)
        coEvery { newSession.connectInternal(any(), any()) } returns SfuConnectionResult.Success

        reconnector().reconnect(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
            "rejoin",
        )
        advanceUntilIdle()

        assertThat(sessionFlow.value).isSameInstanceAs(newSession)
        coVerify { call.monitorSession(joinResponse) }
    }

    @Test
    fun `rejoin failures are retried until the attempts are exhausted`() = runTest(testDispatcher) {
        val oldSession = mockk<RtcSession>(relaxed = true)
        val newSession = mockk<RtcSession>(relaxed = true)
        val joinResponse = mockk<JoinCallResponse>(relaxed = true)
        prepareRejoinOrMigrate(oldSession, newSession, joinResponse)
        coEvery { newSession.connectInternal(any(), any()) } returns SfuConnectionResult.Failure(
            Exception("rejoin failed"),
            cause = io.getstream.video.android.core.call.SfuConnectFailureCause.RecoverableSocketFailure,
        )

        reconnector().reconnect(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
            "rejoin",
        )
        advanceUntilIdle()

        verify { call.leave(any<CallLeaveReason>()) }
    }

    @Test
    fun `migrate swaps in the new session and finalizes the old one on success`() = runTest(
        testDispatcher,
    ) {
        val oldSession = mockk<RtcSession>(relaxed = true)
        val newSession = mockk<RtcSession>(relaxed = true)
        val joinResponse = mockk<JoinCallResponse>(relaxed = true)
        prepareRejoinOrMigrate(oldSession, newSession, joinResponse)
        coEvery { newSession.connectInternal(any(), any()) } returns SfuConnectionResult.Success

        reconnector().reconnect(
            WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_MIGRATE,
            "migrate",
        )
        advanceUntilIdle()

        assertThat(sessionFlow.value).isSameInstanceAs(newSession)
        coVerify { oldSession.finalizeMigration() }
        coVerify { call.monitorSession(joinResponse) }
    }

    /**
     * Wires up the shared happy-path state a rejoin/migrate needs: a resolvable location,
     * an existing (old) session, a stubbed join request and an injected new session.
     */
    private fun prepareRejoinOrMigrate(
        oldSession: RtcSession,
        newSession: RtcSession,
        joinResponse: JoinCallResponse,
    ) {
        every { call.location } returns "test-location"
        sessionFlow.value = oldSession
        // The old session becomes the new one on every retry, so both need the same stubs.
        for (s in listOf(oldSession, newSession)) {
            every { s.currentSfuInfo() } returns Triple("prev-session", emptyList(), emptyList())
            every { s.publisher } returns MutableStateFlow<Publisher?>(null)
        }
        coEvery {
            call.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Success(joinResponse)
        every { call.unitTestRtcSessionFactory } returns { newSession }
    }
}
