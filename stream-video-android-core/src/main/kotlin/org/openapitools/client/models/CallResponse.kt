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
    "UnusedImport"
)

package org.openapitools.client.models

import org.openapitools.client.models.CallIngressResponse
import org.openapitools.client.models.CallSessionResponse
import org.openapitools.client.models.CallSettingsResponse
import org.openapitools.client.models.EgressResponse
import org.openapitools.client.models.ThumbnailResponse
import org.openapitools.client.models.UserResponse




import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.openapitools.client.infrastructure.Serializer

/**
 * Represents a call
 *
 * @param backstage
 * @param blockedUserIds
 * @param cid The unique identifier for a call (<type>:<id>)
 * @param createdAt Date/time of creation
 * @param createdBy
 * @param currentSessionId
 * @param custom Custom data for this object
 * @param egress
 * @param id Call ID
 * @param ingress
 * @param recording
 * @param settings
 * @param transcribing
 * @param type The type of call
 * @param updatedAt Date/time of the last update
 * @param endedAt Date/time when the call ended
 * @param session
 * @param startsAt Date/time when the call will start
 * @param team
 * @param thumbnails
 */


data class CallResponse (

    @Json(name = "backstage")
    val backstage: kotlin.Boolean,

    @Json(name = "blocked_user_ids")
    val blockedUserIds: kotlin.collections.List<kotlin.String>,

    /* The unique identifier for a call (<type>:<id>) */
    @Json(name = "cid")
    val cid: kotlin.String,

    /* Date/time of creation */
    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "created_by")
    val createdBy: UserResponse,

    @Json(name = "current_session_id")
    val currentSessionId: kotlin.String,

    /* Custom data for this object */
    @Json(name = "custom")
    val custom: kotlin.collections.Map<kotlin.String, kotlin.Any?> = emptyMap(),

    @Json(name = "egress")
    val egress: EgressResponse,

    /* Call ID */
    @Json(name = "id")
    val id: kotlin.String,

    @Json(name = "ingress")
    val ingress: CallIngressResponse,

    @Json(name = "recording")
    val recording: kotlin.Boolean,

    @Json(name = "settings")
    val settings: CallSettingsResponse,

    @Json(name = "captioning")
    val captioning: kotlin.Boolean,

    @Json(name = "transcribing")
    val transcribing: kotlin.Boolean,

    /* The type of call */
    @Json(name = "type")
    val type: kotlin.String,

    /* Date/time of the last update */
    @Json(name = "updated_at")
    val updatedAt: org.threeten.bp.OffsetDateTime,

    /* Date/time when the call ended */
    @Json(name = "ended_at")
    val endedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "session")
    val session: CallSessionResponse? = null,

    /* Date/time when the call will start */
    @Json(name = "starts_at")
    val startsAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "team")
    val team: kotlin.String? = null,

    @Json(name = "thumbnails")
    val thumbnails: ThumbnailResponse? = null

)
