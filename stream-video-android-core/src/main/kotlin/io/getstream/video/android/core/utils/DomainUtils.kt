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

import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.model.CallData
import io.getstream.video.android.core.model.CallRecordingData
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.model.CallUserState
import io.getstream.video.android.core.model.EdgeData
import io.getstream.video.android.core.model.QueriedCalls
import io.getstream.video.android.core.model.ReactionData
import io.getstream.video.android.core.model.toCallInfo
import org.openapitools.client.models.CallRecording
import org.openapitools.client.models.CallStateResponseFields
import org.openapitools.client.models.EdgeResponse
import org.openapitools.client.models.MemberResponse
import org.openapitools.client.models.QueryCallsResponse
import org.openapitools.client.models.ReactionResponse
import org.openapitools.client.models.UserResponse
import stream.video.sfu.models.Participant
import stream.video.sfu.models.TrackType
import java.util.Date

@JvmSynthetic
internal fun MemberResponse.toCallUser(): CallUser {
    return CallUser(
        id = userId,
        name = user.name.orEmpty(),
        imageUrl = user.image.orEmpty(),
        teams = user.teams ?: emptyList(),
        role = user.role,
        state = null,
        createdAt = Date(createdAt.toEpochSecond()),
        updatedAt = Date(updatedAt.toEpochSecond()),
    )
}

@JvmSynthetic
@InternalStreamVideoApi
public fun ParticipantState.toCallUser(): CallUser {
    return CallUser(
        id = initialUser.id,
        name = initialUser.name,
        imageUrl = initialUser.image,
        teams = initialUser.teams,
        role = initialUser.role,
        state = null,
        createdAt = null,
        updatedAt = null,
    )
}

@JvmSynthetic
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

@JvmSynthetic
internal fun UserResponse.toUser(): io.getstream.video.android.model.User {
    return io.getstream.video.android.model.User(
        id = id,
        role = role,
        name = name ?: "",
        image = image ?: "",
        teams = teams ?: emptyList(),
        custom = custom.mapValues { it.value.toString() }
    )
}

@JvmSynthetic
internal fun QueryCallsResponse.toQueriedCalls(): QueriedCalls {
    return QueriedCalls(
        calls = calls.toCallData(),
        next = next,
        prev = prev
    )
}

@JvmSynthetic
internal fun List<CallStateResponseFields>.toCallData(): List<CallData> {
    return map { it.toCallData() }
}

@JvmSynthetic
internal fun CallStateResponseFields.toCallData(): CallData {
    return CallData(
        blockedUsers = blockedUsers.map { it.toUser() },
        call = call.toCallInfo(),
        members = members.map { it.toCallUser() },
        ownMembership = membership?.toCallUser()
    )
}

@JvmSynthetic
internal fun CallRecording.toRecording(): CallRecordingData {
    return CallRecordingData(
        fileName = filename,
        url = url,
        start = startTime.toEpochSecond() * 1000,
        end = endTime.toEpochSecond() * 1000
    )
}

@JvmSynthetic
internal fun ReactionResponse.toReaction(): ReactionData {
    return ReactionData(
        type = type,
        user = user.toUser(),
        emoji = emojiCode,
        custom = custom
    )
}

@JvmSynthetic
internal fun EdgeResponse.toEdge(): EdgeData {
    return EdgeData(
        id = id,
        latencyTestUrl = latencyTestUrl,
        latitude = latitude,
        longitude = longitude,
        green = green,
        yellow = yellow,
        red = red
    )
}
