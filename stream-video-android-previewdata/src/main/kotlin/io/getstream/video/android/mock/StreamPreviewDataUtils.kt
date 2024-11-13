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

package io.getstream.video.android.mock

import android.content.Context
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.model.MediaTrack
import io.getstream.video.android.model.User
import org.threeten.bp.OffsetDateTime
import org.webrtc.VideoTrack
import java.util.UUID

/**
 * Stream Video mock utils to initialize [StreamVideo] for writing unit testings or supporting Compose previews.
 */
public object StreamPreviewDataUtils {
    @PublishedApi
    internal lateinit var streamVideo: StreamVideo

    public fun initializeStreamVideo(context: Context) {
        if (::streamVideo.isInitialized.not()) {
            streamVideo = StreamVideoBuilder(
                context = context.applicationContext,
                apiKey = "stream-api-key",
                user = previewUsers.first(),
                token = "user-token",
            ).build()
        }
    }
}

/** Mock a [Call] that contains a mock user. */
public val previewCall: Call = Call(
    client = StreamPreviewDataUtils.streamVideo,
    type = "default",
    id = "123",
    user = previewUsers[0],
).apply {
    val participants = previewUsers.take(2).map { user ->
        val sessionId = UUID.randomUUID().toString()
        ParticipantState(
            initialUserId = user.id,
            sessionId = sessionId,
            call = this,
        )
    }
    state.upsertParticipants(participants)
}

/** Mock a new [MediaTrack]. */
public val previewVideoMediaTrack: MediaTrack
    inline get() = io.getstream.video.android.core.model.VideoTrack(
        UUID.randomUUID().toString(),
        VideoTrack(123),
    )

/** Mock a list of [User]. */
public val previewUsers: List<User>
    inline get() = listOf(
        User(
            id = "thierry",
            name = "Thierry",
            image = "https://avatars.githubusercontent.com/u/265409?v=4",
            role = "admin",
        ),
        User(
            id = "jaewoong",
            name = "Jaewoong Eum",
            image = "https://ca.slack-edge.com/T02RM6X6B-U02HU1XR9LM-626fb91c334e-128",
            role = "admin",
        ),
        User(
            id = "toma_zdravkovic",
            name = "Toma Zdravkovic",
            image = "https://upload.wikimedia.org/wikipedia/commons/d/da/Toma_Zdravkovi%C4%87.jpg",
            role = "admin",
        ),
        User(
            id = "tyrone_bailey",
            name = "Tyrone Bailey",
            image = "https://getstream.io/chat/docs/sdk/avatars/jpg/Tyrone%20Bailey.jpg",
            role = "admin",
        ),
        User(
            id = "willard",
            name = "Willard Hessel",
            image = "https://getstream.io/chat/docs/sdk/avatars/jpg/Willard%20Hessel.jpg",
            role = "admin",
        ),
        User(
            id = "blanche",
            name = "Blanche Schoen",
            image = "https://getstream.io/chat/docs/sdk/avatars/jpg/Blanche%20Schoen.jpg",
            role = "admin",
        ),
    )

/** Mock a new list of [ParticipantState]. */
public val previewParticipantsList: List<ParticipantState>
    inline get() {
        val participants = arrayListOf<ParticipantState>()
        previewCall.state.clearParticipants()
        previewUsers.forEach { user ->
            val sessionId = if (user == previewUsers.first()) {
                previewCall.sessionId ?: UUID.randomUUID().toString()
            } else {
                UUID.randomUUID().toString()
            }
            participants.add(
                ParticipantState(
                    initialUserId = user.id,
                    sessionId = sessionId,
                    call = previewCall,
                ).also { previewCall.state.updateParticipant(it) },
            )
        }
        return participants
    }

/** Mock a new list of [ParticipantState]. */
public val previewMemberListState: List<MemberState>
    inline get() {
        val participants = arrayListOf<MemberState>()
        previewCall.state.clearParticipants()
        previewUsers.forEach { user ->
            val sessionId = if (user == previewUsers.first()) {
                previewCall.sessionId
            } else {
                UUID.randomUUID().toString()
            }
            participants.add(
                MemberState(
                    user = user,
                    createdAt = OffsetDateTime.now(),
                    updatedAt = OffsetDateTime.now(),
                    custom = emptyMap(),
                    role = "admin",
                ),
            )
        }
        return participants
    }

/** Mock a new [ParticipantState]. */
public val previewParticipant: ParticipantState
    inline get() = previewParticipantsList[0]

/** Preview a new [MemberState]. */
public val previewMember: MemberState
    inline get() = previewMemberListState[0]

public val previewTwoMembers: List<MemberState>
    inline get() = previewMemberListState.take(2)

public val previewThreeMembers: List<MemberState>
    inline get() = previewMemberListState.take(3)
