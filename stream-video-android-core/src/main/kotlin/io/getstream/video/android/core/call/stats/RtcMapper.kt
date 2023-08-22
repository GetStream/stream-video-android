package io.getstream.video.android.core.call.stats

import io.getstream.log.StreamLog
import io.getstream.video.android.core.call.stats.model.RtcCodec
import io.getstream.video.android.core.call.stats.model.RtcIceCandidatePair
import io.getstream.video.android.core.call.stats.model.RtcReportType
import org.webrtc.RTCStats
import org.webrtc.RTCStatsReport

fun RTCStatsReport.toRtcStats() {

    for (report in statsMap.values) {
        when (RtcReportType.fromAlias(report.type)) {
            RtcReportType.CODEC -> {
                val codec = RtcCodec(
                    id = report.id,
                    type = report.type,
                    timestampUs = report.timestampUs,
                    sdpFmtpLine = report.getStringOrNull(RtcCodec.SDP_FMTP_LINE),
                    payloadType = report.getLongOrNull(RtcCodec.PAYLOAD_TYPE),
                    transportId = report.getStringOrNull(RtcCodec.TRANSPORT_ID),
                    mimeType = report.getStringOrNull(RtcCodec.MIME_TYPE),
                    clockRate = report.getLongOrNull(RtcCodec.CLOCL_RATE),
                )
                StreamLog.i("RtcParser") { "[toRtcStats] codec: $codec" }
            }
            RtcReportType.CANDIDATE_PAIR -> {
                val candidatePair = RtcIceCandidatePair(
                    id = report.id,
                    type = report.type,
                    timestampUs = report.timestampUs,
                    transportId = report.getStringOrNull(RtcCodec.TRANSPORT_ID),
                    requestsSent = report.getLongOrNull(RtcIceCandidatePair.REQUESTS_SENT),
                    localCandidateId = report.getStringOrNull(RtcIceCandidatePair.LOCAL_CANDIDATE_ID),
                    bytesSent = report.getLongOrNull(RtcIceCandidatePair.BYTES_SENT),
                    bytesDiscardedOnSend = report.getLongOrNull(RtcIceCandidatePair.BYTES_DISCARDED_ON_SEND),
                    priority = report.getLongOrNull(RtcIceCandidatePair.PRIORITY),
                    requestsReceived = report.getLongOrNull(RtcIceCandidatePair.REQUESTS_RECEIVED),
                    writable = report.getBooleanOrNull(RtcIceCandidatePair.WRITABLE),
                    remoteCandidateId = report.getStringOrNull(RtcIceCandidatePair.REMOTE_CANDIDATE_ID),
                    bytesReceived = report.getLongOrNull(RtcIceCandidatePair.BYTES_RECEIVED),
                    packetsReceived = report.getLongOrNull(RtcIceCandidatePair.PACKETS_RECEIVED),
                    responsesSent = report.getLongOrNull(RtcIceCandidatePair.RESPONSES_SENT),
                    packetsDiscardedOnSend = report.getLongOrNull(RtcIceCandidatePair.PACKETS_DISCARDED_ON_SEND),
                    nominated = report.getBooleanOrNull(RtcIceCandidatePair.NOMINATED),
                    packetsSent = report.getLongOrNull(RtcIceCandidatePair.PACKETS_SENT),
                    totalRoundTripTime = report.getDoubleOrNull(RtcIceCandidatePair.TOTAL_ROUND_TRIP_TIME),
                    responsesReceived = report.getLongOrNull(RtcIceCandidatePair.RESPONSES_RECEIVED),
                    state = report.getStringOrNull(RtcIceCandidatePair.STATE),
                    consentRequestsSent = report.getLongOrNull(RtcIceCandidatePair.CONSENT_REQUESTS_SENT),
                )
                StreamLog.i("RtcParser") { "[toRtcStats] candidatePair: $candidatePair" }
            }

            else -> {

            }
        }
    }
}

fun RTCStats.getStringOrNull(key: String): String? = members[key] as String?
fun RTCStats.getDoubleOrNull(key: String): Double? = members[key] as Double?
fun RTCStats.getLongOrNull(key: String): Long? = members[key] as Long?
fun RTCStats.getBooleanOrNull(key: String): Boolean? = members[key] as Boolean?
