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
public data class CallResponse (
    @Json(name = "backstage")
    public val backstage: Boolean,

    @Json(name = "captioning")
    public val captioning: Boolean,

    @Json(name = "cid")
    public val cid: String,

    @Json(name = "created_at")
    public val createdAt: kotlin.time.Instant,

    @Json(name = "current_session_id")
    public val currentSessionId: String,

    @Json(name = "id")
    public val id: String,

    @Json(name = "recording")
    public val recording: Boolean,

    @Json(name = "transcribing")
    public val transcribing: Boolean,

    @Json(name = "translating")
    public val translating: Boolean,

    @Json(name = "type")
    public val type: String,

    @Json(name = "updated_at")
    public val updatedAt: kotlin.time.Instant,

    @Json(name = "blocked_user_ids")
    public val blockedUserIds: List<String> = emptyList(),

    @Json(name = "created_by")
    public val createdBy: io.getstream.android.video.api.generated.models.UserResponse,

    @Json(name = "custom")
    public val custom: Map<String, Any?> = emptyMap(),

    @Json(name = "egress")
    public val egress: io.getstream.android.video.api.generated.models.EgressResponse,

    @Json(name = "ingress")
    public val ingress: io.getstream.android.video.api.generated.models.CallIngressResponse,

    @Json(name = "settings")
    public val settings: io.getstream.android.video.api.generated.models.CallSettingsResponse,

    @Json(name = "channel_cid")
    public val channelCid: String? = null,

    @Json(name = "ended_at")
    public val endedAt: kotlin.time.Instant? = null,

    @Json(name = "join_ahead_time_seconds")
    public val joinAheadTimeSeconds: Int? = null,

    @Json(name = "routing_number")
    public val routingNumber: String? = null,

    @Json(name = "starts_at")
    public val startsAt: kotlin.time.Instant? = null,

    @Json(name = "team")
    public val team: String? = null,

    @Json(name = "session")
    public val session: io.getstream.android.video.api.generated.models.CallSessionResponse? = null,

    @Json(name = "thumbnails")
    public val thumbnails: io.getstream.android.video.api.generated.models.ThumbnailResponse? = null
)
