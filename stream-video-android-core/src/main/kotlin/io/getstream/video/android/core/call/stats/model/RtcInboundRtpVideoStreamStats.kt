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

data class RtcInboundRtpVideoStreamStats(
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
    override val trackIdentifier: String?,
    override val mid: String?,
    override val remoteId: String?,
    override val lastPacketReceivedTimestamp: Double?,
    override val headerBytesReceived: BigInteger?,
    override val packetsDiscarded: BigInteger?,
    override val fecBytesReceived: BigInteger?,
    override val fecPacketsReceived: BigInteger?,
    override val fecPacketsDiscarded: BigInteger?,
    override val bytesReceived: BigInteger?,
    override val nackCount: Long?,
    override val totalProcessingDelay: Double?,
    override val estimatedPlayoutTimestamp: Double?,
    override val jitterBufferDelay: Double?,
    override val jitterBufferTargetDelay: Double?,
    override val jitterBufferEmittedCount: BigInteger?,
    override val jitterBufferMinimumDelay: Double?,
    override val decoderImplementation: String?,
    override val playoutId: String?,
    override val powerEfficientDecoder: Boolean?,
    override val retransmittedPacketsReceived: BigInteger?,
    override val retransmittedBytesReceived: BigInteger?,
    override val rtxSsrc: Long?,
    override val fecSsrc: Long?,
    val framesDecoded: Long?,
    val keyFramesDecoded: Long?,
    val framesRendered: Long?,
    val framesDropped: Long?,
    val frameWidth: Long?,
    val frameHeight: Long?,
    val framesPerSecond: Double?,
    val qpSum: BigInteger?,
    val totalDecodeTime: Double?,
    val totalInterFrameDelay: Double?,
    val totalSquaredInterFrameDelay: Double?,
    val pauseCount: Long?,
    val totalPausesDuration: Double?,
    val freezeCount: Long?,
    val totalFreezesDuration: Double?,
    val firCount: Long?,
    val pliCount: Long?,
    val framesReceived: Long?,
    val framesAssembledFromMultiplePackets: Long?,
    val totalAssemblyTime: Double?,
) : RtcInboundRtpStreamStats {

    companion object {
        const val FRAMES_DECODED = "framesDecoded"
        const val KEY_FRAMES_DECODED = "keyFramesDecoded"
        const val FRAMES_RENDERED = "framesRendered"
        const val FRAMES_DROPPED = "framesDropped"
        const val FRAME_WIDTH = "frameWidth"
        const val FRAME_HEIGHT = "frameHeight"
        const val FRAMES_PER_SECOND = "framesPerSecond"
        const val QP_SUM = "qpSum"
        const val TOTAL_DECODE_TIME = "totalDecodeTime"
        const val TOTAL_INTER_FRAME_DELAY = "totalInterFrameDelay"
        const val TOTAL_SQUARED_INTER_FRAME_DELAY = "totalSquaredInterFrameDelay"
        const val PAUSE_COUNT = "pauseCount"
        const val TOTAL_PAUSES_DURATION = "totalPausesDuration"
        const val FREEZE_COUNT = "freezeCount"
        const val TOTAL_FREEZES_DURATION = "totalFreezesDuration"
        const val FIR_COUNT = "firCount"
        const val PLI_COUNT = "pliCount"
        const val FRAMES_RECEIVED = "framesReceived"
        const val FRAMES_ASSEMBLED_FROM_MULTIPLE_PACKETS = "framesAssembledFromMultiplePackets"
        const val TOTAL_ASSEMBLY_TIME = "totalAssemblyTime"
    }
}
