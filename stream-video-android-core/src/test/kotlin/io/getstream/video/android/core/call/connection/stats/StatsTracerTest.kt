package io.getstream.video.android.core.call.connection.stats

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
import org.webrtc.PeerConnection
import org.webrtc.RTCStats
import org.webrtc.RTCStatsCollectorCallback
import org.webrtc.RTCStatsReport
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
        assertTrue(result.delta.isEmpty())
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
