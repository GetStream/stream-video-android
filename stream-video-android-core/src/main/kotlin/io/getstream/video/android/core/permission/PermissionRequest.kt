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

package io.getstream.video.android.core.permission

import io.getstream.result.Result
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.utils.toUser
import io.getstream.video.android.model.User
import io.getstream.android.video.generated.models.PermissionRequestEvent
import io.getstream.android.video.generated.models.UpdateUserPermissionsResponse
import org.threeten.bp.Clock
import org.threeten.bp.OffsetDateTime

data class PermissionRequest(
    val call: Call,
    val user: User,
    val createdAt: OffsetDateTime,
    val permissions: List<String>,
    var grantedAt: OffsetDateTime? = null,
    var rejectedAt: OffsetDateTime? = null,
) {
    constructor(call: Call, event: PermissionRequestEvent) : this(
        call = call,
        user = event.user.toUser(),
        createdAt = event.createdAt,
        permissions = event.permissions,
    )

    suspend fun grant(): Result<UpdateUserPermissionsResponse> {
        val result = call.grantPermissions(userId = user.id, permissions = permissions)
        result.onSuccess {
            grantedAt = OffsetDateTime.now(Clock.systemUTC())
        }
        return result
    }

    fun reject() {
        rejectedAt = OffsetDateTime.now(Clock.systemUTC())
    }
}
