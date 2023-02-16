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

package io.getstream.video.android.core.utils

import io.getstream.video.android.core.model.CallDetails
import io.getstream.video.android.core.model.CallEgress
import io.getstream.video.android.core.model.CallMetadata
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.model.CallUserState
import io.getstream.video.android.core.model.StreamCallKind
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.model.toCallUsers
import org.openapitools.client.models.GetOrCreateCallResponse
import org.openapitools.client.models.MemberResponse
import org.openapitools.client.models.UserResponse
import stream.video.sfu.models.Participant
import stream.video.sfu.models.TrackType
import java.util.*

internal fun GetOrCreateCallResponse.toCall(kind: StreamCallKind): CallMetadata {
    return with(call) {
        CallMetadata(
            cid = cid,
            id = id,
            type = type,
            kind = kind,
            createdByUserId = createdBy.id,
            createdAt = createdAt.toEpochSecond(),
            updatedAt = updatedAt.toEpochSecond(),
            recordingEnabled = settings.recording.audioOnly, // TODO
            broadcastingEnabled = settings.broadcasting.enabled,
            users = members.toCallUsers(),
            callEgress = CallEgress(
                broadcastEgress = broadcastEgress,
                recordEgress = recordEgress
            ),
            callDetails = CallDetails(
                members = members.map { it.toCallUser() }.associateBy { it.id },
                memberUserIds = members.map { it.userId },
                ownCapabilities = ownCapabilities
            ),
            custom = custom
        )
    }
}

internal fun MemberResponse.toCallUser(): CallUser {
    return CallUser(
        id = userId,
        name = user.name ?: "",
        role = role,
        imageUrl = user.image ?: "",
        teams = user.teams ?: emptyList(),
        state = null,
        createdAt = Date.from(createdAt.toInstant()),
        updatedAt = Date.from(updatedAt.toInstant()),
    )
}

internal fun Participant.toPartialUser(): CallUser {
    return CallUser(
        id = user_id,
        role = "",
        name = "",
        imageUrl = "",
        createdAt = null,
        updatedAt = null,
        teams = emptyList(),
        state = CallUserState(
            trackIdPrefix = track_lookup_prefix,
            audio = TrackType.TRACK_TYPE_AUDIO in published_tracks,
            video = TrackType.TRACK_TYPE_VIDEO in published_tracks,
            online = true
        )
    )
}

internal fun UserResponse.toUser(): User {
    return User(
        id = id,
        role = role,
        name = name ?: "",
        token = "",
        imageUrl = image,
        teams = teams ?: emptyList(),
        extraData = custom.mapValues { it.value.toString() }
    )
}
