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

package io.getstream.video.android.core.utils

import io.getstream.video.android.model.User
import io.getstream.android.video.generated.models.UserResponse
import org.threeten.bp.OffsetDateTime

internal fun User.toResponse(): UserResponse {
    return UserResponse(
        id = id,
        role = role ?: "user",
        name = name,
        image = image,
        teams = teams ?: emptyList(),
        custom = custom ?: emptyMap(),
        createdAt = createdAt ?: OffsetDateTime.now(),
        updatedAt = updatedAt ?: OffsetDateTime.now(),
        deletedAt = deletedAt,
        // TODO: Implement these fields
        blockedUserIds = emptyList(),
        language = "",

    )
}
