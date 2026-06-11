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

package io.getstream.video.android.core.analytics.call

import io.getstream.video.android.core.BackendCause
import io.getstream.video.android.core.CallLeaveReason
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.UserActionCause
import io.getstream.video.android.core.analytics.call.observer.model.Stage
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import io.getstream.video.android.core.analytics.reporting.model.AnalyticsCallAbortReason
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallAnalyticsTest {

    private val reporter = mockk<ClientEventReporter>(relaxed = true)
    private val userLeave = CallLeaveReason.UserAction(UserActionCause.CANCELLED_BY_SELF)
    private val backendLeave = CallLeaveReason.Backend(BackendCause.CALL_ENDED_EVENT)

    private fun callAnalytics(scope: CoroutineScope) = CallAnalytics(
        context = mockk(relaxed = true),
        callId = "call-1",
        callType = "default",
        myParticipantState = MutableStateFlow<ParticipantState?>(null),
        connectionFlow = MutableStateFlow(RealtimeConnection.PreJoin),
        participants = MutableStateFlow<List<ParticipantState>>(emptyList()),
        eventReporter = reporter,
        observerScope = scope,
    )

    @Test
    fun `onCallLeave without any stage in progress does not abort`() = runTest {
        callAnalytics(backgroundScope).onCallLeave(userLeave)

        verify(exactly = 0) { reporter.abortAllPostCallInFlight(any(), any()) }
    }

    @Test
    fun `a client leave during an in-progress join aborts with CLIENT_ABORTED`() = runTest {
        val analytics = callAnalytics(backgroundScope)
        analytics.joinAnalyticsStateHolder.updateStage(Stage.IN_PROGRESS)

        analytics.onCallLeave(userLeave)

        verify(exactly = 1) {
            reporter.abortAllPostCallInFlight(AnalyticsCallAbortReason.CLIENT_ABORTED.name, any())
        }
    }

    @Test
    fun `a backend leave during an in-progress join aborts with BACKEND_LEAVE`() = runTest {
        val analytics = callAnalytics(backgroundScope)
        analytics.joinAnalyticsStateHolder.updateStage(Stage.IN_PROGRESS)

        analytics.onCallLeave(backendLeave)

        verify(exactly = 1) {
            reporter.abortAllPostCallInFlight(AnalyticsCallAbortReason.BACKEND_LEAVE.name, any())
        }
    }

    @Test
    fun `an in-progress sfu ws stage also triggers the abort`() = runTest {
        val analytics = callAnalytics(backgroundScope)
        analytics.sfuAnalyticsStateHolder.updateStage(Stage.IN_PROGRESS)

        analytics.onCallLeave(userLeave)

        verify(exactly = 1) {
            reporter.abortAllPostCallInFlight(AnalyticsCallAbortReason.CLIENT_ABORTED.name, any())
        }
    }

    @Test
    fun `an in-progress publisher peer connection also triggers the abort`() = runTest {
        val analytics = callAnalytics(backgroundScope)
        analytics.peerConnectionAnalytics.stateHolder.updatePublisherStage(Stage.IN_PROGRESS)

        analytics.onCallLeave(userLeave)

        verify(exactly = 1) {
            reporter.abortAllPostCallInFlight(AnalyticsCallAbortReason.CLIENT_ABORTED.name, any())
        }
    }

    @Test
    fun `an in-progress subscriber peer connection also triggers the abort`() = runTest {
        val analytics = callAnalytics(backgroundScope)
        analytics.peerConnectionAnalytics.stateHolder.updateSubscriberStage(Stage.IN_PROGRESS)

        analytics.onCallLeave(userLeave)

        verify(exactly = 1) {
            reporter.abortAllPostCallInFlight(AnalyticsCallAbortReason.CLIENT_ABORTED.name, any())
        }
    }

    @Test
    fun `stopObservers cancels the peer connection observer job`() = runTest {
        val analytics = callAnalytics(backgroundScope)
        val observerJob = Job()
        analytics.peerConnectionAnalytics.stateHolder.updatePeerConnectionObserverJob(observerJob)

        analytics.stopObservers()

        assertTrue(observerJob.isCancelled)
        assertNull(
            analytics.peerConnectionAnalytics.stateHolder.state.value.peerConnectionObserverJob,
        )
    }

    @Test
    fun `resetAfterJoinSuccess clears the first video frame dedupe state`() = runTest {
        val analytics = callAnalytics(backgroundScope)
        analytics.videoAnalytics.stageId.value = "stage-x"

        analytics.resetAfterJoinSuccess()

        assertEquals("", analytics.videoAnalytics.stageId.value)
    }
}
