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

package io.getstream.video.android.core

import io.getstream.video.android.core.call.stats.model.RtcStatsReport
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.webrtc.RTCStats
import org.webrtc.RTCStatsReport

/**
 * Covers the audio side of [CallStats] — the send/receive rate derived from the RTP byte
 * counters, the target bitrate, and the codec.
 *
 * The rate is the part worth guarding: it is a delta between two polls, so every way the counter
 * can misbehave (first sample, a stream replaced underneath it, a clock that did not move) has to
 * leave the reported value alone rather than publish a nonsense number. The codec is guarded
 * because one report describes one direction, and the publisher and the subscriber are both fed
 * through the same method.
 */
class CallStatsTest {

    private val testScope = TestScope(StandardTestDispatcher())

    private fun callStats(): CallStats {
        val call = mockk<Call>(relaxed = true)
        // updateFromRTCStats reaches through the session for the video track mapping; no session
        // means the audio path runs on its own.
        every { call.session } returns MutableStateFlow(null)
        return CallStats(call, testScope)
    }

    private fun stat(
        type: String,
        id: String = type,
        timestampUs: Long = 0,
        members: Map<String, Any>,
    ) = RTCStats(timestampUs, type, id, members)

    private fun report(vararg stats: RTCStats): RtcStatsReport {
        val origin = RTCStatsReport(0, stats.associateBy { it.id })
        return RtcStatsReport(origin, emptyMap())
    }

    private fun outboundAudio(
        bytesSent: Long,
        timestampUs: Long,
        targetBitrate: Double? = null,
        codecId: String? = null,
    ) = stat(
        type = "outbound-rtp",
        id = "outbound-audio",
        timestampUs = timestampUs,
        members = buildMap {
            put("kind", "audio")
            put("bytesSent", bytesSent)
            targetBitrate?.let { put("targetBitrate", it) }
            codecId?.let { put("codecId", it) }
        },
    )

    private fun inboundAudio(
        bytesReceived: Long,
        timestampUs: Long,
        codecId: String? = null,
    ) = stat(
        type = "inbound-rtp",
        id = "inbound-audio",
        timestampUs = timestampUs,
        members = buildMap {
            put("kind", "audio")
            put("bytesReceived", bytesReceived)
            codecId?.let { put("codecId", it) }
        },
    )

    private fun codec(
        id: String,
        mimeType: String = "audio/opus",
        clockRate: Long = 48000,
        channels: Long = 1,
        fmtp: String = "minptime=10;useinbandfec=1",
    ) = stat(
        type = "codec",
        id = id,
        members = mapOf(
            "mimeType" to mimeType,
            "clockRate" to clockRate,
            "channels" to channels,
            "sdpFmtpLine" to fmtp,
        ),
    )

    //region audio bitrate derivation

    @Test
    fun `a missing byte counter leaves the baseline untouched`() {
        val stats = callStats()

        stats.publisher.updateAudioBitrate(bytes = null, timestampUs = 1_000_000.0)

        assertEquals(null, stats.publisher.lastAudioBytes)
        assertEquals(0F, stats.publisher.audioBitrateKbps.value)
    }

    @Test
    fun `the first sample only seeds the baseline`() {
        val stats = callStats()

        stats.publisher.updateAudioBitrate(bytes = 8_000, timestampUs = 1_000_000.0)

        // A rate needs two points; reporting one would divide by a baseline that does not exist.
        assertEquals(8_000L, stats.publisher.lastAudioBytes)
        assertEquals(0F, stats.publisher.audioBitrateKbps.value)
    }

    @Test
    fun `the second sample derives the rate from the delta`() {
        val stats = callStats()

        stats.publisher.updateAudioBitrate(bytes = 0, timestampUs = 0.0)
        stats.publisher.updateAudioBitrate(bytes = 8_000, timestampUs = 1_000_000.0)

        // 8000 bytes over one second is 64 kbps, the default voice bitrate.
        assertEquals(64F, stats.publisher.audioBitrateKbps.value)
    }

    @Test
    fun `a counter that went backwards reseeds instead of reporting a negative rate`() {
        val stats = callStats()

        stats.publisher.updateAudioBitrate(bytes = 0, timestampUs = 0.0)
        stats.publisher.updateAudioBitrate(bytes = 16_000, timestampUs = 1_000_000.0)
        // The track was replaced, so the counter restarted.
        stats.publisher.updateAudioBitrate(bytes = 100, timestampUs = 2_000_000.0)

        // The last good reading stands rather than a negative one, and the new counter becomes
        // the baseline for the next poll.
        assertEquals(128F, stats.publisher.audioBitrateKbps.value)
        assertEquals(100L, stats.publisher.lastAudioBytes)
    }

    @Test
    fun `a clock that did not move is ignored`() {
        val stats = callStats()

        stats.publisher.updateAudioBitrate(bytes = 0, timestampUs = 1_000_000.0)
        stats.publisher.updateAudioBitrate(bytes = 8_000, timestampUs = 1_000_000.0)

        assertEquals(0F, stats.publisher.audioBitrateKbps.value)
    }

    //endregion

    //region report plumbing

    @Test
    fun `a publisher report fills the audio send rate and the target bitrate`() {
        val stats = callStats()

        stats.updateFromRTCStats(
            report(outboundAudio(bytesSent = 0, timestampUs = 0, targetBitrate = 128_000.0)),
            isPublisher = true,
        )
        stats.updateFromRTCStats(
            report(
                outboundAudio(
                    bytesSent = 16_000,
                    timestampUs = 1_000_000,
                    targetBitrate = 128_000.0,
                ),
            ),
            isPublisher = true,
        )

        assertEquals(128F, stats.publisher.audioBitrateKbps.value)
        // What the encoder was asked for, against the 128 kbps it actually sent.
        assertEquals(128F, stats.publisher.audioTargetBitrateKbps.value)
    }

    @Test
    fun `a subscriber report fills the audio receive rate`() {
        val stats = callStats()

        stats.updateFromRTCStats(
            report(inboundAudio(bytesReceived = 0, timestampUs = 0)),
            isPublisher = false,
        )
        stats.updateFromRTCStats(
            report(inboundAudio(bytesReceived = 8_000, timestampUs = 1_000_000)),
            isPublisher = false,
        )

        assertEquals(64F, stats.subscriber.audioBitrateKbps.value)
        // The receive side has no target of its own to report.
        assertEquals(0F, stats.subscriber.audioTargetBitrateKbps.value)
    }

    @Test
    fun `a null report is ignored`() {
        val stats = callStats()

        stats.updateFromRTCStats(null)

        assertEquals(0F, stats.publisher.audioBitrateKbps.value)
    }

    @Test
    fun `a seeded byte count with no timestamp behind it does not derive a rate`() {
        val stats = callStats()
        // The two halves of the baseline are separate fields, so a half-written one has to be
        // treated as no baseline rather than divided by.
        stats.publisher.lastAudioBytes = 8_000

        stats.publisher.updateAudioBitrate(bytes = 16_000, timestampUs = 1_000_000.0)

        assertEquals(0F, stats.publisher.audioBitrateKbps.value)
    }

    @Test
    fun `a report carrying no audio at all leaves every audio value alone`() {
        val stats = callStats()

        stats.updateFromRTCStats(
            report(stat(type = "candidate-pair", members = mapOf("currentRoundTripTime" to 0.05))),
            isPublisher = true,
        )

        assertEquals(0F, stats.publisher.audioBitrateKbps.value)
        assertEquals(0F, stats.publisher.audioTargetBitrateKbps.value)
        assertEquals("", stats.publisher.audioCodec.value)
    }

    @Test
    fun `byte counters of the wrong type are treated as absent`() {
        val stats = callStats()

        // WebRTC reports these as BigInteger on some builds; the cast has to fail into "no
        // sample" rather than seed a baseline that later polls subtract from.
        stats.updateFromRTCStats(
            report(
                stat(
                    type = "outbound-rtp",
                    id = "outbound-audio",
                    members = mapOf("kind" to "audio", "bytesSent" to "16000"),
                ),
            ),
            isPublisher = true,
        )
        stats.updateFromRTCStats(
            report(
                stat(
                    type = "inbound-rtp",
                    id = "inbound-audio",
                    members = mapOf("kind" to "audio", "bytesReceived" to 1.5),
                ),
            ),
            isPublisher = false,
        )

        assertNull(stats.publisher.lastAudioBytes)
        assertNull(stats.subscriber.lastAudioBytes)
    }

    @Test
    fun `a target bitrate of the wrong type is skipped`() {
        val stats = callStats()

        stats.updateFromRTCStats(
            report(
                stat(
                    type = "outbound-rtp",
                    id = "outbound-audio",
                    members = mapOf(
                        "kind" to "audio",
                        "bytesSent" to 0L,
                        "targetBitrate" to 128_000,
                    ),
                ),
            ),
            isPublisher = true,
        )

        assertEquals(0F, stats.publisher.audioTargetBitrateKbps.value)
    }

    //endregion

    //region codec direction

    @Test
    fun `a publisher report sets the publisher codec only`() {
        val stats = callStats()

        stats.updateFromRTCStats(
            report(
                outboundAudio(bytesSent = 0, timestampUs = 0, codecId = "codec-send"),
                codec("codec-send", channels = 2),
            ),
            isPublisher = true,
        )

        assertEquals(
            "audio/opus 48000 Hz stereo minptime=10;useinbandfec=1",
            stats.publisher.audioCodec.value,
        )
        // Writing both sides from one report is how the subscriber ended up showing the
        // publisher's codec, and the other way round on the next poll.
        assertEquals("", stats.subscriber.audioCodec.value)
    }

    @Test
    fun `a subscriber report sets the subscriber codec only`() {
        val stats = callStats()

        stats.updateFromRTCStats(
            report(
                inboundAudio(bytesReceived = 0, timestampUs = 0, codecId = "codec-recv"),
                codec("codec-recv"),
            ),
            isPublisher = false,
        )

        assertEquals(
            "audio/opus 48000 Hz mono minptime=10;useinbandfec=1",
            stats.subscriber.audioCodec.value,
        )
        assertEquals("", stats.publisher.audioCodec.value)
    }

    @Test
    fun `the codec is resolved through the RTP statistic's own codecId`() {
        val stats = callStats()

        stats.updateFromRTCStats(
            report(
                outboundAudio(bytesSent = 0, timestampUs = 0, codecId = "codec-second"),
                // A report can carry more than one audio codec; the first one is not necessarily
                // the one this direction is using.
                codec("codec-first", mimeType = "audio/PCMU", clockRate = 8000),
                codec("codec-second", channels = 2),
            ),
            isPublisher = true,
        )

        assertEquals(
            "audio/opus 48000 Hz stereo minptime=10;useinbandfec=1",
            stats.publisher.audioCodec.value,
        )
    }

    @Test
    fun `a codecId of the wrong type falls back to the codec the report carries`() {
        val stats = callStats()

        stats.updateFromRTCStats(
            report(
                stat(
                    type = "outbound-rtp",
                    id = "outbound-audio",
                    members = mapOf("kind" to "audio", "bytesSent" to 0L, "codecId" to 7L),
                ),
                codec("codec-only"),
            ),
            isPublisher = true,
        )

        assertEquals(
            "audio/opus 48000 Hz mono minptime=10;useinbandfec=1",
            stats.publisher.audioCodec.value,
        )
    }

    @Test
    fun `a codecId pointing at nothing reports no codec`() {
        val stats = callStats()

        // The elvis falls through to codec:audio, and there is none — the alternative is an
        // exception on a stats poll.
        stats.updateFromRTCStats(
            report(outboundAudio(bytesSent = 0, timestampUs = 0, codecId = "missing")),
            isPublisher = true,
        )

        assertEquals("", stats.publisher.audioCodec.value)
    }

    @Test
    fun `codec members of the wrong type drop out of the description`() {
        val stats = callStats()

        stats.updateFromRTCStats(
            report(
                outboundAudio(bytesSent = 0, timestampUs = 0, codecId = "odd-codec"),
                stat(
                    type = "codec",
                    id = "odd-codec",
                    members = mapOf(
                        "mimeType" to 1,
                        "clockRate" to "48000",
                        "channels" to "1",
                        "sdpFmtpLine" to 2,
                    ),
                ),
            ),
            isPublisher = true,
        )

        // Every member failed its cast, so the description is empty rather than "null null null".
        assertEquals("", stats.publisher.audioCodec.value)
    }

    @Test
    fun `a codec with no channel count reports without a channel word`() {
        val stats = callStats()

        stats.updateFromRTCStats(
            report(
                outboundAudio(bytesSent = 0, timestampUs = 0, codecId = "no-channels"),
                stat(
                    type = "codec",
                    id = "no-channels",
                    members = mapOf("mimeType" to "audio/opus", "clockRate" to 48000L),
                ),
            ),
            isPublisher = true,
        )

        assertEquals("audio/opus 48000 Hz", stats.publisher.audioCodec.value)
    }

    @Test
    fun `a report with no codecId still reports the codec it carries`() {
        val stats = callStats()

        stats.updateFromRTCStats(
            report(
                outboundAudio(bytesSent = 0, timestampUs = 0),
                codec("codec-only"),
            ),
            isPublisher = true,
        )

        assertEquals(
            "audio/opus 48000 Hz mono minptime=10;useinbandfec=1",
            stats.publisher.audioCodec.value,
        )
    }

    //endregion
}
