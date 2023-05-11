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

package io.getstream.video.android.mock

import android.content.Context
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.model.MediaTrack
import io.getstream.video.android.model.User
import org.webrtc.VideoTrack
import java.util.UUID

/**
 * Stream Video mock utils to initialize [StreamVideo] for writing unit testings or supporting Compose previews.
 */
public object StreamMockUtils {
    @PublishedApi
    internal lateinit var streamVideo: StreamVideo

    public fun initializeStreamVideo(context: Context) {
        if (::streamVideo.isInitialized.not()) {
            streamVideo = StreamVideoBuilder(
                context = context.applicationContext,
                apiKey = "stream-api-key",
                user = mockUsers.first(),
                token = "user-token"
            ).build()
        }
    }
}

/** Mock a [Call] that contains a mock user. */
public val mockCall: Call = Call(
    client = StreamMockUtils.streamVideo, type = "default", id = "123", user = mockUsers[0]
)

/** Mock a new [MediaTrack]. */
public val mockVideoMediaTrack: MediaTrack
    inline get() = io.getstream.video.android.core.model.VideoTrack(
        UUID.randomUUID().toString(), VideoTrack(123)
    )

/** Mock a list of [User]. */
public val mockUsers: List<io.getstream.video.android.model.User>
    inline get() = listOf(
        io.getstream.video.android.model.User(
            id = "filip_babic",
            name = "Filip",
            image = "https://avatars.githubusercontent.com/u/17215808?v=4",
        ),
        io.getstream.video.android.model.User(
            id = "jaewoong",
            name = "Jaewoong Eum",
            image = "https://ca.slack-edge.com/T02RM6X6B-U02HU1XR9LM-626fb91c334e-128",
        ),
        io.getstream.video.android.model.User(
            id = "toma_zdravkovic",
            name = "Toma Zdravkovic",
            image = "https://upload.wikimedia.org/wikipedia/commons/d/da/Toma_Zdravkovi%C4%87.jpg",
        ),
        io.getstream.video.android.model.User(
            id = "tyrone_bailey",
            name = "Tyrone Bailey",
            image = "https://getstream.io/chat/docs/sdk/avatars/jpg/Tyrone%20Bailey.jpg",
        ),
        io.getstream.video.android.model.User(
            id = "willard",
            name = "Willard Hessel",
            image = "https://getstream.io/chat/docs/sdk/avatars/jpg/Willard%20Hessel.jpg",
        ),
        io.getstream.video.android.model.User(
            id = "blanche",
            name = "Blanche Schoen",
            image = "https://getstream.io/chat/docs/sdk/avatars/jpg/Blanche%20Schoen.jpg",
        ),
    )

/** Mock a new list of [ParticipantState]. */
public val mockParticipantList: List<ParticipantState>
    inline get() {
        val participants = arrayListOf<ParticipantState>()
        mockCall.state.clearParticipants()
        mockUsers.forEach { user ->
            val sessionId = if (user == mockUsers.first()) {
                mockCall.sessionId ?: UUID.randomUUID().toString()
            } else {
                UUID.randomUUID().toString()
            }
            participants.add(
                ParticipantState(
                    initialUser = user,
                    sessionId = sessionId,
                    call = mockCall
                ).also { mockCall.state.updateParticipant(it) }
            )
        }
        return participants
    }

/** Mock a new [ParticipantState]. */
public val mockParticipant: ParticipantState
    inline get() = mockParticipantList[0]
