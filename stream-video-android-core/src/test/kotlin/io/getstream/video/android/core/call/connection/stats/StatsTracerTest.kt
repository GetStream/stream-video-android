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

package io.getstream.video.android.core.call.connection.stats

import io.getstream.webrtc.PeerConnection
import io.getstream.webrtc.RTCStats
import io.getstream.webrtc.RTCStatsCollectorCallback
import io.getstream.webrtc.RTCStatsReport
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import stream.video.sfu.models.PeerType
import stream.video.sfu.models.TrackType

class StatsTracerTest {
    private lateinit var peerConnection: PeerConnection
    private lateinit var tracer: StatsTracer

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        peerConnection = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `get returns empty stats when report is empty`() = runBlocking {
        val report = mockk<RTCStatsReport>(relaxed = true) {
            every { statsMap } returns emptyMap()
        }
        every { peerConnection.getStats(any()) } answers {
            val cb = arg<RTCStatsCollectorCallback>(0)
            cb.onStatsDelivered(report)
        }
        tracer = StatsTracer(peerConnection, PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED)
        val result = tracer.get(emptyMap())
        assertTrue(result.performanceStats.isEmpty())
        assertTrue(result.delta.size == 1)
        assertTrue(result.delta["timestamp"] != null)
        assertEquals(report, result.stats)
    }

    @Test
    fun `get handles non-empty stats for publisher`() = runBlocking {
        val outboundStat = mockk<RTCStats>(relaxed = true) {
            every { type } returns "outbound-rtp"
            every { id } returns "track1"
        }
        val report = mockk<RTCStatsReport> {
            every { statsMap } returns mapOf("track1" to outboundStat)
        }
        every { peerConnection.getStats(any()) } answers {
            val cb = arg<RTCStatsCollectorCallback>(0)
            cb.onStatsDelivered(report)
        }

        tracer = StatsTracer(peerConnection, PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED)
        val result = tracer.get(mapOf("track1" to TrackType.TRACK_TYPE_VIDEO))
        assertNotNull(result.performanceStats)
        assertEquals(report, result.stats)
    }

    @Test
    fun `get handles non-empty stats for subscriber`() = runBlocking {
        val inboundStat = mockk<RTCStats>(relaxed = true) {
            every { type } returns "inbound-rtp"
            every { id } returns "track2"
        }
        val report = mockk<RTCStatsReport> {
            every { statsMap } returns mapOf("track2" to inboundStat)
        }
        every { peerConnection.getStats(any()) } answers {
            val cb = arg<RTCStatsCollectorCallback>(0)
            cb.onStatsDelivered(report)
        }
        tracer = StatsTracer(peerConnection, PeerType.PEER_TYPE_SUBSCRIBER)
        val result = tracer.get(mapOf("track2" to TrackType.TRACK_TYPE_VIDEO))
        assertNotNull(result.performanceStats)
        assertEquals(report, result.stats)
    }

    @Test
    fun `get returns ComputedStats with expected structure for publisher unspecified`() = runBlocking {
        val outboundStat = mockk<RTCStats>(relaxed = true) {
            every { type } returns "outbound-rtp"
            every { id } returns "track1"
        }
        val report = mockk<RTCStatsReport> {
            every { statsMap } returns mapOf("track1" to outboundStat)
        }
        every { peerConnection.getStats(any()) } answers {
            val cb = arg<RTCStatsCollectorCallback>(0)
            cb.onStatsDelivered(report)
        }
        tracer = StatsTracer(peerConnection, PeerType.PEER_TYPE_PUBLISHER_UNSPECIFIED)
        val result = tracer.get(mapOf("track1" to TrackType.TRACK_TYPE_VIDEO))
        assertNotNull(result)
        assertNotNull(result.performanceStats)
        assertNotNull(result.delta)
        assertEquals(report, result.stats)
    }
}
