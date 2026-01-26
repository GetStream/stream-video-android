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

data class RtcInboundRtpAudioStreamStats(
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
    override val bytesReceived: BigInteger?,
    override val packetsDiscarded: BigInteger?,
    override val fecBytesReceived: BigInteger?,
    override val fecPacketsReceived: BigInteger?,
    override val fecPacketsDiscarded: BigInteger?,
    override val jitterBufferDelay: Double?,
    override val jitterBufferTargetDelay: Double?,
    override val jitterBufferEmittedCount: BigInteger?,
    override val jitterBufferMinimumDelay: Double?,
    override val nackCount: Long?,
    override val totalProcessingDelay: Double?,
    override val estimatedPlayoutTimestamp: Double?,
    override val decoderImplementation: String?,
    override val playoutId: String?,
    override val powerEfficientDecoder: Boolean?,
    override val retransmittedPacketsReceived: BigInteger?,
    override val retransmittedBytesReceived: BigInteger?,
    override val rtxSsrc: Long?,
    override val fecSsrc: Long?,
    val audioLevel: Double?,
    val totalAudioEnergy: Double?,
    val totalSamplesReceived: BigInteger?,
    val totalSamplesDuration: Double?,
    val concealedSamples: BigInteger?,
    val silentConcealedSamples: BigInteger?,
    val concealmentEvents: BigInteger?,
    val insertedSamplesForDeceleration: BigInteger?,
    val removedSamplesForAcceleration: BigInteger?,
) : RtcInboundRtpStreamStats {

    companion object {
        const val AUDIO_LEVEL = "audioLevel"
        const val TOTAL_AUDIO_ENERGY = "totalAudioEnergy"
        const val TOTAL_SAMPLES_RECEIVED = "totalSamplesReceived"
        const val TOTAL_SAMPLES_DURATION = "totalSamplesDuration"
        const val CONCEALED_SAMPLES = "concealedSamples"
        const val SILENT_CONCEALED_SAMPLES = "silentConcealedSamples"
        const val CONCEALMENT_EVENTS = "concealmentEvents"
        const val INSERTED_SAMPLES_FOR_DECELERATION = "insertedSamplesForDeceleration"
        const val REMOVED_SAMPLES_FOR_ACCELERATION = "removedSamplesForAcceleration"
    }
}
