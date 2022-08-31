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

package io.getstream.video.android.model

import org.webrtc.VideoTrack

public data class CallParticipant(
    public val id: String,
    public val role: String,
    public val name: String,
    public val profileImageURL: String?,
    public val isOnline: Boolean,
    public val hasVideo: Boolean,
    public val hasAudio: Boolean,
    public val track: VideoTrack?,
    public val trackSize: Pair<Float, Float>
)

// TODO - add mapping to/from SFU Participant
