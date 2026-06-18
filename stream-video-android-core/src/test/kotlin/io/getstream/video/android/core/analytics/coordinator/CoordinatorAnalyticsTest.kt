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

package io.getstream.video.android.core.analytics.coordinator

import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import io.getstream.video.android.core.socket.coordinator.CoordinatorSocketStateService
import io.getstream.video.android.core.socket.coordinator.state.VideoSocketConnectionType
import io.getstream.video.android.core.socket.coordinator.state.VideoSocketState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoordinatorAnalyticsTest {

    private val reporter = mockk<ClientEventReporter>(relaxed = true)
    private val socketState =
        MutableStateFlow<VideoSocketState>(VideoSocketState.Disconnected.Stopped)
    private var previousRetryAttempts = 0

    @Before
    fun setup() {
        mockkObject(StreamVideo)
        val streamVideo = mockk<StreamVideo>(relaxed = true)
        every { streamVideo.userId } returns "user-1"
        every { StreamVideo.instanceOrNull() } returns streamVideo

        every { reporter.reportCoordinatorWSInitiated() } returns "ws-stage-1"
        previousRetryAttempts = CoordinatorSocketStateService.lastRetryAttempts
        CoordinatorSocketStateService.lastRetryAttempts = 2
    }

    @After
    fun tearDown() {
        unmockkObject(StreamVideo)
        CoordinatorSocketStateService.lastRetryAttempts = previousRetryAttempts
    }

    private fun TestScope.startedAnalytics(): CoordinatorAnalytics {
        val analytics = CoordinatorAnalytics(
            CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            reporter,
            CoordinatorAnalyticsStateHolder(),
        )
        analytics.startObserver(socketState)
        return analytics
    }

    private fun connecting(type: VideoSocketConnectionType) =
        VideoSocketState.Connecting(mockk(), type)

    @Test
    fun `an initial connection reports coordinator ws initiated`() = runTest {
        startedAnalytics()

        socketState.value = connecting(VideoSocketConnectionType.INITIAL_CONNECTION)
        runCurrent()

        verify(exactly = 1) { reporter.reportCoordinatorWSInitiated() }
    }

    @Test
    fun `nothing is reported while the user id is not available`() = runTest {
        every { StreamVideo.instanceOrNull() } returns null
        startedAnalytics()

        socketState.value = connecting(VideoSocketConnectionType.INITIAL_CONNECTION)
        runCurrent()

        verify(exactly = 0) { reporter.reportCoordinatorWSInitiated() }
    }

    @Test
    fun `automatic and forced reconnections are not reported as initiated`() = runTest {
        startedAnalytics()

        socketState.value = connecting(VideoSocketConnectionType.AUTOMATIC_RECONNECTION)
        runCurrent()
        socketState.value = connecting(VideoSocketConnectionType.FORCE_RECONNECTION)
        runCurrent()

        verify(exactly = 0) { reporter.reportCoordinatorWSInitiated() }
    }

    @Test
    fun `connected after initiated reports completion with the retry count`() = runTest {
        startedAnalytics()

        socketState.value = connecting(VideoSocketConnectionType.INITIAL_CONNECTION)
        runCurrent()
        socketState.value = VideoSocketState.Connected(mockk())
        runCurrent()

        verify(exactly = 1) {
            reporter.reportCoordinatorWSCompleted("ws-stage-1", true, 2, null, null)
        }
    }

    @Test
    fun `connected without a prior initiated stage reports nothing`() = runTest {
        startedAnalytics()

        socketState.value = VideoSocketState.Connected(mockk())
        runCurrent()

        verify(exactly = 0) {
            reporter.reportCoordinatorWSCompleted(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `a permanent disconnect after initiated reports a failed completion`() = runTest {
        startedAnalytics()

        socketState.value = connecting(VideoSocketConnectionType.INITIAL_CONNECTION)
        runCurrent()
        socketState.value = VideoSocketState.Disconnected.DisconnectedPermanently(mockk())
        runCurrent()

        verify(exactly = 1) {
            reporter.reportCoordinatorWSCompleted("ws-stage-1", false, 2, null, null)
        }
    }

    @Test
    fun `endObserver stops reporting socket state changes`() = runTest {
        val analytics = startedAnalytics()

        analytics.endObserver()
        socketState.value = connecting(VideoSocketConnectionType.INITIAL_CONNECTION)
        runCurrent()

        verify(exactly = 0) { reporter.reportCoordinatorWSInitiated() }
    }
}
