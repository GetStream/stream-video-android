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

sealed interface RtcOutboundRtpStreamStats : RtcSentRtpStreamStats {
    val mid: String?
    val mediaSourceId: String?
    val remoteId: String?
    val headerBytesSent: BigInteger?
    val retransmittedPacketsSent: BigInteger?
    val retransmittedBytesSent: BigInteger?
    val rtxSsrc: Long?
    val targetBitrate: Double?
    val totalEncodedBytesTarget: BigInteger?
    val totalEncodeTime: Double?
    val totalPacketSendDelay: Double?
    val active: Boolean?

    companion object {
        const val SSRC = RtcSentRtpStreamStats.SSRC
        const val KIND = RtcSentRtpStreamStats.KIND
        const val TRANSPORT_ID = RtcSentRtpStreamStats.TRANSPORT_ID
        const val CODEC_ID = RtcSentRtpStreamStats.CODEC_ID
        const val PACKETS_SENT = RtcSentRtpStreamStats.PACKETS_SENT
        const val BYTES_SENT = RtcSentRtpStreamStats.BYTES_SENT
        const val MID = "mid"
        const val MEDIA_SOURCE_ID = "mediaSourceId"
        const val REMOTE_ID = "remoteId"
        const val HEADER_BYTES_SENT = "headerBytesSent"
        const val RETRANSMITTED_PACKETS_SENT = "retransmittedPacketsSent"
        const val RETRANSMITTED_BYTES_SENT = "retransmittedBytesSent"
        const val RTX_SSRC = "rtxSsrc"
        const val TARGET_BITRATE = "targetBitrate"
        const val TOTAL_ENCODED_BYTES_TARGET = "totalEncodedBytesTarget"
        const val TOTAL_ENCODE_TIME = "totalEncodeTime"
        const val TOTAL_PACKET_SEND_DELAY = "totalPacketSendDelay"
        const val ACTIVE = "active"
    }
}
