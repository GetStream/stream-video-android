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
public data class PublisherSession (
    @Json(name = "duration_min")
    public val durationMin: Double,

    @Json(name = "started_offset_min")
    public val startedOffsetMin: Double,

    @Json(name = "user_id")
    public val userId: String,

    @Json(name = "user_session_id")
    public val userSessionId: String,

    @Json(name = "avg_jitter_ms")
    public val avgJitterMs: Double? = null,

    @Json(name = "browser")
    public val browser: String? = null,

    @Json(name = "delivery_zone")
    public val deliveryZone: String? = null,

    @Json(name = "ingest")
    public val ingest: String? = null,

    @Json(name = "os")
    public val os: String? = null,

    @Json(name = "send_quality_score")
    public val sendQualityScore: Double? = null,

    @Json(name = "tool")
    public val tool: String? = null,

    @Json(name = "encoding")
    public val encoding: io.getstream.android.video.api.generated.models.EncodingProfile? = null
)
