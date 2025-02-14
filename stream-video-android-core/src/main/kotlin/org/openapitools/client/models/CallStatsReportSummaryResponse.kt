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

import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.*
import kotlin.io.*
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 *
 */

data class CallStatsReportSummaryResponse (
    @Json(name = "call_cid")
    val callCid: kotlin.String,

    @Json(name = "call_duration_seconds")
    val callDurationSeconds: kotlin.Int,

    @Json(name = "call_session_id")
    val callSessionId: kotlin.String,

    @Json(name = "call_status")
    val callStatus: kotlin.String,

    @Json(name = "first_stats_time")
    val firstStatsTime: org.threeten.bp.OffsetDateTime,

    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "min_user_rating")
    val minUserRating: kotlin.Int? = null,

    @Json(name = "quality_score")
    val qualityScore: kotlin.Int? = null
)
