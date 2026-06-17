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
import kotlinx.coroutines.test.advanceUntilIdle
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
            peerConnectionHashCode = 42,
            role = PeerConnectionRole.SUBSCRIBE,
            iceState = VideoAnalyticsIceState.NOT_CONNECTED,
            peerConnectionState = PeerConnection.PeerConnectionState.CONNECTING,
        )

        verify(exactly = 1) {
            reporter.onPeerConnectionStateChanged(
                peerConnectionHashCode = 42,
                callId = "call-1",
                callType = "default",
                joinStageAttemptId = "attempt-7",
                callSessionId = "session-7",
                sfuId = "sfu-7",
                joinReason = JoinReason.ReJoin,
                role = PeerConnectionRole.SUBSCRIBE,
                iceState = VideoAnalyticsIceState.NOT_CONNECTED,
                peerConnectionState = PeerConnection.PeerConnectionState.CONNECTING,
            )
        }
    }

    @Test
    fun `a connecting publisher reports its current ice state immediately and marks the stage in progress`() = runTest {
        val session = mockSession(
            publisherState = PeerConnection.PeerConnectionState.CONNECTING,
            publisherIceState = PeerConnection.IceConnectionState.CONNECTED,
        )
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        analytics(scope).observePeerConnections(MutableStateFlow<RtcSession?>(session))
        runCurrent()

        assertEquals(Stage.IN_PROGRESS, stateHolder.state.value.publisherStage)
        verify {
            reporter.onPeerConnectionStateChanged(
                peerConnectionHashCode = any(),
                callId = "call-1",
                callType = "default",
                joinStageAttemptId = any(),
                callSessionId = any(),
                sfuId = any(),
                joinReason = any(),
                role = PeerConnectionRole.PUBLISH,
                iceState = VideoAnalyticsIceState.CONNECTED,
                peerConnectionState = PeerConnection.PeerConnectionState.CONNECTING,
            )
        }
        scope.cancel()
    }

    @Test
    fun `a connected publisher whose ice never connects reports the current ice after the grace period`() = runTest {
        val publisher = mockk<Publisher>()
        every { publisher.state } returns
            MutableStateFlow<PeerConnection.PeerConnectionState?>(PeerConnection.PeerConnectionState.CONNECTED)
        // ICE stays DISCONNECTED (never reaches CONNECTED), so the grace period will elapse.
        every { publisher.iceState } returns
            MutableStateFlow<PeerConnection.IceConnectionState?>(PeerConnection.IceConnectionState.DISCONNECTED)

        val subscriber = mockk<Subscriber>()
        every { subscriber.state } returns
            MutableStateFlow<PeerConnection.PeerConnectionState?>(null)
        every { subscriber.iceState } returns
            MutableStateFlow<PeerConnection.IceConnectionState?>(null)

        val session = mockk<RtcSession>()
        every { session.publisher } returns MutableStateFlow<Publisher?>(publisher)
        every { session.subscriber } returns MutableStateFlow<Subscriber?>(subscriber)

        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        analytics(scope).observePeerConnections(MutableStateFlow<RtcSession?>(session))
        runCurrent()

        // While the grace period is running nothing is reported, and the stage is NOT yet marked
        // completed: the stage flips only when the completed event is actually emitted, so a leave
        // mid-grace still sees the session as in progress.
        assertEquals(Stage.NOT_STARTED, stateHolder.state.value.publisherStage)
        verify(exactly = 0) {
            reporter.onPeerConnectionStateChanged(
                peerConnectionHashCode = any(),
                callId = any(),
                callType = any(),
                joinStageAttemptId = any(),
                callSessionId = any(),
                sfuId = any(),
                joinReason = any(),
                role = PeerConnectionRole.PUBLISH,
                iceState = any(),
                peerConnectionState = any(),
            )
        }

        // Once the grace period elapses, the current (non-connected) ICE state is reported and the
        // stage is marked completed in lockstep.
        advanceUntilIdle()

        assertEquals(Stage.COMPLETED, stateHolder.state.value.publisherStage)
        verify(exactly = 1) {
            reporter.onPeerConnectionStateChanged(
                peerConnectionHashCode = any(),
                callId = "call-1",
                callType = "default",
                joinStageAttemptId = any(),
                callSessionId = any(),
                sfuId = any(),
                joinReason = any(),
                role = PeerConnectionRole.PUBLISH,
                iceState = VideoAnalyticsIceState.NOT_CONNECTED,
                peerConnectionState = PeerConnection.PeerConnectionState.CONNECTED,
            )
        }
        scope.cancel()
    }

    @Test
    fun `a connected publisher reports connected when ice connects within the grace period`() = runTest {
        val publisher = mockk<Publisher>()
        // ICE has not reached CONNECTED yet, so the connected peer connection keeps waiting.
        val iceState =
            MutableStateFlow<PeerConnection.IceConnectionState?>(
                PeerConnection.IceConnectionState.DISCONNECTED,
            )
        every { publisher.state } returns
            MutableStateFlow<PeerConnection.PeerConnectionState?>(PeerConnection.PeerConnectionState.CONNECTED)
        every { publisher.iceState } returns iceState

        val subscriber = mockk<Subscriber>()
        every { subscriber.state } returns
            MutableStateFlow<PeerConnection.PeerConnectionState?>(null)
        every { subscriber.iceState } returns
            MutableStateFlow<PeerConnection.IceConnectionState?>(null)

        val session = mockk<RtcSession>()
        every { session.publisher } returns MutableStateFlow<Publisher?>(publisher)
        every { session.subscriber } returns MutableStateFlow<Subscriber?>(subscriber)

        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        analytics(scope).observePeerConnections(MutableStateFlow<RtcSession?>(session))
        runCurrent()

        verify(exactly = 0) {
            reporter.onPeerConnectionStateChanged(
                peerConnectionHashCode = any(),
                callId = any(),
                callType = any(),
                joinStageAttemptId = any(),
                callSessionId = any(),
                sfuId = any(),
                joinReason = any(),
                role = PeerConnectionRole.PUBLISH,
                iceState = any(),
                peerConnectionState = any(),
            )
        }

        iceState.value = PeerConnection.IceConnectionState.CONNECTED
        runCurrent()

        verify(exactly = 1) {
            reporter.onPeerConnectionStateChanged(
                peerConnectionHashCode = any(),
                callId = "call-1",
                callType = "default",
                joinStageAttemptId = any(),
                callSessionId = any(),
                sfuId = any(),
                joinReason = any(),
                role = PeerConnectionRole.PUBLISH,
                iceState = VideoAnalyticsIceState.CONNECTED,
                peerConnectionState = PeerConnection.PeerConnectionState.CONNECTED,
            )
        }
        scope.cancel()
    }

    @Test
    fun `a connecting publisher with a new ice state reports not connected`() = runTest {
        val session = mockSession(
            publisherState = PeerConnection.PeerConnectionState.CONNECTING,
            // NEW is allowed and maps to NOT_CONNECTED.
            publisherIceState = PeerConnection.IceConnectionState.NEW,
        )
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        analytics(scope).observePeerConnections(MutableStateFlow<RtcSession?>(session))
        runCurrent()

        assertEquals(Stage.IN_PROGRESS, stateHolder.state.value.publisherStage)
        verify(exactly = 1) {
            reporter.onPeerConnectionStateChanged(
                peerConnectionHashCode = any(),
                callId = "call-1",
                callType = "default",
                joinStageAttemptId = any(),
                callSessionId = any(),
                sfuId = any(),
                joinReason = any(),
                role = PeerConnectionRole.PUBLISH,
                iceState = VideoAnalyticsIceState.NOT_CONNECTED,
                peerConnectionState = PeerConnection.PeerConnectionState.CONNECTING,
            )
        }
        scope.cancel()
    }

    @Test
    fun `a failed ice state reports failed`() = runTest {
        val session = mockSession(
            publisherState = PeerConnection.PeerConnectionState.FAILED,
            publisherIceState = PeerConnection.IceConnectionState.FAILED,
        )
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        analytics(scope).observePeerConnections(MutableStateFlow<RtcSession?>(session))
        runCurrent()

        assertEquals(Stage.COMPLETED, stateHolder.state.value.publisherStage)
        verify(exactly = 1) {
            reporter.onPeerConnectionStateChanged(
                peerConnectionHashCode = any(),
                callId = "call-1",
                callType = "default",
                joinStageAttemptId = any(),
                callSessionId = any(),
                sfuId = any(),
                joinReason = any(),
                role = PeerConnectionRole.PUBLISH,
                iceState = VideoAnalyticsIceState.FAILED,
                peerConnectionState = PeerConnection.PeerConnectionState.FAILED,
            )
        }
        scope.cancel()
    }

    @Test
    fun `toVideoAnalyticsIceState maps webrtc ice states to the analytics enum`() {
        assertEquals(
            VideoAnalyticsIceState.CONNECTED,
            PeerConnection.IceConnectionState.CONNECTED.toVideoAnalyticsIceState(),
        )
        assertEquals(
            VideoAnalyticsIceState.CONNECTED,
            PeerConnection.IceConnectionState.COMPLETED.toVideoAnalyticsIceState(),
        )
        assertEquals(
            VideoAnalyticsIceState.FAILED,
            PeerConnection.IceConnectionState.FAILED.toVideoAnalyticsIceState(),
        )
        assertEquals(
            VideoAnalyticsIceState.NOT_CONNECTED,
            PeerConnection.IceConnectionState.NEW.toVideoAnalyticsIceState(),
        )
        assertEquals(
            VideoAnalyticsIceState.NOT_CONNECTED,
            PeerConnection.IceConnectionState.CHECKING.toVideoAnalyticsIceState(),
        )
        assertEquals(
            VideoAnalyticsIceState.NOT_CONNECTED,
            PeerConnection.IceConnectionState.DISCONNECTED.toVideoAnalyticsIceState(),
        )
        assertEquals(
            VideoAnalyticsIceState.NOT_CONNECTED,
            PeerConnection.IceConnectionState.CLOSED.toVideoAnalyticsIceState(),
        )
        val nullIceState: PeerConnection.IceConnectionState? = null
        assertEquals(
            VideoAnalyticsIceState.NOT_CONNECTED,
            nullIceState.toVideoAnalyticsIceState(),
        )
    }

    @Test
    fun `a connected publisher with ice already connected reports connected without waiting`() = runTest {
        val session = mockSession(
            publisherState = PeerConnection.PeerConnectionState.CONNECTED,
            publisherIceState = PeerConnection.IceConnectionState.CONNECTED,
        )
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        analytics(scope).observePeerConnections(MutableStateFlow<RtcSession?>(session))
        // No time is advanced: the grace period must not delay an already-connected ICE.
        runCurrent()

        verify(exactly = 1) {
            reporter.onPeerConnectionStateChanged(
                peerConnectionHashCode = any(),
                callId = "call-1",
                callType = "default",
                joinStageAttemptId = any(),
                callSessionId = any(),
                sfuId = any(),
                joinReason = any(),
                role = PeerConnectionRole.PUBLISH,
                iceState = VideoAnalyticsIceState.CONNECTED,
                peerConnectionState = PeerConnection.PeerConnectionState.CONNECTED,
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

        assertEquals(Stage.COMPLETED, stateHolder.state.value.publisherStage)
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
