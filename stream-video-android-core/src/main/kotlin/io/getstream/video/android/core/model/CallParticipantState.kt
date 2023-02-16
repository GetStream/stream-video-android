/*
 * Copyright (c) 2014-2022 Stream.io Inc. All rights reserved.
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

import stream.video.sfu.models.ConnectionQuality
import stream.video.sfu.models.TrackType

public data class CallParticipantState(
    public val id: String,
    public val role: String,
    public val name: String,
    public val profileImageURL: String?,
    public var sessionId: String,
    public val idPrefix: String,
    public val isLocal: Boolean = false,
    public val connectionQuality: ConnectionQuality = ConnectionQuality.CONNECTION_QUALITY_UNSPECIFIED,
    public var isOnline: Boolean = false,
    public var videoTrack: VideoTrack? = null,
    public var screenSharingTrack: VideoTrack? = null,
    public var publishedTracks: Set<TrackType> = emptySet(),
    public var videoTrackSize: Pair<Int, Int> = Pair(0, 0),
    public var audioLevel: Float = 0f,
    public var isSpeaking: Boolean = false
) {
    public val hasVideo: Boolean
        get() = TrackType.TRACK_TYPE_VIDEO in publishedTracks

    public val hasAudio: Boolean
        get() = TrackType.TRACK_TYPE_AUDIO in publishedTracks

    public val hasScreenShare: Boolean
        get() = TrackType.TRACK_TYPE_SCREEN_SHARE in publishedTracks && screenSharingTrack != null

    public val hasScreenShareAudio: Boolean
        get() = TrackType.TRACK_TYPE_SCREEN_SHARE_AUDIO in publishedTracks
}

public fun CallParticipantState.toUser(): User {
    return User(
        id = id,
        role = role,
        name = name,
        token = "",
        imageUrl = profileImageURL,
        teams = emptyList(),
        extraData = emptyMap()
    )
}
