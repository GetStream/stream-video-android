package io.getstream.video.android.core.call.stats.model

import java.math.BigInteger

data class RtcRemoteInboundRtpVideoStreamStats(
    override val id: String?,
    override val type: String?,
    override val timestampUs: Double?,
    override val ssrc: Long?,
    override val kind: String?,
    override val transportId: String?,
    override val codecId: String?,
    override val packetsReceived: Long?,
    override val packetsLost: Int?,
    override val jitter: Double?,
    override val localId: String?,
    override val roundTripTime: Double?,
    override val totalRoundTripTime: Double?,
    override val fractionLost: Double?,
    override val roundTripTimeMeasurements: Int?,
) : RtcRemoteInboundRtpStreamStats {

    companion object {
        const val LOCAL_ID = "localId"
        const val ROUND_TRIP_TIME = "roundTripTime"
        const val TOTAL_ROUND_TRIP_TIME = "totalRoundTripTime"
        const val FRACTION_LOST = "fractionLost"
        const val ROUND_TRIP_TIME_MEASUREMENTS = "roundTripTimeMeasurements"

        const val JITTER = "jitter"
        const val PACKETS_LOST = "packetsLost"
    }

}