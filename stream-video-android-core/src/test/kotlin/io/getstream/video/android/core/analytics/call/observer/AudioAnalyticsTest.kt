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
import io.getstream.video.android.core.analytics.call.observer.model.JoinReason
import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import io.getstream.video.android.core.model.AudioTrack
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.webrtc.AudioTrackSink
import java.nio.ByteBuffer

@OptIn(ExperimentalCoroutinesApi::class)
class AudioAnalyticsTest {

    private val reporter = mockk<ClientEventReporter>(relaxed = true)
    private val joinHolder = JoinAnalyticsStateHolder()
    private val sfuHolder = SfuAnalyticsStateHolder()

    private val sinkSlot = slot<AudioTrackSink>()
    private val webRtcAudioTrack = mockk<org.webrtc.AudioTrack>(relaxed = true)

    @Before
    fun setup() {
        every { webRtcAudioTrack.addSink(capture(sinkSlot)) } just Runs
        joinHolder.update(JoinReason.FirstAttempt, "attempt-1")
        joinHolder.updateCallSessionId("session-1")
        sfuHolder.updateSfuId("sfu-1")
    }

    private fun audioAnalytics(isEnabled: Boolean = true) = AudioAnalytics(
        "call-1",
        "default",
        reporter,
        joinHolder,
        sfuHolder,
        isEnabled,
    )

    private fun remoteParticipant(
        streamId: String = "remote-stream",
        track: org.webrtc.AudioTrack = webRtcAudioTrack,
    ): ParticipantState = mockk {
        every { isLocal } returns false
        every { audioTrack } returns MutableStateFlow(AudioTrack(streamId, track))
    }

    private fun localParticipant(): ParticipantState = mockk {
        every { isLocal } returns true
    }

    private fun TestScope.observe(
        analytics: AudioAnalytics,
        participants: StateFlow<List<ParticipantState>>,
    ): CoroutineScope {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        analytics.observeParticipantsForFirstRemoteAudioFrame(participants, scope)
        runCurrent()
        return scope
    }

    private fun deliverFrame(frames: Int = 480, bytes: Int = 960) {
        sinkSlot.captured.onData(ByteBuffer.allocate(bytes), 16, 48_000, 1, frames, 0L)
    }

    // --- Disabled ---

    @Test
    fun `observing participants is a no-op while the feature is disabled`() = runTest {
        val analytics = audioAnalytics(isEnabled = false)
        val participants = MutableStateFlow(listOf(remoteParticipant()))

        val scope = observe(analytics, participants)

        assertFalse(sinkSlot.isCaptured)
        assertFalse(analytics.recordedFirstFrame.get())
        verify { reporter wasNot Called }
        scope.cancel()
    }

    @Test
    fun `reset leaves state untouched while the feature is disabled`() {
        val analytics = audioAnalytics(isEnabled = false)
        analytics.recordedFirstFrame.set(true)

        analytics.reset()

        assertTrue(analytics.recordedFirstFrame.get())
        verify { reporter wasNot Called }
    }

    // --- Sink attachment ---

    @Test
    fun `a sink is attached to each remote participant audio track`() = runTest {
        val participants = MutableStateFlow(listOf(remoteParticipant()))

        val scope = observe(audioAnalytics(), participants)

        verify(exactly = 1) { webRtcAudioTrack.addSink(any()) }
        scope.cancel()
    }

    @Test
    fun `local participants never get a sink`() = runTest {
        val participants = MutableStateFlow(listOf(localParticipant()))

        val scope = observe(audioAnalytics(), participants)

        verify(exactly = 0) { webRtcAudioTrack.addSink(any()) }
        scope.cancel()
    }

    @Test
    fun `a track with an already-sinked stream id is not sinked twice`() = runTest {
        val audioTrackFlow = MutableStateFlow<AudioTrack?>(
            AudioTrack("remote-stream", webRtcAudioTrack),
        )
        val participant = mockk<ParticipantState> {
            every { isLocal } returns false
            every { audioTrack } returns audioTrackFlow
        }
        val otherWebRtcTrack = mockk<org.webrtc.AudioTrack>(relaxed = true)
        val scope = observe(audioAnalytics(), MutableStateFlow(listOf(participant)))

        audioTrackFlow.value = AudioTrack("remote-stream", otherWebRtcTrack)
        runCurrent()

        verify(exactly = 1) { webRtcAudioTrack.addSink(any()) }
        verify(exactly = 0) { otherWebRtcTrack.addSink(any()) }
        scope.cancel()
    }

    // --- First frame detection ---

    @Test
    fun `the first decoded frame reports once with the shared context and cleans up`() = runTest {
        val analytics = audioAnalytics()
        val scope = observe(analytics, MutableStateFlow(listOf(remoteParticipant())))

        deliverFrame()
        runCurrent()

        assertTrue(analytics.recordedFirstFrame.get())
        verify(exactly = 1) {
            reporter.reportFirstAudioFrameRendered(
                sfuId = "sfu-1",
                callId = "call-1",
                callType = "default",
                joinStageAttemptId = "attempt-1",
                callSessionId = "session-1",
                joinReason = JoinReason.FirstAttempt,
            )
        }
        verify(exactly = 1) { webRtcAudioTrack.removeSink(sinkSlot.captured) }
        scope.cancel()
    }

    @Test
    fun `frames without an audio payload are ignored`() = runTest {
        val analytics = audioAnalytics()
        val scope = observe(analytics, MutableStateFlow(listOf(remoteParticipant())))

        deliverFrame(frames = 0)
        deliverFrame(bytes = 0)
        runCurrent()

        assertFalse(analytics.recordedFirstFrame.get())
        verify(exactly = 0) {
            reporter.reportFirstAudioFrameRendered(any(), any(), any(), any(), any(), any())
        }

        deliverFrame()
        runCurrent()

        assertTrue(analytics.recordedFirstFrame.get())
        verify(exactly = 1) {
            reporter.reportFirstAudioFrameRendered(any(), any(), any(), any(), any(), any())
        }
        scope.cancel()
    }

    @Test
    fun `only the first frame is reported even when more frames arrive`() = runTest {
        val analytics = audioAnalytics()
        val scope = observe(analytics, MutableStateFlow(listOf(remoteParticipant())))

        deliverFrame()
        deliverFrame()
        deliverFrame()
        runCurrent()

        verify(exactly = 1) {
            reporter.reportFirstAudioFrameRendered(any(), any(), any(), any(), any(), any())
        }
        scope.cancel()
    }

    // --- Reset ---

    @Test
    fun `reset removes attached sinks and clears the first-frame flag`() = runTest {
        val analytics = audioAnalytics()
        val scope = observe(analytics, MutableStateFlow(listOf(remoteParticipant())))
        analytics.recordedFirstFrame.set(true)

        analytics.reset()

        assertFalse(analytics.recordedFirstFrame.get())
        verify(exactly = 1) { webRtcAudioTrack.removeSink(sinkSlot.captured) }
        scope.cancel()
    }
}
