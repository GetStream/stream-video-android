package io.getstream.video.android.core.call.stats.model.discriminator

enum class RtcReportType(
    private val alias: String,
) {
    CERTIFICATE("certificate"),
    CODEC("codec"),
    CANDIDATE_PAIR("candidate-pair"),
    REMOTE_CANDIDATE("remote-candidate"),
    LOCAL_CANDIDATE("local-candidate"),
    REMOTE_INBOUND_RTP("remote-inbound-rtp"),
    REMOTE_OUTBOUND_RTP("remote-outbound-rtp"),
    INBOUND_RTP("inbound-rtp"),
    OUTBOUND_RTP("outbound-rtp"),
    TRACK("track"),
    MEDIA_SOURCE("media-source"),
    STREAM("stream"),
    PEER_CONNECTION("peer-connection"),
    TRANSPORT("transport"),
    UNKNOWN("unknown");

    override fun toString(): String = alias


    companion object {
        fun fromAlias(alias: String?): RtcReportType {
            return RtcReportType.values().firstOrNull {
                it.alias == alias
            } ?: UNKNOWN
        }
    }

}