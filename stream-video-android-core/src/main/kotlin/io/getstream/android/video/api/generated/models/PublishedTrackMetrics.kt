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
public data class PublishedTrackMetrics (
    @Json(name = "codec")
    public val codec: String? = null,

    @Json(name = "track_id")
    public val trackId: String? = null,

    @Json(name = "track_type")
    public val trackType: String? = null,

    @Json(name = "warnings")
    public val warnings: List<io.getstream.android.video.api.generated.models.SessionWarningResponse>? = null,

    @Json(name = "bitrate")
    public val bitrate: io.getstream.android.video.api.generated.models.MetricTimeSeries? = null,

    @Json(name = "framerate")
    public val framerate: io.getstream.android.video.api.generated.models.MetricTimeSeries? = null,

    @Json(name = "resolution")
    public val resolution: io.getstream.android.video.api.generated.models.ResolutionMetricsTimeSeries? = null
)
