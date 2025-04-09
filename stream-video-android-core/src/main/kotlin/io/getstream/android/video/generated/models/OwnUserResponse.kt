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

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport",
)

package io.getstream.android.video.generated.models

import com.squareup.moshi.Json
import kotlin.collections.List
import kotlin.collections.Map

/**
 *
 */

data class OwnUserResponse(
    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "id")
    val id: kotlin.String,

    @Json(name = "language")
    val language: kotlin.String,

    @Json(name = "role")
    val role: kotlin.String,

    @Json(name = "updated_at")
    val updatedAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "devices")
    val devices: kotlin.collections.List<io.getstream.android.video.generated.models.DeviceResponse> = emptyList(),

    @Json(name = "teams")
    val teams: kotlin.collections.List<kotlin.String> = emptyList(),

    @Json(name = "custom")
    val custom: kotlin.collections.Map<kotlin.String, Any?> = emptyMap(),

    @Json(name = "deactivated_at")
    val deactivatedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "deleted_at")
    val deletedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "image")
    val image: kotlin.String? = null,

    @Json(name = "last_active")
    val lastActive: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "name")
    val name: kotlin.String? = null,

    @Json(name = "revoke_tokens_issued_before")
    val revokeTokensIssuedBefore: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "blocked_user_ids")
    val blockedUserIds: kotlin.collections.List<kotlin.String>? = null,

    @Json(name = "push_preferences")
    val pushPreferences: io.getstream.android.video.generated.models.PushPreferences? = null,
)
