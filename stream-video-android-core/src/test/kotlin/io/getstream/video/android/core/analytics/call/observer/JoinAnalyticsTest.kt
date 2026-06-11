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

import io.getstream.video.android.core.analytics.call.observer.model.JoinAnalyticsModel
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.call.observer.model.Stage
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class JoinAnalyticsTest {

    private val reporter = mockk<ClientEventReporter>(relaxed = true)
    private val stateHolder = JoinAnalyticsStateHolder()
    private var joinSuccessCount = 0
    private lateinit var joinAnalytics: JoinAnalytics

    @Before
    fun setup() {
        joinSuccessCount = 0
        joinAnalytics = JoinAnalytics("call-1", "default", reporter, stateHolder) {
            joinSuccessCount++
        }
        every {
            reporter.reportCoordinatorJoinInitiated(any(), any(), any(), any())
        } returns "stage-1"
    }

    @Test
    fun `onJoinFunctionStart stores a fresh attempt id and reports the sdk join`() {
        joinAnalytics.onJoinFunctionStart()

        val attemptId = stateHolder.state.value.joinStageAttemptId
        assertNotNull(attemptId)
        verify(exactly = 1) {
            reporter.reportSdkMethodJoinInitiated(
                callId = "call-1",
                callType = "default",
                joinStageAttemptId = attemptId!!,
            )
        }
    }

    @Test
    fun `onJoinRequestStart reports the coordinator join and moves the stage in progress`() {
        joinAnalytics.onJoinRequestStart(JoinReason.FirstAttempt)

        assertEquals(Stage.IN_PROGRESS, stateHolder.state.value.joinStage)
        assertEquals("stage-1", stateHolder.state.value.stageId)
        assertEquals(JoinReason.FirstAttempt, stateHolder.state.value.joinReason)
        verify(exactly = 1) {
            reporter.reportCoordinatorJoinInitiated(any(), any(), any(), any())
        }
    }

    @Test
    fun `onJoinRequestStart while already in progress does not report again`() {
        joinAnalytics.onJoinRequestStart(JoinReason.FirstAttempt)
        joinAnalytics.onJoinRequestStart(JoinReason.FirstAttempt)

        verify(exactly = 1) {
            reporter.reportCoordinatorJoinInitiated(any(), any(), any(), any())
        }
    }

    @Test
    fun `a rejoin generates a new attempt id while a first attempt keeps it`() {
        stateHolder.updateJoinStageAttemptId("original-attempt")

        joinAnalytics.onJoinRequestStart(JoinReason.FirstAttempt)
        assertEquals("original-attempt", stateHolder.state.value.joinStageAttemptId)

        joinAnalytics.resetStage()
        joinAnalytics.onJoinRequestStart(JoinReason.ReJoin)
        assertNotEquals("original-attempt", stateHolder.state.value.joinStageAttemptId)
    }

    @Test
    fun `onJoinRequestSuccess completes the stage and notifies the callback`() {
        joinAnalytics.onJoinRequestStart(JoinReason.FirstAttempt)

        joinAnalytics.onJoinRequestSuccess(JoinAnalyticsModel(retryAttempt = 2), "session-9")

        verify(exactly = 1) {
            reporter.reportCoordinatorJoinCompleted("stage-1", true, 2, null, null, "session-9")
        }
        assertEquals(1, joinSuccessCount)
        assertEquals(Stage.COMPLETED, stateHolder.state.value.joinStage)
        assertEquals("session-9", stateHolder.state.value.callSessionId)
    }

    @Test
    fun `onJoinRequestSuccess without a started stage does nothing`() {
        joinAnalytics.onJoinRequestSuccess(JoinAnalyticsModel(retryAttempt = 0), "session-9")

        verify(exactly = 0) {
            reporter.reportCoordinatorJoinCompleted(any(), any(), any(), any(), any(), any())
        }
        assertEquals(0, joinSuccessCount)
    }

    @Test
    fun `onJoinRequestPermanentError completes the stage as failure`() {
        joinAnalytics.onJoinRequestStart(JoinReason.FirstAttempt)

        joinAnalytics.onJoinRequestPermanentError(retryCount = 3, message = "boom")

        verify(exactly = 1) {
            reporter.reportCoordinatorJoinCompleted("stage-1", false, 3, "boom", null, null)
        }
        assertEquals(Stage.COMPLETED, stateHolder.state.value.joinStage)
        assertEquals(0, joinSuccessCount)
    }

    @Test
    fun `onJoinRequestRetryExhausted behaves like a permanent error`() {
        joinAnalytics.onJoinRequestStart(JoinReason.FirstAttempt)

        joinAnalytics.onJoinRequestRetryExhausted(retryCount = 5, message = "exhausted")

        verify(exactly = 1) {
            reporter.reportCoordinatorJoinCompleted("stage-1", false, 5, "exhausted", null, null)
        }
        assertEquals(Stage.COMPLETED, stateHolder.state.value.joinStage)
    }
}
