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

import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import org.webrtc.VideoTrack
import java.util.UUID

@InternalStreamVideoApi
public val mockVideoTrackWrapper: io.getstream.video.android.core.model.TrackWrapper
    inline get() = io.getstream.video.android.core.model.TrackWrapper(
        UUID.randomUUID().toString(),
        VideoTrack(123)
    )

@InternalStreamVideoApi
public val mockParticipant: ParticipantState
    inline get() = mockUsers[0]

@InternalStreamVideoApi
public val mockParticipantList: List<ParticipantState>
    inline get() = mockUsers

@InternalStreamVideoApi
public val mockUsers: List<ParticipantState>
    inline get() = listOf<ParticipantState>()
