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

// {
//  id: RTCMediaStreamTrack_receiver_5,
//  type: track,
//  timestamp: 1679434739982604.0,
//  totalAudioEnergy: 0.008388901526724536,
//  totalInterruptionDuration: 0.0,
//  removedSamplesForAcceleration: 373,
//  kind: audio,
//  audioLevel: 0.10351878414258248,
//  jitterBufferDelay: 17644.8,
//  interruptionCount: 0,
//  relativePacketArrivalDelay: 7.19,
//  jitterBufferFlushes: 2,
//  concealedSamples: 58080,
//  jitterBufferTargetDelay: 18892.8,
//  trackIdentifier: fb83345f-0514-4793-872b-10524723b9e4,
//  totalSamplesDuration: 8.589999999999861,
//  detached: false,
//  insertedSamplesForDeceleration: 3094,
//  jitterBufferEmittedCount: 236160,
//  delayedPacketOutageSamples: 0,
//  ended: false,
//  totalSamplesReceived: 296160,
//  concealmentEvents: 1,
//  remoteSource: true,
//  silentConcealedSamples: 57560
// }

// https://www.w3.org/TR/2023/CRD-webrtc-stats-20230427/#dom-rtcmediastreamtrackstats
@Deprecated("Was deprecated in 11 May 2023")
data class RtcMediaStreamAudioTrackReceiverStats(
    override val id: String?,
    override val type: String?,
    override val timestampUs: Double?,
    override val trackIdentifier: String?,
    override val ended: Boolean?,
    override val kind: String?,
    override val priority: String?,
    override val remoteSource: Boolean?,
    override val detached: Boolean?,
    override val estimatedPlayoutTimestamp: Double?,
    override val jitterBufferDelay: Double?,
    override val jitterBufferEmittedCount: Long?,
    override val audioLevel: Double?,
    override val totalAudioEnergy: Double?,
    override val totalSamplesDuration: Double?,
    val totalInterruptionDuration: Double?,
    val removedSamplesForAcceleration: Long?,
    val interruptionCount: Long?,
    val relativePacketArrivalDelay: Double?,
    val jitterBufferFlushes: Long?,
    val concealedSamples: Long?,
    val jitterBufferTargetDelay: Double?,
    val insertedSamplesForDeceleration: Long?,
    val delayedPacketOutageSamples: Long?,
    val totalSamplesReceived: Long?,
    val concealmentEvents: Long?,
    val silentConcealedSamples: Long?,
) : RtcMediaStreamTrackReceiverStats, RtcMediaStreamAudioTrackStats {

    companion object {
        const val KIND = "kind"
        const val TRACK_IDENTIFIER = "trackIdentifier"
        const val PRIORITY = "priority"
        const val JITTER_BUFFER_DELAY = "jitterBufferDelay"
        const val JITTER_BUFFER_EMITTED_COUNT = "jitterBufferEmittedCount"
        const val ESTIMATED_PLAYOUT_TIMESTAMP = "estimatedPlayoutTimestamp"
        const val REMOTE_SOURCE = "remoteSource"
        const val DETACHED = "detached"
        const val ENDED = "ended"
        const val TOTAL_AUDIO_ENERGY = "totalAudioEnergy"
        const val TOTAL_INTERRUPTION_DURATION = "totalInterruptionDuration"
        const val REMOVED_SAMPLES_FOR_ACCELERATION = "removedSamplesForAcceleration"
        const val AUDIO_LEVEL = "audioLevel"
        const val INTERRUPTION_COUNT = "interruptionCount"
        const val RELATIVE_PACKET_ARRIVAL_DELAY = "relativePacketArrivalDelay"
        const val JITTER_BUFFER_FLUSHES = "jitterBufferFlushes"
        const val CONCEALED_SAMPLES = "concealedSamples"
        const val JITTER_BUFFER_TARGET_DELAY = "jitterBufferTargetDelay"
        const val TOTAL_SAMPLES_DURATION = "totalSamplesDuration"
        const val INSERTED_SAMPLES_FOR_DECELERATION = "insertedSamplesForDeceleration"
        const val DELAYED_PACKET_OUTAGE_SAMPLES = "delayedPacketOutageSamples"
        const val TOTAL_SAMPLES_RECEIVED = "totalSamplesReceived"
        const val CONCEALMENT_EVENTS = "concealmentEvents"
        const val SILENT_CONCEALED_SAMPLES = "silentConcealedSamples"
    }
}
