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

package io.getstream.android.video.generated.models

import kotlin.collections.List
import kotlin.collections.Map
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * Basic response information
 */

data class GetCallStatsResponse(
    @Json(name = "call_duration_seconds")
    val callDurationSeconds: kotlin.Int,

    @Json(name = "call_status")
    val callStatus: kotlin.String,

    @Json(name = "duration")
    val duration: kotlin.String,

    @Json(name = "max_freezes_duration_seconds")
    val maxFreezesDurationSeconds: kotlin.Int,

    @Json(name = "max_participants")
    val maxParticipants: kotlin.Int,

    @Json(name = "max_total_quality_limitation_duration_seconds")
    val maxTotalQualityLimitationDurationSeconds: kotlin.Int,

    @Json(name = "publishing_participants")
    val publishingParticipants: kotlin.Int,

    @Json(name = "quality_score")
    val qualityScore: kotlin.Int,

    @Json(name = "sfu_count")
    val sfuCount: kotlin.Int,

    @Json(name = "participant_report")
    val participantReport: kotlin.collections.List<io.getstream.android.video.generated.models.UserStats> = emptyList(),

    @Json(name = "sfus")
    val sfus: kotlin.collections.List<io.getstream.android.video.generated.models.SFULocationResponse> = emptyList(),

    @Json(name = "average_connection_time")
    val averageConnectionTime: kotlin.Float? = null,

    @Json(name = "aggregated")
    val aggregated: io.getstream.android.video.generated.models.AggregatedStats? = null,

    @Json(name = "call_timeline")
    val callTimeline: io.getstream.android.video.generated.models.CallTimeline? = null,

    @Json(name = "jitter")
    val jitter: io.getstream.android.video.generated.models.TimeStats? = null,

    @Json(name = "latency")
    val latency: io.getstream.android.video.generated.models.TimeStats? = null
)
