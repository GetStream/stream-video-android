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
data class ParticipantReportResponse (
    @Json(name = "sum")
    val sum: Int,

    @Json(name = "unique")
    val unique: Int,

    @Json(name = "max_concurrent")
    val maxConcurrent: Int? = null,

    @Json(name = "by_browser")
    val byBrowser: List<io.getstream.android.video.api.generated.models.GroupedStatsResponse>? = emptyList(),

    @Json(name = "by_country")
    val byCountry: List<io.getstream.android.video.api.generated.models.GroupedStatsResponse>? = emptyList(),

    @Json(name = "by_device")
    val byDevice: List<io.getstream.android.video.api.generated.models.GroupedStatsResponse>? = emptyList(),

    @Json(name = "by_operating_system")
    val byOperatingSystem: List<io.getstream.android.video.api.generated.models.GroupedStatsResponse>? = emptyList(),

    @Json(name = "count_over_time")
    val countOverTime: io.getstream.android.video.api.generated.models.ParticipantCountOverTimeResponse? = null,

    @Json(name = "publishers")
    val publishers: io.getstream.android.video.api.generated.models.PublisherStatsResponse? = null,

    @Json(name = "subscribers")
    val subscribers: io.getstream.android.video.api.generated.models.SubscriberStatsResponse? = null
)
