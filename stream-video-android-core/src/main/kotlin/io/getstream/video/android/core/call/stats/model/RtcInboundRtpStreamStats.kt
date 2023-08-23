package io.getstream.video.android.core.call.stats.model

import java.math.BigInteger


sealed class RtcInboundRtpStreamStats : RtcReceivedRtpStreamStats() {

    abstract val trackIdentifier: String?
    abstract val mid: String?
    abstract val remoteId: String?
    abstract val lastPacketReceivedTimestamp: Double?
    abstract val headerBytesReceived: BigInteger?
    abstract val bytesReceived: BigInteger?
    abstract val packetsDiscarded: BigInteger?
    abstract val fecBytesReceived: BigInteger?
    abstract val fecPacketsReceived: BigInteger?
    abstract val fecPacketsDiscarded: BigInteger?
    abstract val jitterBufferDelay: Double?
    abstract val jitterBufferTargetDelay: Double?
    abstract val jitterBufferEmittedCount: BigInteger?
    abstract val jitterBufferMinimumDelay: Double?
    abstract val nackCount: Long?
    abstract val totalProcessingDelay: Double?
    abstract val estimatedPlayoutTimestamp: Double?
    abstract val decoderImplementation: String?
    abstract val playoutId: String?
    abstract val powerEfficientDecoder: Boolean?
    abstract val retransmittedPacketsReceived: BigInteger?
    abstract val retransmittedBytesReceived: BigInteger?
    abstract val rtxSsrc: Long?
    abstract val fecSsrc: Long?

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