package io.getstream.video.android.core.call.stats.model

import java.math.BigInteger

data class RtcIceCandidatePairStats(
    override val id: String?,
    override val type: String?,
    override val timestampUs: Double?,
    val transportId: String?,
    val requestsSent: BigInteger?,
    val localCandidateId: String?,
    val bytesSent: BigInteger?,
    val bytesDiscardedOnSend: BigInteger?,
    val priority: BigInteger?,
    val requestsReceived: BigInteger?,
    val writable: Boolean?,
    val remoteCandidateId: String?,
    val bytesReceived: BigInteger?,
    val packetsReceived: BigInteger?,
    val responsesSent: BigInteger?,
    val packetsDiscardedOnSend: BigInteger?,
    val nominated: Boolean?,
    val packetsSent: BigInteger?,
    val totalRoundTripTime: Double?,
    val responsesReceived: BigInteger?,
    val state: String?,
    val consentRequestsSent: BigInteger?,
) : RtcStats() {
    companion object {
        const val TRANSPORT_ID = "transportId"
        const val REQUESTS_SENT = "requestsSent"
        const val LOCAL_CANDIDATE_ID = "localCandidateId"
        const val BYTES_SENT = "bytesSent"
        const val BYTES_DISCARDED_ON_SEND = "bytesDiscardedOnSend"
        const val PRIORITY = "priority"
        const val REQUESTS_RECEIVED = "requestsReceived"
        const val WRITABLE = "writable"
        const val REMOTE_CANDIDATE_ID = "remoteCandidateId"
        const val BYTES_RECEIVED = "bytesReceived"
        const val PACKETS_RECEIVED = "packetsReceived"
        const val RESPONSES_SENT = "responsesSent"
        const val PACKETS_DISCARDED_ON_SEND = "packetsDiscardedOnSend"
        const val NOMINATED = "nominated"
        const val PACKETS_SENT = "packetsSent"
        const val TOTAL_ROUND_TRIP_TIME = "totalRoundTripTime"
        const val RESPONSES_RECEIVED = "responsesReceived"
        const val STATE = "state"
        const val CONSENT_REQUESTS_SENT = "consentRequestsSent"
    }
}