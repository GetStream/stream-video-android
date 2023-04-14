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

import android.content.Context
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.model.User
import org.webrtc.VideoTrack
import java.util.UUID

@InternalStreamVideoApi
public object MockUtils {
    @PublishedApi
    internal lateinit var streamVideo: StreamVideo

    public fun initializeStreamVideo(context: Context) {
        if (::streamVideo.isInitialized.not()) {
            streamVideo = StreamVideoBuilder(
                context = context.applicationContext,
                apiKey = "stream-api-key",
                user = mockUsers.first(),
                userToken = "user-token"
            ).build()
        }
    }
}

@InternalStreamVideoApi
public val mockCall: Call
    inline get() = Call(
        client = MockUtils.streamVideo, type = "default", id = "123", user = mockUsers[0]
    )

@InternalStreamVideoApi
public val mockVideoTrackWrapper: io.getstream.video.android.core.model.TrackWrapper
    inline get() = io.getstream.video.android.core.model.TrackWrapper(
        UUID.randomUUID().toString(), VideoTrack(123)
    )

@InternalStreamVideoApi
public val mockParticipant: ParticipantState
    inline get() = mockParticipants[0]

@InternalStreamVideoApi
public val mockParticipantList: List<ParticipantState>
    inline get() = mockParticipants

@InternalStreamVideoApi
public val mockUsers: List<User>
    inline get() = listOf(
        User(
            id = "filip_babic",
            name = "Filip",
            image = "https://avatars.githubusercontent.com/u/17215808?v=4",
            isOnline = true
        ),
        User(
            id = "jaewoong",
            name = "Jaewoong Eum",
            image = "https://ca.slack-edge.com/T02RM6X6B-U02HU1XR9LM-626fb91c334e-128",
            isOnline = true
        ),
        User(
            id = "toma_zdravkovic",
            name = "Toma Zdravkovic",
            image = "https://upload.wikimedia.org/wikipedia/commons/d/da/Toma_Zdravkovi%C4%87.jpg",
            isOnline = true
        ),
        User(
            id = "tyrone_bailey",
            name = "Tyrone Bailey",
            image = "https://getstream.io/chat/docs/sdk/avatars/jpg/Tyrone%20Bailey.jpg",
            isOnline = true
        ),
        User(
            id = "willard",
            name = "Willard Hessel",
            image = "https://getstream.io/chat/docs/sdk/avatars/jpg/Willard%20Hessel.jpg",
            isOnline = true
        ),
        User(
            id = "blanche",
            name = "Blanche Schoen",
            image = "https://getstream.io/chat/docs/sdk/avatars/jpg/Blanche%20Schoen.jpg",
            isOnline = true
        ),
    )

@InternalStreamVideoApi
public val mockParticipants: List<ParticipantState>
    inline get() {
        val participants = arrayListOf<ParticipantState>()
        mockUsers.forEach {
            participants.add(
                ParticipantState(
                    initialUser = it,
                    sessionId = UUID.randomUUID().toString(),
                    isLocal = it.id == mockUsers.first().id,
                    call = mockCall
                )
            )
        }
        return participants
    }
