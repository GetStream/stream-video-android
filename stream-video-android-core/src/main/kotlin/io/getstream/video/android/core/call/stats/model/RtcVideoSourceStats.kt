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

data class RtcVideoSourceStats(
    override val id: String?,
    override val type: String?,
    override val timestampUs: Double?,
    override val kind: String?,
    override val trackIdentifier: String?,
    val width: Long?,
    val height: Long?,
    val framesPerSecond: Double?,
    val frames: Long?,

) : RtcMediaSourceStats {

    companion object {
        const val KIND = RtcMediaSourceStats.KIND
        const val TRACK_IDENTIFIER = RtcMediaSourceStats.TRACK_IDENTIFIER
        const val WIDTH = "width"
        const val HEIGHT = "height"
        const val FRAMES_PER_SECOND = "framesPerSecond"
        const val FRAMES = "frames"
    }
}
