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

package io.getstream.video.android.common.util

import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.model.CallParticipantState
import org.webrtc.VideoTrack
import stream.video.sfu.models.ConnectionQuality
import stream.video.sfu.models.TrackType
import java.util.UUID

@InternalStreamVideoApi
public val mockVideoTrack: io.getstream.video.android.core.model.VideoTrack
    inline get() = io.getstream.video.android.core.model.VideoTrack(
        UUID.randomUUID().toString(),
        VideoTrack(123)
    )

@InternalStreamVideoApi
public val mockParticipant: CallParticipantState
    inline get() = mockUsers[0]

@InternalStreamVideoApi
public val mockParticipantList: List<CallParticipantState>
    inline get() = mockUsers

@InternalStreamVideoApi
public val mockUsers: List<CallParticipantState>
    inline get() = listOf(
        CallParticipantState(
            id = "filip_babic",
            name = "Filip",
            profileImageURL = "https://avatars.githubusercontent.com/u/17215808?v=4",
            idPrefix = "",
            role = "",
            sessionId = "",
            videoTrack = mockVideoTrack,
            isLocal = true,
            isSpeaking = true,
            connectionQuality = ConnectionQuality.CONNECTION_QUALITY_GOOD,
            publishedTracks = setOf(TrackType.TRACK_TYPE_VIDEO, TrackType.TRACK_TYPE_AUDIO)
        ),
        CallParticipantState(
            id = "jaewoong",
            name = "Jaewoong Eum",
            profileImageURL = "https://ca.slack-edge.com/T02RM6X6B-U02HU1XR9LM-626fb91c334e-128",
            idPrefix = "",
            role = "",
            sessionId = "",
            videoTrack = mockVideoTrack,
            isLocal = false,
            isSpeaking = true,
            connectionQuality = ConnectionQuality.CONNECTION_QUALITY_GOOD,
            publishedTracks = setOf(TrackType.TRACK_TYPE_VIDEO, TrackType.TRACK_TYPE_AUDIO)
        ),
        CallParticipantState(
            id = "toma_zdravkovic",
            name = "Toma Zdravkovic",
            profileImageURL = "https://upload.wikimedia.org/wikipedia/commons/d/da/Toma_Zdravkovi%C4%87.jpg",
            idPrefix = "",
            role = "",
            sessionId = "",
            videoTrack = mockVideoTrack,
            isLocal = false,
            isSpeaking = false,
            connectionQuality = ConnectionQuality.CONNECTION_QUALITY_GOOD,
            publishedTracks = setOf(TrackType.TRACK_TYPE_VIDEO, TrackType.TRACK_TYPE_AUDIO)
        ),
        CallParticipantState(
            id = "tyrone_bailey",
            name = "Tyrone Bailey",
            profileImageURL = "https://getstream.io/chat/docs/sdk/avatars/jpg/Tyrone%20Bailey.jpg",
            idPrefix = "",
            role = "",
            sessionId = "",
            videoTrack = mockVideoTrack,
            isLocal = false,
            isSpeaking = false,
            connectionQuality = ConnectionQuality.CONNECTION_QUALITY_GOOD,
            publishedTracks = setOf(TrackType.TRACK_TYPE_VIDEO)
        )
    )
