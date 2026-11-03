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
public data class ViewerBehavior (
    @Json(name = "connection_duration_p50_s")
    public val connectionDurationP50S: Int,

    @Json(name = "connections_per_viewer_mean")
    public val connectionsPerViewerMean: Double,

    @Json(name = "median_watch_min")
    public val medianWatchMin: Double,

    @Json(name = "note")
    public val note: String,

    @Json(name = "p90_watch_min")
    public val p90WatchMin: Double,

    @Json(name = "bounce_rate_pct")
    public val bounceRatePct: Double? = null,

    @Json(name = "connections_under_30s_pct")
    public val connectionsUnder30sPct: Double? = null,

    @Json(name = "return_visit_rate_pct")
    public val returnVisitRatePct: Double? = null
)
