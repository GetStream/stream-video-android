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

// {
//  id: RTCMediaStreamTrack_receiver_6,
//  type: track,
//  timestamp: 1679434739982604.0,
//  kind: video,
//  frameHeight: 540,
//  frameWidth: 960,
//  framesReceived: 127,
//  framesDecoded: 126,
//  framesDropped: 1
//  totalFramesDuration: 4.572,
//  totalFreezesDuration: 0.0,
//  freezeCount: 0,
//  pauseCount: 0,
//  detached: false,
//  ended: false,
//  remoteSource: true,
//  jitterBufferDelay: 4.69,
//  trackIdentifier: 7df380d6-8e4c-4fb7-a6f2-bdc42bbd0dc6,
//  totalPausesDuration: 0.0,
//  jitterBufferEmittedCount: 126,
//  sumOfSquaredFramesDuration: 0.2560839999999999,
// }

// https://www.w3.org/TR/2023/CRD-webrtc-stats-20230427/#dom-rtcmediastreamtrackstats
@Deprecated("Was deprecated in 11 May 2023")
sealed interface RtcMediaStreamTrackReceiverStats : RtcMediaStreamTrackStats {
    val estimatedPlayoutTimestamp: Double?
    val jitterBufferDelay: Double?
    val jitterBufferEmittedCount: Long?

    companion object {
        const val ESTIMATED_PLAYOUT_TIMESTAMP = "estimatedPlayoutTimestamp"
        const val JITTER_BUFFER_DELAY = "jitterBufferDelay"
        const val JITTER_BUFFER_EMITTED_COUNT = "jitterBufferEmittedCount"
    }
}
