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
data class Audience (
    @Json(name = "avg_concurrent_viewers")
    val avgConcurrentViewers: Int,

    @Json(name = "hours_watched")
    val hoursWatched: Double,

    @Json(name = "peak_concurrent_viewers")
    val peakConcurrentViewers: Int,

    @Json(name = "unique_viewers")
    val uniqueViewers: Int,

    @Json(name = "viewer_connections")
    val viewerConnections: Int,

    @Json(name = "concurrency_by_minute")
    val concurrencyByMinute: List<io.getstream.android.video.api.generated.models.ConcurrencyMinute> = emptyList(),

    @Json(name = "peak_at")
    val peakAt: String? = null,

    @Json(name = "ramp_up_min_to_90pct_peak")
    val rampUpMinTo90pctPeak: Int? = null,

    @Json(name = "retention_at_90pct_mark")
    val retentionAt90pctMark: Double? = null,

    @Json(name = "retention_at_midpoint")
    val retentionAtMidpoint: Double? = null,

    @Json(name = "shape")
    val shape: String? = null
)
