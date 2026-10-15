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
 * Represents a call
 */
data class CallResponse (
    @Json(name = "backstage")
    val backstage: Boolean,

    @Json(name = "captioning")
    val captioning: Boolean,

    @Json(name = "cid")
    val cid: String,

    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "current_session_id")
    val currentSessionId: String,

    @Json(name = "id")
    val id: String,

    @Json(name = "recording")
    val recording: Boolean,

    @Json(name = "transcribing")
    val transcribing: Boolean,

    @Json(name = "translating")
    val translating: Boolean,

    @Json(name = "type")
    val type: String,

    @Json(name = "updated_at")
    val updatedAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "blocked_user_ids")
    val blockedUserIds: List<String> = emptyList(),

    @Json(name = "created_by")
    val createdBy: io.getstream.android.video.api.generated.models.UserResponse,

    @Json(name = "custom")
    val custom: Map<String, Any?> = emptyMap(),

    @Json(name = "egress")
    val egress: io.getstream.android.video.api.generated.models.EgressResponse,

    @Json(name = "ingress")
    val ingress: io.getstream.android.video.api.generated.models.CallIngressResponse,

    @Json(name = "settings")
    val settings: io.getstream.android.video.api.generated.models.CallSettingsResponse,

    @Json(name = "channel_cid")
    val channelCid: String? = null,

    @Json(name = "ended_at")
    val endedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "join_ahead_time_seconds")
    val joinAheadTimeSeconds: Int? = null,

    @Json(name = "routing_number")
    val routingNumber: String? = null,

    @Json(name = "starts_at")
    val startsAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "team")
    val team: String? = null,

    @Json(name = "session")
    val session: io.getstream.android.video.api.generated.models.CallSessionResponse? = null,

    @Json(name = "thumbnails")
    val thumbnails: io.getstream.android.video.api.generated.models.ThumbnailResponse? = null
)
