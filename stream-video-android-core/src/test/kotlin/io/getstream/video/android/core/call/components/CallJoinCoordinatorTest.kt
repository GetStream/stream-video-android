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
import io.getstream.android.video.generated.models.RingCallRequest
import io.getstream.android.video.generated.models.RingCallResponse
import io.getstream.result.Error
import io.getstream.result.Result.Failure
import io.getstream.result.Result.Success
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MediaManagerImpl
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests the join orchestration extracted into [CallJoinCoordinator]: the public
 * [Call.join] retry loop, join-and-ring, the coordinator's own join request, and the
 * permanent-vs-transient error handling. Exercised through the [Call] facade with the
 * RtcSession injected via [Call.unitTestRtcSessionFactory].
 */
class CallJoinCoordinatorTest {

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
        every {
            mockClientImpl.permissionCheck.checkAndroidPermissionsGroup(any(), any())
        } returns Pair(true, emptySet())

        mockJoinResponse = mockk(relaxed = true)

        Call.testInstanceProvider.mediaManagerCreator = { mockk<MediaManagerImpl>(relaxed = true) }

        call = spyk(
            Call(
                client = mockClientImpl,
                type = "default",
                id = "test-call",
                user = User(id = "test-user", role = "user"),
            ),
        )
        repointComponentToSpy("joinCoordinator", call)
        call.unitTestRtcSessionFactory = { mockSession }
        every { call.monitorSession(any()) } returns Unit
    }

    @After
    fun tearDown() {
        Call.testInstanceProvider.mediaManagerCreator = null
        StreamVideo.removeClient()
        unmockkAll()
    }

    private fun repointComponentToSpy(fieldName: String, spy: Call) {
        val field = Call::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        val component = field.get(spy)
        val callField = component.javaClass.getDeclaredField("call")
        callField.isAccessible = true
        callField.set(component, spy)
    }

    @Test
    fun `join succeeds and returns the connected session`() = runTest(testDispatcher) {
        coEvery {
            call.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Success(mockJoinResponse)
        coEvery { mockSession.connectInternal(any(), any()) } returns SfuConnectionResult.Success

        val result = call.join()
        advanceUntilIdle()

        assertThat(result).isInstanceOf(Success::class.java)
        assertThat((result as Success).value).isSameInstanceAs(mockSession)
        coVerify { call.monitorSession(mockJoinResponse) }
    }

    @Test
    fun `join fails permanently on a terminal SFU failure without retrying`() = runTest(
        testDispatcher,
    ) {
        coEvery {
            call.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Success(mockJoinResponse)
        coEvery { mockSession.connectInternal(any(), any()) } returns SfuConnectionResult.Failure(
            Exception("permanent auth error"),
            cause = SfuConnectFailureCause.TerminalSocketFailure,
        )

        val result = call.join()
        advanceUntilIdle()

        assertThat(result).isInstanceOf(Failure::class.java)
        assertThat(call.state.connection.value).isInstanceOf(RealtimeConnection.Failed::class.java)
    }

    @Test
    fun `join retries transient errors and gives up after three attempts`() = runTest(
        testDispatcher,
    ) {
        // "Unable to resolve host" is treated as transient, so the loop retries.
        coEvery {
            call.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Failure(Error.ThrowableError("Unable to resolve host \"sfu\"", Exception("dns")))

        val result = call.join()
        advanceUntilIdle()

        assertThat(result).isInstanceOf(Failure::class.java)
        // joinRequest is attempted once per retry (3 attempts total).
        coVerify(exactly = 3) {
            call.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `join fails when the call is already joined`() = runTest(testDispatcher) {
        call.session.value = mockk(relaxed = true)

        val result = call._join(joinAnalyticsModel = JoinAnalyticsModel(0, JoinReason.FirstAttempt))

        assertThat(result).isInstanceOf(Failure::class.java)
    }

    @Test
    fun `join fails when the location cannot be resolved`() = runTest(testDispatcher) {
        coEvery { mockClientImpl.getCachedLocation() } returns
            Failure(Error.GenericError("no location"))

        val result = call._join(joinAnalyticsModel = JoinAnalyticsModel(0, JoinReason.FirstAttempt))

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
        coEvery {
            call.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Success(mockJoinResponse)
        coEvery { mockSession.connectInternal(any(), any()) } returns SfuConnectionResult.Success
        coEvery { call.ring(any<RingCallRequest>()) } returns
            Success(mockk<RingCallResponse>(relaxed = true))

        val result = call.joinAndRing(members = listOf("u1"))
        advanceUntilIdle()

        assertThat(result).isInstanceOf(Success::class.java)
        coVerify { call.ring(any<RingCallRequest>()) }
    }

    @Test
    fun `joinAndRing leaves the call when ringing fails`() = runTest(testDispatcher) {
        coEvery {
            call.joinRequest(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Success(mockJoinResponse)
        coEvery { mockSession.connectInternal(any(), any()) } returns SfuConnectionResult.Success
        coEvery { call.ring(any<RingCallRequest>()) } returns
            Failure(Error.GenericError("ring failed"))
        every { call.leave(any<io.getstream.video.android.core.CallLeaveReason>()) } returns Unit

        val result = call.joinAndRing(members = listOf("u1"))
        advanceUntilIdle()

        assertThat(result).isInstanceOf(Failure::class.java)
        coVerify { call.leave(any<io.getstream.video.android.core.CallLeaveReason>()) }
    }

    @Test
    fun `joinRequest delegates to the coordinator client`() = runTest(testDispatcher) {
        coEvery {
            mockClientImpl.joinCall(
                any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(),
            )
        } returns Failure(Error.GenericError("boom"))

        val result = call.joinRequest(
            location = "test-location",
            joinAnalyticsModel = JoinAnalyticsModel(0, JoinReason.FirstAttempt),
        )

        assertThat(result).isInstanceOf(Failure::class.java)
        coVerify {
            mockClientImpl.joinCall(
                any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(),
            )
        }
    }

    private fun coordinator(): CallJoinCoordinator {
        val field = Call::class.java.getDeclaredField("joinCoordinator")
        field.isAccessible = true
        return field.get(call) as CallJoinCoordinator
    }
}
