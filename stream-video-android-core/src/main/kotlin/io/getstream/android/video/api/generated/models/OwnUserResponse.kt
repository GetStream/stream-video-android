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

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package io.getstream.android.video.api.generated.models

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * 
 */
public data class OwnUserResponse (
    @Json(name = "created_at")
    public val createdAt: kotlin.time.Instant,

    @Json(name = "id")
    public val id: String,

    @Json(name = "language")
    public val language: String,

    @Json(name = "role")
    public val role: String,

    @Json(name = "updated_at")
    public val updatedAt: kotlin.time.Instant,

    @Json(name = "blocked_user_ids")
    public val blockedUserIds: List<String> = emptyList(),

    @Json(name = "devices")
    public val devices: List<io.getstream.android.video.api.generated.models.DeviceResponse> = emptyList(),

    @Json(name = "teams")
    public val teams: List<String> = emptyList(),

    @Json(name = "custom")
    public val custom: Map<String, Any?> = emptyMap(),

    @Json(name = "avg_response_time")
    public val avgResponseTime: Int? = null,

    @Json(name = "deactivated_at")
    public val deactivatedAt: kotlin.time.Instant? = null,

    @Json(name = "deleted_at")
    public val deletedAt: kotlin.time.Instant? = null,

    @Json(name = "image")
    public val image: String? = null,

    @Json(name = "last_active")
    public val lastActive: kotlin.time.Instant? = null,

    @Json(name = "name")
    public val name: String? = null,

    @Json(name = "push_preferences")
    public val pushPreferences: io.getstream.android.video.api.generated.models.PushPreferencesResponse? = null,

    @Json(name = "teams_role")
    public val teamsRole: Map<String, String>? = null
)
