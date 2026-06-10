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

import io.getstream.video.android.core.analytics.reporting.ClientEventReporter
import io.getstream.video.android.core.call.RtcSession
import io.getstream.video.android.core.call.connection.Subscriber
import io.getstream.video.android.core.model.VideoTrack
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import stream.video.sfu.models.TrackType

class VideoAnalyticsTest {

    private val reporter = mockk<ClientEventReporter>(relaxed = true)
    private val joinHolder = JoinAnalyticsStateHolder()
    private val sfuHolder = SfuAnalyticsStateHolder()

    private val webRtcTrack = mockk<org.webrtc.VideoTrack>()
    private val remoteSubscriber = mockk<Subscriber>()
    private val rtcSession = mockk<RtcSession>()

    private lateinit var videoAnalytics: VideoAnalytics

    @Before
    fun setup() {
        every { webRtcTrack.id() } returns "track-1"
        every { remoteSubscriber.getTrack(any(), any()) } returns
            VideoTrack(streamId = "remote-session", video = webRtcTrack)
        every { rtcSession.subscriber } returns MutableStateFlow<Subscriber?>(remoteSubscriber)
        every {
            reporter.reportFirstVideoFrameRendered(any(), any(), any(), any(), any(), any(), any())
        } returns "video-stage-1"

        videoAnalytics = VideoAnalytics("call-1", "default", reporter, joinHolder, sfuHolder)
    }

    private fun render(
        trackType: TrackType = TrackType.TRACK_TYPE_VIDEO,
        videoSessionId: String = "remote-session",
        callSessionId: String = "local-session",
        session: RtcSession? = rtcSession,
    ) = videoAnalytics.firstVideoFrameRendered(
        trackType = trackType,
        width = 1280,
        height = 720,
        rtcSession = session,
        videoSessionId = videoSessionId,
        callSessionId = callSessionId,
    )

    @Test
    fun `the first remote video frame is reported once with the track id`() {
        render()

        assertEquals("video-stage-1", videoAnalytics.stageId.value)
        verify(exactly = 1) {
            reporter.reportFirstVideoFrameRendered(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                "track-1",
            )
        }
    }

    @Test
    fun `subsequent frames are not reported again`() {
        render()
        render()

        verify(exactly = 1) {
            reporter.reportFirstVideoFrameRendered(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `a screen share frame also counts as a first video frame`() {
        render(trackType = TrackType.TRACK_TYPE_SCREEN_SHARE)

        verify(exactly = 1) {
            reporter.reportFirstVideoFrameRendered(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `a local participant frame is ignored`() {
        render(videoSessionId = "local-session", callSessionId = "local-session")

        verify(exactly = 0) {
            reporter.reportFirstVideoFrameRendered(any(), any(), any(), any(), any(), any(), any())
        }
        assertEquals("", videoAnalytics.stageId.value)
    }

    @Test
    fun `an audio track frame is ignored`() {
        render(trackType = TrackType.TRACK_TYPE_AUDIO)

        verify(exactly = 0) {
            reporter.reportFirstVideoFrameRendered(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `a missing rtc session is ignored`() {
        render(session = null)

        verify(exactly = 0) {
            reporter.reportFirstVideoFrameRendered(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `reset allows the first frame to be reported again`() {
        render()
        videoAnalytics.reset()
        render()

        verify(exactly = 2) {
            reporter.reportFirstVideoFrameRendered(any(), any(), any(), any(), any(), any(), any())
        }
    }
}
