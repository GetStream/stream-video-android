package io.getstream.video.android.core.call.stats.model

sealed interface RtcRtpStreamStats : RtcStats {

        val ssrc: Long?
        val kind: String?
        val transportId: String?
        val codecId: String?

        companion object {
            const val SSRC = "ssrc"
            const val KIND = "kind"
            const val TRANSPORT_ID = "transportId"
            const val CODEC_ID = "codecId"
        }
}