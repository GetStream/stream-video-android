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

package io.getstream.video.android.core.model

import androidx.compose.runtime.Stable

/**
 * How this client receives the call's media, as decided by the backend when the call is joined.
 *
 * Beta: HLS delivery is enabled per call type by Stream and may change.
 */
@Stable
public sealed interface PlaybackType {

    /** Media comes from the SFU over WebRTC. The default, and the only mode for publishers. */
    public data object WebRtc : PlaybackType

    /**
     * Media comes from the call's HLS broadcast at [playlistUrl]. There is no SFU connection, so
     * participants have no video or audio tracks and the client cannot publish.
     */
    public data class Hls(val playlistUrl: String) : PlaybackType
}
