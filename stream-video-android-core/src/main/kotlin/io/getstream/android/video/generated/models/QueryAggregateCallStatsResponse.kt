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
import kotlin.collections.*
import kotlin.io.*
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * Basic response information
 */

data class QueryAggregateCallStatsResponse (
    @Json(name = "duration")
    val duration: kotlin.String,

    @Json(name = "call_duration_report")
    val callDurationReport: io.getstream.android.video.generated.models.CallDurationReportResponse? = null,

    @Json(name = "call_participant_count_report")
    val callParticipantCountReport: io.getstream.android.video.generated.models.CallParticipantCountReportResponse? = null,

    @Json(name = "calls_per_day_report")
    val callsPerDayReport: io.getstream.android.video.generated.models.CallsPerDayReportResponse? = null,

    @Json(name = "network_metrics_report")
    val networkMetricsReport: io.getstream.android.video.generated.models.NetworkMetricsReportResponse? = null,

    @Json(name = "quality_score_report")
    val qualityScoreReport: io.getstream.android.video.generated.models.QualityScoreReportResponse? = null,

    @Json(name = "sdk_usage_report")
    val sdkUsageReport: io.getstream.android.video.generated.models.SDKUsageReportResponse? = null,

    @Json(name = "user_feedback_report")
    val userFeedbackReport: io.getstream.android.video.generated.models.UserFeedbackReportResponse? = null
)
