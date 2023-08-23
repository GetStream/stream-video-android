package io.getstream.video.android.core.call.stats.model

sealed class RtcRtpStreamStats : RtcStats() {

        abstract val ssrc: Long?
        abstract val kind: String?
        abstract val transportId: String?
        abstract val codecId: String?

        companion object {
            const val SSRC = "ssrc"
            const val KIND = "kind"
            const val TRANSPORT_ID = "transportId"
            const val CODEC_ID = "codecId"
        }
}