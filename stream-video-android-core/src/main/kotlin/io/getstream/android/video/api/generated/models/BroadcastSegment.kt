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
data class BroadcastSegment (
    @Json(name = "key")
    val key: String,

    @Json(name = "sessions")
    val sessions: Int,

    @Json(name = "avg_quality_score")
    val avgQualityScore: Float? = null,

    @Json(name = "p5_quality_score")
    val p5QualityScore: Float? = null,

    @Json(name = "poor_pct")
    val poorPct: Float? = null,

    @Json(name = "share_pct")
    val sharePct: Float? = null,

    @Json(name = "watch_share_pct")
    val watchSharePct: Float? = null
)
