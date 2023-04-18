/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

package org.openapitools.client.models

import com.squareup.moshi.Json

/**
 *
 *
 * @param createdAt
 * @param custom
 * @param devices
 * @param id
 * @param role
 * @param teams
 * @param updatedAt
 * @param deletedAt
 * @param image
 * @param name
 */

data class OwnUserResponse(

    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "custom")
    val custom: kotlin.collections.Map<kotlin.String, kotlin.Any>,

    @Json(name = "devices")
    val devices: kotlin.collections.List<Device>,

    @Json(name = "id")
    val id: kotlin.String,

    @Json(name = "role")
    val role: kotlin.String,

    @Json(name = "teams")
    val teams: kotlin.collections.List<kotlin.String>,

    @Json(name = "updated_at")
    val updatedAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "deleted_at")
    val deletedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "image")
    val image: kotlin.String? = null,

    @Json(name = "name")
    val name: kotlin.String? = null

)
