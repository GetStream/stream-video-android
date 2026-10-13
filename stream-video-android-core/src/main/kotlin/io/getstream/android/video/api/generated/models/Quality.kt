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
data class Quality (
    @Json(name = "viewer_interruption_note")
    val viewerInterruptionNote: String,

    @Json(name = "interruption_incidents")
    val interruptionIncidents: List<io.getstream.android.video.api.generated.models.Incident> = emptyList(),

    @Json(name = "connection_avg_jitter_ms")
    val connectionAvgJitterMs: io.getstream.android.video.api.generated.models.Percentiles,

    @Json(name = "connection_avg_latency_ms")
    val connectionAvgLatencyMs: io.getstream.android.video.api.generated.models.Percentiles,

    @Json(name = "score_bands_by_connection_pct")
    val scoreBandsByConnectionPct: io.getstream.android.video.api.generated.models.ScoreBands,

    @Json(name = "score_bands_by_watch_time_pct")
    val scoreBandsByWatchTimePct: io.getstream.android.video.api.generated.models.ScoreBands,

    @Json(name = "p50_quality_score")
    val p50QualityScore: Double? = null,

    @Json(name = "p5_quality_score")
    val p5QualityScore: Double? = null,

    @Json(name = "viewer_interruption_rate_pct")
    val viewerInterruptionRatePct: Double? = null
)
