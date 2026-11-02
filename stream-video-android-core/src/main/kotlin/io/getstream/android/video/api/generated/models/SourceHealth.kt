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
public data class SourceHealth (
    @Json(name = "co_host_peak")
    public val coHostPeak: Int,

    @Json(name = "dead_air_s")
    public val deadAirS: Int,

    @Json(name = "interruptions")
    public val interruptions: List<io.getstream.android.video.api.generated.models.SourceInterruption> = emptyList(),

    @Json(name = "publisher_sessions")
    public val publisherSessions: List<io.getstream.android.video.api.generated.models.PublisherSession> = emptyList()
)
