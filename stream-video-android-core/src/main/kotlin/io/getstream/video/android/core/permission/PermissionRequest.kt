package io.getstream.video.android.core.permission

import com.squareup.moshi.Json
import io.getstream.result.Result
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.model.User
import io.getstream.video.android.core.utils.toUser
import org.openapitools.client.models.PermissionRequestEvent
import org.openapitools.client.models.UpdateUserPermissionsResponse
import org.openapitools.client.models.UserResponse
import org.threeten.bp.Clock
import org.threeten.bp.OffsetDateTime

data class PermissionRequest(
    val call: Call,
    val user: User,
    val createdAt: org.threeten.bp.OffsetDateTime,
    val permissions: List<String>,
    var grantedAt: org.threeten.bp.OffsetDateTime? = null,
    var rejectedAt: org.threeten.bp.OffsetDateTime? = null,
) {
    constructor(call: Call, event: PermissionRequestEvent) : this(
            call = call,
            user = event.user.toUser(),
            createdAt = event.createdAt,
            permissions = event.permissions,
        )


    suspend fun grant(): Result<UpdateUserPermissionsResponse> {
        val result = call.grantPermissions(userId=user.id, permissions=permissions)
        result.onSuccess {
            grantedAt = OffsetDateTime.now(Clock.systemUTC())
        }
        return result
    }

    fun reject() {
        rejectedAt = OffsetDateTime.now(Clock.systemUTC())
    }
}
