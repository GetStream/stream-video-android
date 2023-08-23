package io.getstream.video.android.core.call.stats.model

// {
//   id: RTCCodec_0_Inbound_101,
//   type: codec,
//   timestampUs: 1679505083000830,
//   mimeType: video/rtx,
//   transportId: RTCTransport_0_1,
//   clockRate: 90000,
//   sdpFmtpLine: apt=100,
//   payloadType: 101
// }
data class RtcCodecStats(
    override val id: String?,
    override val type: String?,
    override val timestampUs: Double?,
    val sdpFmtpLine: String?,
    val payloadType: Long?,
    val transportId: String?,
    val mimeType: String?,
    val clockRate: Long?,
) : RtcStats() {
    companion object {
        const val SDP_FMTP_LINE = "sdpFmtpLine"
        const val PAYLOAD_TYPE = "payloadType"
        const val TRANSPORT_ID = "transportId"
        const val MIME_TYPE = "mimeType"
        const val CLOCL_RATE = "clockRate"
    }
}