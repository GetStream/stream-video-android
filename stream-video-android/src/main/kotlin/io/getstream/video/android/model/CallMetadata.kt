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

package io.getstream.video.android.model

import java.util.*

public data class CallMetadata(
    val cid: String,
    val id: String,
    val type: String,
    val createdByUserId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val recordingEnabled: Boolean,
    val broadcastingEnabled: Boolean,
    val users: Map<String, CallUser>,
    val members: Map<String, CallMember>,
    val extraData: Map<String, String>?,
) : java.io.Serializable

public fun CallMetadata.toInfo(): CallInfo = CallInfo(
    cid = cid,
    id = id,
    type = type,
    createdByUserId = createdByUserId,
    broadcastingEnabled = broadcastingEnabled,
    recordingEnabled = recordingEnabled,
    createdAt = Date(createdAt),
    updatedAt = Date(updatedAt)
)

public fun CallMetadata.toDetails(): CallDetails = CallDetails(
    members = users.toMembers(cid),
    memberUserIds = users.keys.toList(),
    broadcastingEnabled = broadcastingEnabled,
    recordingEnabled = recordingEnabled
)

public fun Map<String, CallUser>.toMembers(callCid: String): Map<String, CallMember> = mapValues { (_, value) ->
    CallMember(
        callCid = callCid,
        userId = value.id,
        role = value.role,
        createdAt = value.createdAt,
        updatedAt = value.updatedAt
    )
}
