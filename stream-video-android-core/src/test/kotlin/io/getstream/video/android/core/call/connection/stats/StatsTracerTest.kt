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

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.webrtc.PeerConnection
import org.webrtc.RTCStats
import org.webrtc.RTCStatsCollectorCallback
import org.webrtc.RTCStatsReport
import stream.video.sfu.models.PeerType
import stream.video.sfu.models.TrackType
import java.math.BigInteger

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

    @Test
    fun `get polls inbound audio rtp stats for subscriber`() = runBlocking {
        val inboundAudioStat = inboundAudioStat(
            id = "inbound-audio-1",
            trackIdentifier = "audio-track-1",
            packetsReceived = 5,
            bytesReceived = BigInteger.valueOf(1000),
            totalAudioEnergy = 0.4,
            totalSamplesReceived = BigInteger.valueOf(480),
            silentConcealedSamples = BigInteger.ZERO,
        )
        val report = mockk<RTCStatsReport> {
            every { statsMap } returns mapOf("inbound-audio-1" to inboundAudioStat)
        }
        every { peerConnection.getStats(any()) } answers {
            val cb = arg<RTCStatsCollectorCallback>(0)
            cb.onStatsDelivered(report)
        }

        tracer = StatsTracer(peerConnection, PeerType.PEER_TYPE_SUBSCRIBER)
        val result = tracer.get(
            trackIdToTrackType = mapOf("audio-track-1" to TrackType.TRACK_TYPE_AUDIO),
            trackIdToParticipant = mapOf("audio-track-1" to "session-1"),
        )

        assertEquals(1, result.inboundAudioStats.size)
        val audioStats = result.inboundAudioStats.first()
        assertEquals("audio-track-1", audioStats.trackIdentifier)
        assertEquals("session-1", audioStats.sessionId)
        assertEquals(TrackType.TRACK_TYPE_AUDIO, audioStats.trackType)
        assertEquals(5, audioStats.deltaPacketsReceived)
        assertEquals(BigInteger.valueOf(1000), audioStats.deltaBytesReceived)
        assertEquals(BigInteger.valueOf(480), audioStats.deltaTotalSamplesReceived)
        assertTrue(audioStats.isReceivingRealAudio)
    }

    @Test
    fun `get does not mark silent concealed inbound audio as real audio`() = runBlocking {
        val inboundAudioStat = inboundAudioStat(
            id = "inbound-audio-1",
            trackIdentifier = "audio-track-1",
            packetsReceived = 5,
            bytesReceived = BigInteger.valueOf(1000),
            totalAudioEnergy = 0.0,
            totalSamplesReceived = BigInteger.valueOf(480),
            silentConcealedSamples = BigInteger.valueOf(480),
        )
        val report = mockk<RTCStatsReport> {
            every { statsMap } returns mapOf("inbound-audio-1" to inboundAudioStat)
        }
        every { peerConnection.getStats(any()) } answers {
            val cb = arg<RTCStatsCollectorCallback>(0)
            cb.onStatsDelivered(report)
        }

        tracer = StatsTracer(peerConnection, PeerType.PEER_TYPE_SUBSCRIBER)
        val result = tracer.get(
            trackIdToTrackType = mapOf("audio-track-1" to TrackType.TRACK_TYPE_AUDIO),
            trackIdToParticipant = mapOf("audio-track-1" to "session-1"),
        )

        assertFalse(result.inboundAudioStats.first().isReceivingRealAudio)
    }

    private fun inboundAudioStat(
        id: String,
        trackIdentifier: String,
        packetsReceived: Long,
        bytesReceived: BigInteger,
        totalAudioEnergy: Double,
        totalSamplesReceived: BigInteger,
        silentConcealedSamples: BigInteger,
    ): RTCStats {
        val members = mapOf(
            "kind" to "audio",
            "trackIdentifier" to trackIdentifier,
            "packetsReceived" to packetsReceived,
            "bytesReceived" to bytesReceived,
            "audioLevel" to 0.2,
            "totalAudioEnergy" to totalAudioEnergy,
            "totalSamplesReceived" to totalSamplesReceived,
            "concealedSamples" to BigInteger.ZERO,
            "silentConcealedSamples" to silentConcealedSamples,
        )
        return mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { type } returns "inbound-rtp"
            every { timestampUs } returns 1_000.0
            every { this@mockk.members } returns members
        }
    }
}
