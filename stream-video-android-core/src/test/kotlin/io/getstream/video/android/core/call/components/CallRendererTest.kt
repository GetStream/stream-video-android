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

import io.getstream.video.android.core.Call
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.connection.Subscriber
import io.getstream.video.android.core.model.AudioTrack
import io.getstream.video.android.core.model.MediaTrack
import io.getstream.video.android.core.model.PreferredVideoResolution
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import stream.video.sfu.models.TrackType
import java.util.concurrent.ConcurrentHashMap

/**
 * Unit tests for [CallRenderer], which binds tracks to renderers and forwards
 * visibility / track-dimension and incoming media-quality updates to the session.
 */
class CallRendererTest {

    private val sessionFlow = MutableStateFlow<RtcSession?>(null)
    private val call = mockk<Call>(relaxed = true).also {
        every { it.type } returns "default"
        every { it.id } returns "call-id"
        every { it.session } returns sessionFlow
    }

    private fun renderer() = CallRenderer(call)

    @Test
    fun `setVisibility updates track dimensions with default dimension`() {
        val session = mockk<RtcSession>(relaxed = true)
        sessionFlow.value = session

        renderer().setVisibility("s1", TrackType.TRACK_TYPE_VIDEO, visible = true)

        verify {
            session.updateTrackDimensions(
                "s1",
                TrackType.TRACK_TYPE_VIDEO,
                true,
                any(),
                "s1",
            )
        }
    }

    @Test
    fun `setVisibility with explicit size forwards the requested dimension`() {
        val session = mockk<RtcSession>(relaxed = true)
        sessionFlow.value = session

        renderer().setVisibility(
            sessionId = "s1",
            trackType = TrackType.TRACK_TYPE_VIDEO,
            visible = true,
            width = 640,
            height = 480,
        )

        verify {
            session.updateTrackDimensions("s1", TrackType.TRACK_TYPE_VIDEO, true, any(), "s1")
        }
    }

    @Test
    fun `setVisibility is a no-op when there is no session`() {
        sessionFlow.value = null
        // Should not throw.
        renderer().setVisibility("s1", TrackType.TRACK_TYPE_VIDEO, visible = false)
    }

    @Test
    fun `setPreferredIncomingVideoResolution forwards overrides`() {
        val session = mockk<RtcSession>(relaxed = true)
        sessionFlow.value = session

        renderer().setPreferredIncomingVideoResolution(
            PreferredVideoResolution(width = 1280, height = 720),
            sessionIds = listOf("s1"),
        )

        verify {
            session.trackOverridesHandler.updateOverrides(
                sessionIds = listOf("s1"),
                dimensions = any(),
            )
        }
    }

    @Test
    fun `setPreferredIncomingVideoResolution clears overrides when resolution is null`() {
        val session = mockk<RtcSession>(relaxed = true)
        sessionFlow.value = session

        renderer().setPreferredIncomingVideoResolution(null)

        verify {
            session.trackOverridesHandler.updateOverrides(
                sessionIds = null,
                dimensions = null,
            )
        }
    }

    @Test
    fun `setIncomingVideoEnabled forwards visibility overrides`() {
        val session = mockk<RtcSession>(relaxed = true)
        sessionFlow.value = session

        renderer().setIncomingVideoEnabled(enabled = false, sessionIds = listOf("s1"))

        verify { session.trackOverridesHandler.updateOverrides(listOf("s1"), visible = false) }
    }

    @Test
    fun `setIncomingAudioEnabled returns early when there is no subscriber`() {
        val session = mockk<RtcSession>(relaxed = true)
        every { session.subscriber } returns MutableStateFlow(null)
        sessionFlow.value = session

        // No tracks available -> should return without throwing.
        renderer().setIncomingAudioEnabled(enabled = true)
    }

    @Test
    fun `setIncomingAudioEnabled toggles audio for all participants`() {
        val audioTrack = mockk<AudioTrack>(relaxed = true)
        sessionFlow.value = sessionWithAudioTrack(audioTrack)

        renderer().setIncomingAudioEnabled(enabled = false)

        verify { audioTrack.enableAudio(false) }
    }

    @Test
    fun `setIncomingAudioEnabled toggles audio for the requested sessions`() {
        val audioTrack = mockk<AudioTrack>(relaxed = true)
        sessionFlow.value = sessionWithAudioTrack(audioTrack)

        renderer().setIncomingAudioEnabled(enabled = true, sessionIds = listOf("s1"))

        verify { audioTrack.enableAudio(true) }
    }

    private fun sessionWithAudioTrack(audioTrack: AudioTrack): RtcSession {
        val innerTracks = ConcurrentHashMap<TrackType, MediaTrack>().apply {
            put(TrackType.TRACK_TYPE_AUDIO, audioTrack)
        }
        val tracks = ConcurrentHashMap<String, ConcurrentHashMap<TrackType, MediaTrack>>().apply {
            put("s1", innerTracks)
        }
        val subscriber = mockk<Subscriber>(relaxed = true)
        every { subscriber.tracks } returns tracks
        return mockk<RtcSession>(relaxed = true).also {
            every { it.subscriber } returns MutableStateFlow(subscriber)
        }
    }
}
