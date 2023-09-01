package io.getstream.video.android.core.call.stats.model

import java.math.BigInteger

data class RtcRemoteOutboundRtpVideoStreamStats(
    override val id: String?,
    override val type: String?,
    override val timestampUs: Double?,
    override val ssrc: Long?,
    override val kind: String?,
    override val transportId: String?,
    override val codecId: String?,
    override val packetsSent: BigInteger?,
    override val bytesSent: BigInteger?,
    override val localId: String?,
    override val remoteTimestamp: Double?,
    override val reportsSent: BigInteger?,
    override val roundTripTime: Double?,
    override val totalRoundTripTime: Double?,
    override val roundTripTimeMeasurements: BigInteger?,
) : RtcRemoteOutboundRtpStreamStats {

    companion object {
        const val LOCAL_ID = "localId"
        const val REMOTE_TIMESTAMP = "remoteTimestamp"
        const val REPORTS_SENT = "reportsSent"
        const val ROUND_TRIP_TIME = "roundTripTime"
        const val TOTAL_ROUND_TRIP_TIME = "totalRoundTripTime"
        const val ROUND_TRIP_TIME_MEASUREMENTS = "roundTripTimeMeasurements"
    }

}