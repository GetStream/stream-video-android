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

package io.getstream.video.android.mock

import android.content.Context
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.model.MediaTrack
import io.getstream.video.android.model.User
import io.getstream.webrtc.VideoTrack
import org.threeten.bp.OffsetDateTime
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
            scope = this.state.scope,
            callActions = this.state.callActions,
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
            id = "leia_organa",
            name = "Leia Organa",
            image = "https://vignette.wikia.nocookie.net/starwars/images/f/fc/Leia_Organa_TLJ.png",
            role = "admin",
        ),
        User(
            id = "han_solo",
            name = "Han Solo",
            image = "https://vignette.wikia.nocookie.net/starwars/images/e/e2/TFAHanSolo.png",
            role = "admin",
        ),
        User(
            id = "lando_calrissian",
            name = "Lando Calrissian",
            image = "https://vignette.wikia.nocookie.net/starwars/images/8/8f/Lando_ROTJ.png",
            role = "admin",
        ),
        User(
            id = "chewbacca",
            name = "Chewbacca",
            image = "https://vignette.wikia.nocookie.net/starwars/images/4/48/Chewbacca_TLJ.png",
            role = "admin",
        ),
        User(
            id = "c-3po",
            name = "C-3PO",
            image = "https://vignette.wikia.nocookie.net/starwars/images/3/3f/C-3PO_TLJ_Card_Trader_Award_Card.png",
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
                    scope = previewCall.state.scope,
                    callActions = previewCall.state.callActions,
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
            participants.add(
                MemberState(
                    user = user,
                    createdAt = OffsetDateTime.MIN,
                    updatedAt = OffsetDateTime.MIN,
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
