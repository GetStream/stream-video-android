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

import android.os.PowerManager
import androidx.lifecycle.Lifecycle
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CallState
import io.getstream.video.android.core.MediaManagerImpl
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoClient
import io.getstream.video.android.core.analytics.call.observer.SfuAnalytics
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.components.CallSessionManager
import io.getstream.video.android.core.call.connection.Publisher
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.webrtc.AudioTrack
import stream.video.sfu.models.AudioBitrateProfile

/**
 * Covers the audio side of [RtcSession] — the bridges [io.getstream.video.android.core.Call] calls
 * when the audio bitrate profile changes on a running call, and the capture-pipeline rebuild
 * behind them.
 *
 * Every bridge has a no-publisher branch that has to answer without throwing: the profile can be
 * set on a session that exists but is not publishing audio yet, and a crash there would take the
 * whole switch down rather than reporting one stage as unreachable.
 *
 * Runs under Robolectric even though nothing here needs Android: the other tests that touch
 * [RtcSession] are Robolectric tests, and mixing loaders makes JaCoCo discard this class's
 * execution data as a bytecode mismatch — the coverage silently disappears in a full-suite run
 * while every test still passes.
 */
@RunWith(RobolectricTestRunner::class)
class RtcSessionAudioProfileTest {

    private val testScope = TestScope(StandardTestDispatcher())

    @MockK
    private lateinit var mockPowerManager: PowerManager

    @RelaxedMockK
    private lateinit var mockCall: Call

    @RelaxedMockK
    private lateinit var mockMediaManager: MediaManagerImpl

    @RelaxedMockK
    private lateinit var mockLifecycle: Lifecycle

    @RelaxedMockK
    private lateinit var mockVideoClient: StreamVideoClient

    @RelaxedMockK
    private lateinit var mockCallState: CallState

    private lateinit var session: RtcSession

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        // RtcSession refuses to construct without an installed SDK singleton.
        val streamVideo = mockk<StreamVideo>(relaxed = true)
        StreamVideo.install(streamVideo)

        every { mockCall.state } returns mockCallState
        every { mockCall.scope } returns testScope
        every { mockCall.mediaManager } returns mockMediaManager
        every { mockCall.peerConnectionFactory } returns mockk(relaxed = true) {
            every { makePeerConnection(any(), any(), any(), any()) } returns mockk(relaxed = true)
        }
        every { mockCallState.ownCapabilities } returns MutableStateFlow(emptyList())
        every { mockCallState.participants } returns MutableStateFlow(emptyList())
        every { mockCallState.remoteParticipants } returns MutableStateFlow(emptyList())
        every { mockCallState.me.value } returns null

        session = RtcSession(
            client = streamVideo,
            powerManager = mockPowerManager,
            call = mockCall,
            sessionManager = CallSessionManager(),
            sessionId = "session-id",
            apiKey = "api-key",
            lifecycle = mockLifecycle,
            sfuUrl = "https://test-sfu.stream.com",
            sfuWsUrl = "wss://test-sfu.stream.com",
            sfuToken = "sfu-token",
            sfuName = "test-sfu-edge",
            remoteIceServers = emptyList(),
            clientImpl = mockVideoClient,
            coroutineScope = testScope,
            sfuConnectionModuleProvider = { mockk(relaxed = true) },
            sfuAnalytics = SfuAnalytics.getFakeSfuAnalytics(),
        )
        // A session starts without one; each test opts in.
        session.publisher.value = null
    }

    @After
    fun tearDown() {
        StreamVideo.removeClient()
        unmockkAll()
    }

    private fun publishing(): Publisher = mockk<Publisher>(relaxed = true).also {
        session.publisher.value = it
    }

    /** Runs the media manager's swap callback with [newTrack], the way a real rebuild would. */
    private fun mediaManagerHandsOver(newTrack: AudioTrack) {
        every { mockMediaManager.replaceAudioSourceAndTrack(any()) } answers {
            firstArg<(AudioTrack) -> Boolean>().invoke(newTrack)
        }
    }

    //region rebuildAudioCapturePipeline

    @Test
    fun `rebuilding moves the publisher onto the new track`() {
        val newTrack = mockk<AudioTrack>(relaxed = true)
        val publisher = publishing()
        every { publisher.replaceAudioTrack(newTrack) } returns true
        mediaManagerHandsOver(newTrack)

        assertTrue(session.rebuildAudioCapturePipeline())

        verify { publisher.replaceAudioTrack(newTrack) }
    }

    @Test
    fun `rebuilding accepts the new pair when nothing is published yet`() {
        mediaManagerHandsOver(mockk(relaxed = true))

        // There is no sender to move, so the fresh pair simply becomes current — reporting a
        // failure here would fail a stage that had nothing to do.
        assertTrue(session.rebuildAudioCapturePipeline())
    }

    @Test
    fun `rebuilding reports the publisher's refusal`() {
        val newTrack = mockk<AudioTrack>(relaxed = true)
        val publisher = publishing()
        every { publisher.replaceAudioTrack(newTrack) } returns false
        mediaManagerHandsOver(newTrack)

        assertFalse(session.rebuildAudioCapturePipeline())
    }

    //endregion

    //region bitrate bridges

    @Test
    fun `setAudioMaxBitrate goes to the publisher`() {
        val publisher = publishing()
        every { publisher.setAudioMaxBitrate(128_000) } returns true

        assertTrue(session.setAudioMaxBitrate(128_000))
    }

    @Test
    fun `setAudioMaxBitrate reports false with no publisher`() {
        assertFalse(session.setAudioMaxBitrate(128_000))
    }

    @Test
    fun `audioMaxBitrate reads through to the publisher`() {
        val publisher = publishing()
        every { publisher.audioMaxBitrate() } returns 128_000

        assertEquals(128_000, session.audioMaxBitrate())
    }

    @Test
    fun `audioMaxBitrate is null with no publisher`() {
        assertNull(session.audioMaxBitrate())
    }

    @Test
    fun `negotiatedAudioBitrate reads through to the publisher`() {
        val publisher = publishing()
        every { publisher.negotiatedAudioBitrate() } returns 64_000

        assertEquals(64_000, session.negotiatedAudioBitrate())
    }

    @Test
    fun `negotiatedAudioBitrate is null with no publisher`() {
        assertNull(session.negotiatedAudioBitrate())
    }

    @Test
    fun `audioBitrateFor reads through to the publisher`() {
        val publisher = publishing()
        every {
            publisher.audioBitrateFor(AudioBitrateProfile.AUDIO_BITRATE_PROFILE_MUSIC_HIGH_QUALITY)
        } returns 128_000

        assertEquals(
            128_000,
            session.audioBitrateFor(
                AudioBitrateProfile.AUDIO_BITRATE_PROFILE_MUSIC_HIGH_QUALITY,
            ),
        )
    }

    @Test
    fun `audioBitrateFor is null with no publisher`() {
        assertNull(
            session.audioBitrateFor(
                AudioBitrateProfile.AUDIO_BITRATE_PROFILE_MUSIC_HIGH_QUALITY,
            ),
        )
    }

    //endregion
}
