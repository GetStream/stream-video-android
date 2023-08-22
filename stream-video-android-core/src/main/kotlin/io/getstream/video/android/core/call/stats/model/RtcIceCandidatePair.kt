package io.getstream.video.android.core.call.stats.model

data class RtcIceCandidatePair(
    override val id: String?,
    override val type: String?,
    override val timestampUs: Double?,
    val transportId: String?,
    val requestsSent: Long?,
    val localCandidateId: String?,
    val bytesSent: Long?,
    val bytesDiscardedOnSend: Long?,
    val priority: Long?,
    val requestsReceived: Long?,
    val writable: Boolean?,
    val remoteCandidateId: String?,
    val bytesReceived: Long?,
    val packetsReceived: Long?,
    val responsesSent: Long?,
    val packetsDiscardedOnSend: Long?,
    val nominated: Boolean?,
    val packetsSent: Long?,
    val totalRoundTripTime: Double?,
    val responsesReceived: Long?,
    val state: String?,
    val consentRequestsSent: Long?,
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