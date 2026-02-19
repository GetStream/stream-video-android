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
data class RtcMediaStreamVideoTrackReceiverStats(
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
    override val frameHeight: Long?,
    override val frameWidth: Long?,
    override val framesPerSecond: Double?,
    val framesReceived: Long?,
    val framesDecoded: Long?,
    val framesDropped: Long?,
    val totalFramesDuration: Double?,
    val totalFreezesDuration: Double?,
    val freezeCount: Long?,
    val pauseCount: Long?,
    val totalPausesDuration: Double?,
    val sumOfSquaredFramesDuration: Double?,
) : RtcMediaStreamTrackReceiverStats, RtcMediaStreamVideoTrackStats {

    companion object {
        const val TRACK_IDENTIFIER = "trackIdentifier"
        const val ENDED = "ended"
        const val KIND = "kind"
        const val PRIORITY = "priority"
        const val REMOTE_SOURCE = "remoteSource"
        const val DETACHED = "detached"
        const val ESTIMATED_PLAYOUT_TIMESTAMP = "estimatedPlayoutTimestamp"
        const val JITTER_BUFFER_DELAY = "jitterBufferDelay"
        const val JITTER_BUFFER_EMITTED_COUNT = "jitterBufferEmittedCount"
        const val FRAME_HEIGHT = "frameHeight"
        const val FRAME_WIDTH = "frameWidth"
        const val FRAMES_PER_SECOND = "framesPerSecond"
        const val FRAMES_RECEIVED = "framesReceived"
        const val FRAMES_DECODED = "framesDecoded"
        const val FRAMES_DROPPED = "framesDropped"
        const val TOTAL_FRAMES_DURATION = "totalFramesDuration"
        const val TOTAL_FREEZES_DURATION = "totalFreezesDuration"
        const val FREEZE_COUNT = "freezeCount"
        const val PAUSE_COUNT = "pauseCount"
        const val TOTAL_PAUSES_DURATION = "totalPausesDuration"
        const val SUM_OF_SQUARED_FRAMES_DURATION = "sumOfSquaredFramesDuration"
    }
}
