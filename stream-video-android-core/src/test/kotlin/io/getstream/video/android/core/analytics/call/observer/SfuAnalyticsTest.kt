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

import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.call.observer.model.Stage
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SfuAnalyticsTest {

    private val reporter = mockk<ClientEventReporter>(relaxed = true)
    private val joinHolder = JoinAnalyticsStateHolder()
    private val sfuHolder = SfuAnalyticsStateHolder()

    @Before
    fun setup() {
        every {
            reporter.reportSfuWsJoinInitiated(any(), any(), any(), any(), any(), any(), any())
        } returns "ws-stage-1"
        joinHolder.update(JoinReason.FirstAttempt, "attempt-1")
        joinHolder.updateCallSessionId("session-1")
        sfuHolder.updateSfuId("sfu-1")
    }

    private fun sfuAnalytics(enabled: Boolean = true) = SfuAnalytics(
        enabled,
        "call-1",
        "default",
        MutableStateFlow<RealtimeConnection>(RealtimeConnection.PreJoin),
        CoroutineScope(Dispatchers.Unconfined),
        reporter,
        joinHolder,
        sfuHolder,
    )

    @Test
    fun `onSfuWsInitiated reports with the shared context and stores the stage id`() {
        sfuAnalytics().onSfuWsInitiated(wasPreviouslyConnected = false)

        assertEquals(Stage.IN_PROGRESS, sfuHolder.wsStage.value)
        assertEquals("ws-stage-1", sfuHolder.wsEventStageId.value)
        verify(exactly = 1) {
            reporter.reportSfuWsJoinInitiated(
                sfuId = "sfu-1",
                callId = "call-1",
                callType = "default",
                joinStageAttemptId = "attempt-1",
                callSessionId = "session-1",
                joinReason = JoinReason.FirstAttempt,
                wasPreviouslyConnected = false,
            )
        }
    }

    @Test
    fun `a second onSfuWsInitiated while in progress is ignored`() {
        val analytics = sfuAnalytics()

        analytics.onSfuWsInitiated(wasPreviouslyConnected = false)
        analytics.onSfuWsInitiated(wasPreviouslyConnected = false)

        verify(exactly = 1) {
            reporter.reportSfuWsJoinInitiated(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `onSfuWsCompleted reports with the stored stage id and resets the stage`() {
        val analytics = sfuAnalytics()
        analytics.onSfuWsInitiated(wasPreviouslyConnected = false)

        analytics.onSfuWsCompleted(success = true, retryCount = 1)

        verify(exactly = 1) {
            reporter.reportSfuWsJoinCompleted(
                stageId = "ws-stage-1",
                joinStageAttemptId = "attempt-1",
                success = true,
                retryCount = 1,
                failureReason = null,
                failureCode = null,
            )
        }
        assertEquals(Stage.NOT_STARTED, sfuHolder.wsStage.value)
    }

    @Test
    fun `onSfuWsCompleted forwards failure details`() {
        val analytics = sfuAnalytics()
        analytics.onSfuWsInitiated(wasPreviouslyConnected = false)

        analytics.onSfuWsCompleted(
            success = false,
            retryCount = 4,
            failureReason = "ws dropped",
            failureCode = "WS_ERROR",
        )

        verify(exactly = 1) {
            reporter.reportSfuWsJoinCompleted(
                stageId = "ws-stage-1",
                joinStageAttemptId = "attempt-1",
                success = false,
                retryCount = 4,
                failureReason = "ws dropped",
                failureCode = "WS_ERROR",
            )
        }
    }

    @Test
    fun `onSfuWsCompleted without an in-progress stage does nothing`() {
        sfuAnalytics().onSfuWsCompleted(success = true, retryCount = 0)

        verify(exactly = 0) {
            reporter.reportSfuWsJoinCompleted(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `disabled analytics never reports`() {
        val analytics = sfuAnalytics(enabled = false)

        analytics.onSfuWsInitiated(wasPreviouslyConnected = false)
        analytics.onSfuWsCompleted(success = true, retryCount = 0)

        verify { reporter wasNot Called }
        assertEquals(Stage.NOT_STARTED, sfuHolder.wsStage.value)
    }
}
