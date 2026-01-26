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

import java.math.BigInteger

data class RtcOutboundRtpAudioStreamStats(
    override val id: String?,
    override val type: String?,
    override val timestampUs: Double?,
    override val ssrc: Long?,
    override val kind: String?,
    override val transportId: String?,
    override val codecId: String?,
    override val packetsSent: BigInteger?,
    override val bytesSent: BigInteger?,
    override val mid: String?,
    override val mediaSourceId: String?,
    override val remoteId: String?,
    override val headerBytesSent: BigInteger?,
    override val retransmittedPacketsSent: BigInteger?,
    override val retransmittedBytesSent: BigInteger?,
    override val rtxSsrc: Long?,
    override val targetBitrate: Double?,
    override val totalEncodedBytesTarget: BigInteger?,
    override val totalEncodeTime: Double?,
    override val totalPacketSendDelay: Double?,
    override val active: Boolean?,
) : RtcOutboundRtpStreamStats {

    companion object {
        const val SSRC = RtcSentRtpStreamStats.SSRC
        const val KIND = RtcSentRtpStreamStats.KIND
        const val TRANSPORT_ID = RtcSentRtpStreamStats.TRANSPORT_ID
        const val CODEC_ID = RtcSentRtpStreamStats.CODEC_ID
        const val PACKETS_SENT = RtcSentRtpStreamStats.PACKETS_SENT
        const val BYTES_SENT = RtcSentRtpStreamStats.BYTES_SENT
        const val MID = RtcOutboundRtpStreamStats.MID
        const val MEDIA_SOURCE_ID = RtcOutboundRtpStreamStats.MEDIA_SOURCE_ID
        const val REMOTE_ID = RtcOutboundRtpStreamStats.REMOTE_ID
        const val HEADER_BYTES_SENT = RtcOutboundRtpStreamStats.HEADER_BYTES_SENT
        const val RETRANSMITTED_PACKETS_SENT = RtcOutboundRtpStreamStats.RETRANSMITTED_PACKETS_SENT
        const val RETRANSMITTED_BYTES_SENT = RtcOutboundRtpStreamStats.RETRANSMITTED_BYTES_SENT
        const val RTX_SSRC = RtcOutboundRtpStreamStats.RTX_SSRC
        const val TARGET_BITRATE = RtcOutboundRtpStreamStats.TARGET_BITRATE
        const val TOTAL_ENCODED_BYTES_TARGET = RtcOutboundRtpStreamStats.TOTAL_ENCODED_BYTES_TARGET
        const val TOTAL_ENCODE_TIME = RtcOutboundRtpStreamStats.TOTAL_ENCODE_TIME
        const val TOTAL_PACKET_SEND_DELAY = RtcOutboundRtpStreamStats.TOTAL_PACKET_SEND_DELAY
        const val ACTIVE = RtcOutboundRtpStreamStats.ACTIVE
    }
}
