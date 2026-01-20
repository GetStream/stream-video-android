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

package io.getstream.video.android.core.call.stats.model

import java.math.BigInteger

sealed interface RtcInboundRtpStreamStats : RtcReceivedRtpStreamStats {

    val trackIdentifier: String?
    val mid: String?
    val remoteId: String?
    val lastPacketReceivedTimestamp: Double?
    val headerBytesReceived: BigInteger?
    val bytesReceived: BigInteger?
    val packetsDiscarded: BigInteger?
    val fecBytesReceived: BigInteger?
    val fecPacketsReceived: BigInteger?
    val fecPacketsDiscarded: BigInteger?
    val jitterBufferDelay: Double?
    val jitterBufferTargetDelay: Double?
    val jitterBufferEmittedCount: BigInteger?
    val jitterBufferMinimumDelay: Double?
    val nackCount: Long?
    val totalProcessingDelay: Double?
    val estimatedPlayoutTimestamp: Double?
    val decoderImplementation: String?
    val playoutId: String?
    val powerEfficientDecoder: Boolean?
    val retransmittedPacketsReceived: BigInteger?
    val retransmittedBytesReceived: BigInteger?
    val rtxSsrc: Long?
    val fecSsrc: Long?

    companion object {
        const val SSRC = RtcReceivedRtpStreamStats.SSRC
        const val KIND = RtcReceivedRtpStreamStats.KIND
        const val TRANSPORT_ID = RtcReceivedRtpStreamStats.TRANSPORT_ID
        const val CODEC_ID = RtcReceivedRtpStreamStats.CODEC_ID
        const val PACKETS_RECEIVED = RtcReceivedRtpStreamStats.PACKETS_RECEIVED
        const val PACKETS_LOST = RtcReceivedRtpStreamStats.PACKETS_LOST
        const val JITTER = RtcReceivedRtpStreamStats.JITTER
        const val TRACK_IDENTIFIER = "trackIdentifier"
        const val MID = "mid"
        const val REMOTE_ID = "remoteId"
        const val LAST_PACKET_RECEIVED_TIMESTAMP = "lastPacketReceivedTimestamp"
        const val HEADER_BYTES_RECEIVED = "headerBytesReceived"
        const val PACKETS_DISCARDED = "packetsDiscarded"
        const val FEC_BYTES_RECEIVED = "fecBytesReceived"
        const val FEC_PACKETS_RECEIVED = "fecPacketsReceived"
        const val FEC_PACKETS_DISCARDED = "fecPacketsDiscarded"
        const val BYTES_RECEIVED = "bytesReceived"
        const val NACK_COUNT = "nackCount"
        const val TOTAL_PROCESSING_DELAY = "totalProcessingDelay"
        const val ESTIMATED_PLAYOUT_TIMESTAMP = "estimatedPlayoutTimestamp"
        const val JITTER_BUFFER_DELAY = "jitterBufferDelay"
        const val JITTER_BUFFER_TARGET_DELAY = "jitterBufferTargetDelay"
        const val JITTER_BUFFER_EMITTED_COUNT = "jitterBufferEmittedCount"
        const val JITTER_BUFFER_MINIMUM_DELAY = "jitterBufferMinimumDelay"
        const val DECODER_IMPLEMENTATION = "decoderImplementation"
        const val PLAYOUT_ID = "playoutId"
        const val POWER_EFFICIENT_DECODER = "powerEfficientDecoder"
        const val RETRANSMITTED_PACKETS_RECEIVED = "retransmittedPacketsReceived"
        const val RETRANSMITTED_BYTES_RECEIVED = "retransmittedBytesReceived"
        const val RTX_SSRC = "rtxSsrc"
        const val FEC_SSRC = "fecSsrc"
    }
}
