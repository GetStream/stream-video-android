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

import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioAnalyticsTest {

    private val reporter = mockk<ClientEventReporter>(relaxed = true)

    private fun audioAnalytics() = AudioAnalytics(
        "call-1",
        "default",
        reporter,
        JoinAnalyticsStateHolder(),
        SfuAnalyticsStateHolder(),
    )

    @Test
    fun `observing participants is a no-op while the feature is disabled`() = runTest {
        val analytics = audioAnalytics()
        val participants = MutableStateFlow<List<ParticipantState>>(emptyList())

        analytics.observeParticipantsForFirstRemoteAudioFrame(participants, backgroundScope)
        runCurrent()

        assertFalse(analytics.recordedFirstFrame.get())
        verify { reporter wasNot Called }
    }

    @Test
    fun `reset leaves state untouched while the feature is disabled`() {
        val analytics = audioAnalytics()
        analytics.recordedFirstFrame.set(true)

        analytics.reset()

        assertTrue(analytics.recordedFirstFrame.get())
        verify { reporter wasNot Called }
    }
}
