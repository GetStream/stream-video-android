/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-video-android/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.video.android.core.call.stats.model

import androidx.compose.runtime.Immutable

@Immutable
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
