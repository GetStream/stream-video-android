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

import io.getstream.video.android.core.call.stats.model.discriminator.RtcQualityLimitationReason
import java.math.BigInteger

data class RtcOutboundRtpVideoStreamStats(
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
    val rid: String?,
    val frameWidth: Long?,
    val frameHeight: Long?,
    val framesPerSecond: Double?,
    val framesSent: Long?,
    val hugeFramesSent: Long?,
    val framesEncoded: Long?,
    val keyFramesEncoded: Long?,
    val qpSum: BigInteger?,
    val qualityLimitationReason: RtcQualityLimitationReason?,
    val qualityLimitationDurations: Map<RtcQualityLimitationReason, Double>?,
    val qualityLimitationResolutionChanges: Long?,
    val nackCount: Long?,
    val firCount: Long?,
    val pliCount: Long?,
    val encoderImplementation: String?,
    val powerEfficientEncoder: Boolean?,
    val scalabilityMode: String?,
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
        const val RID = "rid"
        const val FRAME_WIDTH = "frameWidth"
        const val FRAME_HEIGHT = "frameHeight"
        const val FRAMES_PER_SECOND = "framesPerSecond"
        const val FRAMES_SENT = "framesSent"
        const val HUGE_FRAMES_SENT = "hugeFramesSent"
        const val FRAMES_ENCODED = "framesEncoded"
        const val KEY_FRAMES_ENCODED = "keyFramesEncoded"
        const val QP_SUM = "qpSum"
        const val QUALITY_LIMITATION_REASON = "qualityLimitationReason"
        const val QUALITY_LIMITATION_DURATIONS = "qualityLimitationDurations"
        const val QUALITY_LIMITATION_RESOLUTION_CHANGES = "qualityLimitationResolutionChanges"
        const val NACK_COUNT = "nackCount"
        const val FIR_COUNT = "firCount"
        const val PLI_COUNT = "pliCount"
        const val ENCODER_IMPLEMENTATION = "encoderImplementation"
        const val POWER_EFFICIENT_ENCODER = "powerEfficientEncoder"
        const val SCALABILITY_MODE = "scalabilityMode"
    }
}
