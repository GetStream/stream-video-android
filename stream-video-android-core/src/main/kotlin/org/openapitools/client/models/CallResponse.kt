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
 * Represents a call
 *
 * @param createdAt Date/time of creation
 * @param createdBy * @param settings * @param updatedAt Date/time of the last update
 * @param broadcastEgress * @param cid The unique identifier for a call (<type>:<id>)
 * @param custom * @param endedAt Date/time of end
 * @param id Call ID
 * @param ownCapabilities * @param recordEgress * @param team * @param type The type of call
 */

internal data class CallResponse(

    /* Date/time of creation */
    @Json(name = "created_at")
    val createdAt: java.time.OffsetDateTime,

    @Json(name = "created_by")
    val createdBy: UserResponse,

    @Json(name = "settings")
    val settings: CallSettingsResponse,

    /* Date/time of the last update */
    @Json(name = "updated_at")
    val updatedAt: java.time.OffsetDateTime,

    @Json(name = "broadcast_egress")
    val broadcastEgress: kotlin.String? = null,

    /* The unique identifier for a call (<type>:<id>) */
    @Json(name = "cid")
    val cid: kotlin.String? = null,

    @Json(name = "custom")
    val custom: kotlin.collections.Map<kotlin.String, kotlin.Any>? = null,

    /* Date/time of end */
    @Json(name = "ended_at")
    val endedAt: java.time.OffsetDateTime? = null,

    /* Call ID */
    @Json(name = "id")
    val id: kotlin.String? = null,

    @Json(name = "own_capabilities")
    val ownCapabilities: kotlin.collections.List<kotlin.String>? = null,

    @Json(name = "record_egress")
    val recordEgress: kotlin.String? = null,

    @Json(name = "team")
    val team: kotlin.String? = null,

    /* The type of call */
    @Json(name = "type")
    val type: kotlin.String? = null

)
