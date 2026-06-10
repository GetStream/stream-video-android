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

package io.getstream.video.android.core.analytics.call.observer

import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.call.observer.model.Stage
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import io.getstream.video.android.core.analytics.reporting.model.PeerConnectionRole
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.connection.Publisher
import io.getstream.video.android.core.call.connection.Subscriber
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.webrtc.PeerConnection

@OptIn(ExperimentalCoroutinesApi::class)
class PeerConnectionAnalyticsTest {

    private val reporter = mockk<ClientEventReporter>(relaxed = true)
    private val joinHolder = JoinAnalyticsStateHolder()
    private val sfuHolder = SfuAnalyticsStateHolder()
    private val stateHolder = PeerConnectionAnalyticsStateHolder()

    private fun analytics(scope: CoroutineScope) = PeerConnectionAnalytics(
        "call-1",
        "default",
        scope,
        reporter,
        joinHolder,
        sfuHolder,
        stateHolder,
    )

    private fun mockSession(
        publisherState: PeerConnection.PeerConnectionState?,
        publisherIceState: PeerConnection.IceConnectionState?,
    ): RtcSession {
        val publisher = mockk<Publisher>()
        every { publisher.state } returns MutableStateFlow(publisherState)
        every { publisher.iceState } returns MutableStateFlow(publisherIceState)

        val subscriber = mockk<Subscriber>()
        every { subscriber.state } returns
            MutableStateFlow<PeerConnection.PeerConnectionState?>(null)
        every { subscriber.iceState } returns
            MutableStateFlow<PeerConnection.IceConnectionState?>(null)

        val session = mockk<RtcSession>()
        every { session.publisher } returns MutableStateFlow<Publisher?>(publisher)
        every { session.subscriber } returns MutableStateFlow<Subscriber?>(subscriber)
        return session
    }

    @Test
    fun `onPeerConnectionStateChanged forwards the shared analytics context`() {
        joinHolder.update(JoinReason.ReJoin, "attempt-7")
        joinHolder.updateCallSessionId("session-7")
        sfuHolder.updateSfuId("sfu-7")

        analytics(CoroutineScope(Dispatchers.Unconfined)).onPeerConnectionStateChanged(
            role = PeerConnectionRole.SUBSCRIBE,
            iceState = PeerConnection.IceConnectionState.CHECKING,
            peerConnectionState = PeerConnection.PeerConnectionState.CONNECTING,
        )

        verify(exactly = 1) {
            reporter.onPeerConnectionStateChanged(
                callId = "call-1",
                callType = "default",
                joinStageAttemptId = "attempt-7",
                callSessionId = "session-7",
                sfuId = "sfu-7",
                joinReason = JoinReason.ReJoin,
                role = PeerConnectionRole.SUBSCRIBE,
                iceState = PeerConnection.IceConnectionState.CHECKING,
                peerConnectionState = PeerConnection.PeerConnectionState.CONNECTING,
            )
        }
    }

    @Test
    fun `a connecting publisher marks the stage in progress and notifies the reporter`() = runTest {
        val session = mockSession(
            publisherState = PeerConnection.PeerConnectionState.CONNECTING,
            publisherIceState = PeerConnection.IceConnectionState.CHECKING,
        )
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        analytics(scope).observePeerConnections(MutableStateFlow<RtcSession?>(session))
        runCurrent()

        assertEquals(Stage.IN_PROGRESS, stateHolder.state.value.publisherStage)
        verify {
            reporter.onPeerConnectionStateChanged(
                callId = "call-1",
                callType = "default",
                joinStageAttemptId = any(),
                callSessionId = any(),
                sfuId = any(),
                joinReason = any(),
                role = PeerConnectionRole.PUBLISH,
                iceState = PeerConnection.IceConnectionState.CHECKING,
                peerConnectionState = PeerConnection.PeerConnectionState.CONNECTING,
            )
        }
        scope.cancel()
    }

    @Test
    fun `a connected publisher resets the stage back to completed`() = runTest {
        val session = mockSession(
            publisherState = PeerConnection.PeerConnectionState.CONNECTED,
            publisherIceState = PeerConnection.IceConnectionState.CONNECTED,
        )
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        analytics(scope).observePeerConnections(MutableStateFlow<RtcSession?>(session))
        runCurrent()

        assertEquals(Stage.NOT_STARTED, stateHolder.state.value.publisherStage)
        scope.cancel()
    }

    @Test
    fun `stop cancels the observer job and clears it from the state holder`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val peerConnectionAnalytics = analytics(scope)

        peerConnectionAnalytics.observePeerConnections(MutableStateFlow<RtcSession?>(null))
        runCurrent()
        val observerJob = stateHolder.state.value.peerConnectionObserverJob
        assertNotNull(observerJob)

        peerConnectionAnalytics.stop()

        assertTrue(observerJob!!.isCancelled)
        assertNull(stateHolder.state.value.peerConnectionObserverJob)
        scope.cancel()
    }
}
