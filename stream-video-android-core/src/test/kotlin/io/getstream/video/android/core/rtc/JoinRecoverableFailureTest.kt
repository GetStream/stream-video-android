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
import io.getstream.video.android.core.call.SfuConnectionResult
import io.getstream.video.android.core.internal.module.CoordinatorConnectionModule
import io.getstream.video.android.core.internal.network.NetworkStateProvider
import io.getstream.video.android.model.User
import io.mockk.MockKAnnotations
import io.mockk.coEvery
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

/**
 * Tests the initial-join handling of a recoverable SFU connection failure
 * (e.g. a connection timeout) in [Call._join].
 *
 * On a recoverable failure the RtcSession's stateJob has already triggered
 * [Call.reconnect], so `_join` must defer to that single recovery loop and await
 * its terminal outcome instead of declaring a permanent failure. A non-recoverable
 * failure must fail immediately.
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

        // Inject our mocked RtcSession instead of constructing a real one.
        Call.testInstanceProvider.rtcSessionCreator = { mockSession }
    }

    @After
    fun tearDown() {
        Call.testInstanceProvider.rtcSessionCreator = null
        StreamVideo.removeClient()
        unmockkAll()
    }

    @Test
    fun `recoverable failure returns failure when reconnect is exhausted`() = runTest(
        testDispatcher,
    ) {
        coEvery { mockSession.connectInternal(any(), any()) } returns
            SfuConnectionResult.Failure(Exception("SFU connection timed out"), recoverable = true)

        val deferred = async {
            call._join(joinAnalyticsModel = JoinAnalyticsModel(0, JoinReason.FirstAttempt))
        }
        advanceUntilIdle()
        assertThat(deferred.isCompleted).isFalse()

        // The reconnect loop gives up.
        call.state._connection.value = RealtimeConnection.ReconnectingFailed
        advanceUntilIdle()

        assertThat(deferred.await()).isInstanceOf(Failure::class.java)
    }

    @Test
    fun `non-recoverable failure fails immediately without awaiting reconnect`() = runTest(
        testDispatcher,
    ) {
        coEvery { mockSession.connectInternal(any(), any()) } returns
            SfuConnectionResult.Failure(Exception("permanent auth error"), recoverable = false)

        val result = call._join(joinAnalyticsModel = JoinAnalyticsModel(0, JoinReason.FirstAttempt))

        assertThat(result).isInstanceOf(Failure::class.java)
    }
}
