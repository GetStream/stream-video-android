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

package io.getstream.video.android.core.model

import androidx.compose.runtime.Stable
import io.getstream.video.android.core.utils.toCallUser
import io.getstream.android.video.generated.models.CallResponse
import io.getstream.android.video.generated.models.MemberResponse
import io.getstream.android.video.generated.models.OwnCapability
import java.io.Serializable
import java.util.Date

@Stable
public data class CallUser(
    val id: String,
    val name: String? = null,
    val role: String? = null,
    val imageUrl: String? = null,
    val teams: List<String>? = null,
    val state: CallUserState?,
    val createdAt: Date?,
    val updatedAt: Date?,
) : Serializable

@Stable
public data class CallUserState(
    val trackIdPrefix: String,
    val online: Boolean,
    val audio: Boolean,
    val video: Boolean,
)

@Stable
public data class CallMember(
    val callCid: String,
    val role: String,
    val userId: String,
    val createdAt: Date?,
    val updatedAt: Date?,
) : Serializable

@Stable
public data class CallInfo(
    val cid: String,
    val type: String,
    val id: String,
    val createdByUserId: String,
    val broadcastingEnabled: Boolean,
    val recordingEnabled: Boolean,
    val createdAt: Date?,
    val updatedAt: Date?,
//    val callEgress: CallEgress,
    val custom: Map<String, Any?>,
) : Serializable

@Stable
public data class CallDetails(
    val memberUserIds: List<String>,
    val members: Map<String, CallUser>,
    val ownCapabilities: List<OwnCapability>,
) : Serializable

@Stable
public data class CallEgress(
    val broadcastEgress: String,
    val recordEgress: String,
)

internal fun List<MemberResponse>.toCallUsers(): Map<String, CallUser> =
    associate { it.userId to it.toCallUser() }

internal fun CallResponse.toCallInfo(): CallInfo {
    return CallInfo(
        cid = cid,
        id = id,
        type = type,
        createdByUserId = createdBy.id,
        broadcastingEnabled = settings.broadcasting.enabled,
        recordingEnabled = settings.recording.audioOnly, // TODO - how do we know if it's enabled or not
        createdAt = Date(createdAt.toEpochSecond() * 1000L),
        updatedAt = Date(updatedAt.toEpochSecond() * 1000L),
//        callEgress = CallEgress(
//            broadcastEgress = broadcastEgress,
//            recordEgress = recordEgress
//        ),
        custom = custom,
    )
}

/**
 * Merges [CallUser] maps to absorb as many non-null and non-empty data from both collections.
 */
public infix fun Map<String, CallUser>.merge(that: Map<String, CallUser>): Map<String, CallUser> {
    val new = this - that.keys
    val merged = this.map { (userId, user) ->
        userId to user.merge(that[userId])
    }.toMap()
    return merged + new
}

/**
 * Merges [that] [CallUser] into [this] map.
 */
public infix fun Map<String, CallUser>.merge(that: CallUser): Map<String, CallUser> =
    when (contains(that.id)) {
        true -> this.map { (userId, user) ->
            userId to user.merge(that)
        }.toMap()

        else -> this.toMutableMap().also {
            it[that.id] = that
        }
    }

/**
 * Merges [that] into [this] CallUser to absorb as much data as possible from both instances.
 */
public infix fun CallUser.merge(that: CallUser?): CallUser = when (that) {
    null -> this
    else -> copy(
        id = that.id.ifEmpty { this.id },
        name = that.name.takeUnless { it.isNullOrBlank() } ?: this.name,
        role = that.role.takeUnless { it.isNullOrBlank() } ?: this.role,
        imageUrl = that.imageUrl.takeUnless { it.isNullOrBlank() } ?: this.imageUrl,
        state = that.state merge this.state,
        createdAt = that.createdAt ?: this.createdAt,
        updatedAt = that.updatedAt ?: this.updatedAt,
        teams = (that.teams.orEmpty() + this.teams.orEmpty()).distinct(),
    )
}

/**
 * Merges [that] into [this] CallUserState to absorb as much data as possible from both instances.
 */
public infix fun CallUserState?.merge(that: CallUserState?): CallUserState? = when {
    this != null && that != null -> that.copy(
        trackIdPrefix = that.trackIdPrefix.ifEmpty { this.trackIdPrefix },
    )

    this == null -> that
    else -> this
}
