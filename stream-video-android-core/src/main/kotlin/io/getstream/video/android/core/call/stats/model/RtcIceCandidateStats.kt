package io.getstream.video.android.core.call.stats.model

data class RtcIceCandidateStats(
    override val id: String?,
    override val type: String?,
    override val timestampUs: Double?,
    val transportId: String?,
    val candidateType: String?,
    val protocol: String?,
    val address: String?,
    val port: Int?,
    val vpn: Boolean?,
    val isRemote: Boolean?,
    val ip: String?,
    val networkAdapterType: String?,
    val networkType: String?,
    val priority: Int?,
    val url: String?,
    val relayProtocol: String?,
) : RtcStats {

    companion object {
        const val TRANSPORT_ID = "transportId"
        const val CANDIDATE_TYPE = "candidateType"
        const val PROTOCOL = "protocol"
        const val ADDRESS = "address"
        const val PORT = "port"
        const val VPN = "vpn"
        const val IS_REMOTE = "isRemote"
        const val IP = "ip"
        const val NETWORK_ADAPTER_TYPE = "networkAdapterType"
        const val NETWORK_TYPE = "networkType"
        const val PRIORITY = "priority"
        const val URL = "url"
        const val RELAY_PROTOCOL = "relayProtocol"
    }
}