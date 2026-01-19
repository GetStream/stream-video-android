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

// https://www.w3.org/TR/2023/CRD-webrtc-stats-20230427/#dom-rtcmediastreamtrackstats
@Deprecated("Was deprecated in 11 May 2023")
sealed interface RtcMediaStreamTrackStats : RtcStats {
    val trackIdentifier: String?
    val ended: Boolean?
    val kind: String?
    val priority: String?
    val remoteSource: Boolean?
    val detached: Boolean?

    companion object {
        const val TRACK_IDENTIFIER = "trackIdentifier"
        const val KIND = "kind"
        const val ENDED = "ended"
        const val PRIORITY = "priority"
        const val REMOTE_SOURCE = "remoteSource"
        const val DETACHED = "detached"
    }
}
