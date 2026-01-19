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

package io.getstream.video.android.core.utils

import io.getstream.android.video.generated.models.CallRecording
import io.getstream.android.video.generated.models.CallStateResponseFields
import io.getstream.android.video.generated.models.EdgeResponse
import io.getstream.android.video.generated.models.MemberResponse
import io.getstream.android.video.generated.models.QueryCallMembersResponse
import io.getstream.android.video.generated.models.QueryCallsResponse
import io.getstream.android.video.generated.models.ReactionResponse
import io.getstream.android.video.generated.models.UserResponse
import io.getstream.video.android.core.MemberState
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.internal.InternalStreamVideoApi
import io.getstream.video.android.core.model.CallData
import io.getstream.video.android.core.model.CallRecordingData
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.model.CallUserState
import io.getstream.video.android.core.model.EdgeData
import io.getstream.video.android.core.model.Member
import io.getstream.video.android.core.model.QueriedCalls
import io.getstream.video.android.core.model.QueriedMembers
import io.getstream.video.android.core.model.ReactionData
import io.getstream.video.android.core.model.toCallInfo
import io.getstream.video.android.model.User
import io.getstream.video.android.model.User.Companion.isLocalUser
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
public fun MemberState.toCallUser(): CallUser {
    return CallUser(
        id = user.id,
        name = user.name,
        role = user.role,
        imageUrl = user.image,
        isLocalUser = user.isLocalUser(),
        teams = user.teams,
        state = null,
        createdAt = null,
        updatedAt = null,
    )
}

@JvmSynthetic
@InternalStreamVideoApi
public fun ParticipantState.toCallUser(): CallUser {
    return CallUser(
        id = userId.value,
        name = userNameOrId.value,
        imageUrl = image.value,
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
            online = true,
        ),
    )
}

@JvmSynthetic
internal fun UserResponse.toUser(): User {
    return User(
        id = id,
        role = role,
        name = name,
        image = image,
        teams = teams,
        custom = custom.mapValues { it.value.toString() },
    )
}

@JvmSynthetic
internal fun QueryCallsResponse.toQueriedCalls(): QueriedCalls {
    return QueriedCalls(
        original = this,
        calls = calls.toCallData(),
        next = next,
        prev = prev,
    )
}

@JvmSynthetic
internal fun QueryCallMembersResponse.toQueriedMembers(): QueriedMembers {
    return QueriedMembers(
        members = members.map { it.toMember() },
        next = next,
        prev = prev,
    )
}

@JvmSynthetic
internal fun MemberResponse.toMember(): Member {
    return Member(
        createdAt = createdAt,
        custom = custom,
        updatedAt = updatedAt,
        user = user.toUser(),
        deletedAt = deletedAt,
        userId = userId,
        role = role,
    )
}

@JvmSynthetic
internal fun List<CallStateResponseFields>.toCallData(): List<CallData> {
    return map { it.toCallData() }
}

@JvmSynthetic
internal fun CallStateResponseFields.toCallData(): CallData {
    return CallData(
        blockedUsersIds = call.blockedUserIds,
        call = call.toCallInfo(),
        members = members.map { it.toCallUser() },
        ownMembership = membership?.toCallUser(),
    )
}

@JvmSynthetic
internal fun CallRecording.toRecording(): CallRecordingData {
    return CallRecordingData(
        fileName = filename,
        url = url,
        start = startTime.toEpochSecond() * 1000,
        end = endTime.toEpochSecond() * 1000,
    )
}

@JvmSynthetic
internal fun ReactionResponse.toReaction(): ReactionData {
    return ReactionData(
        type = type,
        user = user.toUser(),
        emoji = emojiCode,
        custom = custom,
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
        red = red,
    )
}
