package io.getstream.video.android.core.call.stats.model

sealed class RtcReceivedRtpStreamStats : RtcRtpStreamStats() {

        abstract val packetsReceived: Long?
        abstract val packetsLost: Long?
        abstract val jitter: Double?

        companion object {
            const val SSRC = RtcRtpStreamStats.SSRC
            const val KIND = RtcRtpStreamStats.KIND
            const val TRANSPORT_ID = RtcRtpStreamStats.TRANSPORT_ID
            const val CODEC_ID = RtcRtpStreamStats.CODEC_ID
            const val PACKETS_RECEIVED = "packetsReceived"
            const val PACKETS_LOST = "packetsLost"
            const val JITTER = "jitter"
        }
}