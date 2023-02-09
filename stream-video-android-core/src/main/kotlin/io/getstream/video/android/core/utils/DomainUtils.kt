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

import io.getstream.video.android.core.model.CallMetadata
import io.getstream.video.android.core.model.CallUser
import io.getstream.video.android.core.model.StreamCallKind
import io.getstream.video.android.core.model.toCallUsers
import org.openapitools.client.models.GetOrCreateCallResponse
import org.openapitools.client.models.UserResponse
import java.util.Date

internal fun GetOrCreateCallResponse.toCall(kind: StreamCallKind): CallMetadata {
    return with(call) {
        CallMetadata(
            cid = cid!!,
            id = id!!,
            type = type!!,
            kind = kind,
            createdByUserId = createdBy.id!!,
            createdAt = createdAt.toEpochSecond(),
            updatedAt = updatedAt.toEpochSecond(),
            recordingEnabled = settings.recording.enabled ?: false,
            broadcastingEnabled = settings.broadcasting.enabled ?: false,
            users = members?.toCallUsers() ?: emptyMap(),
            extraData = emptyMap()
        )
    }
}

internal fun UserResponse.toCallUser(): CallUser {
    return CallUser(
        id = id ?: "",
        name = name ?: "",
        role = role ?: "",
        imageUrl = image ?: "",
        teams = teams ?: emptyList(),
        state = null,
        createdAt = Date.from(createdAt.toInstant()),
        updatedAt = Date.from(updatedAt.toInstant()),
    )
}
