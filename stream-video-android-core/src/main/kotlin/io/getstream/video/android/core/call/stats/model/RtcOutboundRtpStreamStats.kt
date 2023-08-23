package io.getstream.video.android.core.call.stats.model

import java.math.BigInteger

sealed class RtcOutboundRtpStreamStats : RtcSentRtpStreamStats() {
    abstract val mid: String?
    abstract val mediaSourceId: String?
    abstract val remoteId: String?
    abstract val headerBytesSent: BigInteger?
    abstract val retransmittedPacketsSent: BigInteger?
    abstract val retransmittedBytesSent: BigInteger?
    abstract val rtxSsrc: Long?
    abstract val targetBitrate: Double?
    abstract val totalEncodedBytesTarget: BigInteger?
    abstract val totalEncodeTime: Double?
    abstract val totalPacketSendDelay: Double?
    abstract val active: Boolean?
    
    companion object {
        const val SSRC = RtcSentRtpStreamStats.SSRC
        const val KIND = RtcSentRtpStreamStats.KIND
        const val TRANSPORT_ID = RtcSentRtpStreamStats.TRANSPORT_ID
        const val CODEC_ID = RtcSentRtpStreamStats.CODEC_ID
        const val PACKETS_SENT = RtcSentRtpStreamStats.PACKETS_SENT
        const val BYTES_SENT = RtcSentRtpStreamStats.BYTES_SENT
        const val MID = "mid"
        const val MEDIA_SOURCE_ID = "mediaSourceId"
        const val REMOTE_ID = "remoteId"
        const val HEADER_BYTES_SENT = "headerBytesSent"
        const val RETRANSMITTED_PACKETS_SENT = "retransmittedPacketsSent"
        const val RETRANSMITTED_BYTES_SENT = "retransmittedBytesSent"
        const val RTX_SSRC = "rtxSsrc"
        const val TARGET_BITRATE = "targetBitrate"
        const val TOTAL_ENCODED_BYTES_TARGET = "totalEncodedBytesTarget"
        const val TOTAL_ENCODE_TIME = "totalEncodeTime"
        const val TOTAL_PACKET_SEND_DELAY = "totalPacketSendDelay"
        const val ACTIVE = "active"
    }
}
