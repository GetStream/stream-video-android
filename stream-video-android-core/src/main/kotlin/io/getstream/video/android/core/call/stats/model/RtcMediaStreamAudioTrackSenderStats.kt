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
//  id: RTCMediaStreamTrack_sender_3,
//  type: track,
//  timestamp: 1679425535359288.0,
//  kind: audio,
//  mediaSourceId: RTCAudioSource_3
//  trackIdentifier: 3137ee0d-6f04-4e53-a45a-20fbdcdcd1ca,
//  remoteSource: false,
//  detached: false,
//  ended: false,
// }

// https://www.w3.org/TR/2023/CRD-webrtc-stats-20230427/#dom-rtcmediastreamtrackstats
@Deprecated("Was deprecated in 11 May 2023")
data class RtcMediaStreamAudioTrackSenderStats(
    override val id: String?,
    override val type: String?,
    override val timestampUs: Double?,
    override val trackIdentifier: String?,
    override val ended: Boolean?,
    override val kind: String?,
    override val priority: String?,
    override val remoteSource: Boolean?,
    override val detached: Boolean?,
    override val mediaSourceId: String?,
    override val audioLevel: Double?,
    override val totalAudioEnergy: Double?,
    override val totalSamplesDuration: Double?,
) : RtcMediaStreamTrackSenderStats, RtcMediaStreamAudioTrackStats {

    companion object {
        const val TRACK_IDENTIFIER = "trackIdentifier"
        const val ENDED = "ended"
        const val KIND = "kind"
        const val PRIORITY = "priority"
        const val REMOTE_SOURCE = "remoteSource"
        const val DETACHED = "detached"
        const val MEDIA_SOURCE_ID = "mediaSourceId"
        const val AUDIO_LEVEL = "audioLevel"
        const val TOTAL_AUDIO_ENERGY = "totalAudioEnergy"
        const val TOTAL_SAMPLES_DURATION = "totalSamplesDuration"
    }
}
