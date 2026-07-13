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
import io.getstream.android.video.generated.models.JoinCallResponse
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.analytics.call.observer.model.JoinAnalyticsModel
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.base.DispatcherRule
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.SfuConnectFailureCause
import io.getstream.video.android.core.call.SfuConnectionResult
import io.getstream.video.android.core.internal.module.CoordinatorConnectionModule
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.model.User
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
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
 * Tests the initial-join handling of failed SFU connect attempts in [Call._join].
 *
 * The failure cause decides how `_join` orchestrates recovery:
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

    @RelaxedMockK
    private lateinit var mockClientImpl: StreamVideoClient

    @RelaxedMockK
    private lateinit var mockSession: RtcSession

    private lateinit var mockStreamVideo: StreamVideo
    private lateinit var mockJoinResponse: JoinCallResponse
    private lateinit var call: Call

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        mockStreamVideo = mockk(relaxed = true)
        StreamVideo.install(mockStreamVideo)

        val mockNetworkStateProvider = mockk<NetworkStateProvider>(relaxed = true)
        every { mockNetworkStateProvider.isConnected() } returns true
        val mockCoordinatorModule = mockk<CoordinatorConnectionModule>(relaxed = true)
        every { mockCoordinatorModule.networkStateProvider } returns mockNetworkStateProvider

        every { mockClientImpl.coordinatorConnectionModule } returns mockCoordinatorModule
        every { mockClientImpl.scope } returns testScope as CoroutineScope
        every { mockClientImpl.leaveAfterDisconnectSeconds } returns 120L
        every { mockClientImpl.apiKey } returns "test-api-key"
        coEvery { mockClientImpl.getCachedLocation() } returns Success("test-location")

        mockJoinResponse = mockk(relaxed = true)

        call = spyk(
            Call(
                client = mockClientImpl,
                type = "default",
                id = "test-call",
                user = User(id = "test-user", role = "user"),
            ),
        )

        // _join builds the JoinCallResponse via joinRequest; stub it out.
        coEvery {
            call.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Success(mockJoinResponse)

        // Inject our mocked RtcSession into this Call instead of constructing a real one.
        call.unitTestRtcSessionFactory = { mockSession }
    }

    @After
    fun tearDown() {
        StreamVideo.removeClient()
        unmockkAll()
    }

    @Test
    fun `recoverable socket failure awaits the existing reconnect loop`() = runTest(
        testDispatcher,
    ) {
        coEvery { mockSession.connectInternal(any(), any()) } returns
            SfuConnectionResult.Failure(
                Exception("SFU socket disconnected"),
                cause = SfuConnectFailureCause.RecoverableSocketFailure,
            )

        val deferred = async {
            call._join(joinAnalyticsModel = JoinAnalyticsModel(0, JoinReason.FirstAttempt))
        }
        advanceUntilIdle()
        assertThat(deferred.isCompleted).isFalse()

        // stateJob owns the loop here; _join must not start its own.
        coVerify(exactly = 0) { call.reconnect(any(), any()) }

        // The reconnect loop gives up.
        call.state._connection.value = RealtimeConnection.ReconnectingFailed
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
        // Stub the loop so we only assert it is invoked, not run it for real.
        coEvery { call.reconnect(any(), any()) } returns Unit

        val deferred = async {
            call._join(joinAnalyticsModel = JoinAnalyticsModel(0, JoinReason.FirstAttempt))
        }
        advanceUntilIdle()

        // Nothing else would drive recovery, so _join must trigger a REJOIN.
        coVerify {
            call.reconnect(
                WebsocketReconnectStrategy.WEBSOCKET_RECONNECT_STRATEGY_REJOIN,
                any(),
            )
        }
        assertThat(deferred.isCompleted).isFalse()

        call.state._connection.value = RealtimeConnection.ReconnectingFailed
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

        val result = call._join(joinAnalyticsModel = JoinAnalyticsModel(0, JoinReason.FirstAttempt))

        assertThat(result).isInstanceOf(Failure::class.java)
    }
}
