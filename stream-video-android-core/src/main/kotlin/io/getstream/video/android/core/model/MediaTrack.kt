/*
 * Copyright (c) 2014-2024 Stream.io Inc. All rights reserved.
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

package io.getstream.video.android.core.model

import androidx.compose.runtime.Stable

@Stable
public sealed class MediaTrack(
    public open val streamId: String,
) {
    public fun asVideoTrack(): VideoTrack? {
        return this as? VideoTrack
    }

    public fun asAudioTrack(): AudioTrack? {
        return this as? AudioTrack
    }

    public fun enableVideo(enabled: Boolean) {
        asVideoTrack()?.video?.setEnabled(enabled)
    }

    public fun enableAudio(enabled: Boolean) {
        asAudioTrack()?.audio?.setEnabled(enabled)
    }
}

@Stable
public data class VideoTrack(
    public override val streamId: String,
    public val video: io.getstream.webrtc.VideoTrack,
) : MediaTrack(streamId)

@Stable
public data class AudioTrack(
    public override val streamId: String,
    public val audio: io.getstream.webrtc.AudioTrack,
) : MediaTrack(streamId)
