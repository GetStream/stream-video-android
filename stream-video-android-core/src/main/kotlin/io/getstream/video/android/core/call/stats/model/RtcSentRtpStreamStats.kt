package io.getstream.video.android.core.call.stats.model

import java.math.BigInteger

sealed interface RtcSentRtpStreamStats : RtcRtpStreamStats {

        val packetsSent: BigInteger?
        val bytesSent: BigInteger?

        companion object {
            const val SSRC = RtcRtpStreamStats.SSRC
            const val KIND = RtcRtpStreamStats.KIND
            const val TRANSPORT_ID = RtcRtpStreamStats.TRANSPORT_ID
            const val CODEC_ID = RtcRtpStreamStats.CODEC_ID
            const val PACKETS_SENT = "packetsSent"
            const val BYTES_SENT = "packetsLost"
        }
}