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
public data class BroadcastDigest (
    @Json(name = "schema_version")
    public val schemaVersion: String,

    @Json(name = "audience")
    public val audience: io.getstream.android.video.api.generated.models.Audience,

    @Json(name = "broadcast")
    public val broadcast: io.getstream.android.video.api.generated.models.BroadcastInfo,

    @Json(name = "coverage")
    public val coverage: io.getstream.android.video.api.generated.models.Coverage,

    @Json(name = "joins")
    public val joins: io.getstream.android.video.api.generated.models.Joins,

    @Json(name = "poor_tail")
    public val poorTail: io.getstream.android.video.api.generated.models.PoorTail,

    @Json(name = "quality")
    public val quality: io.getstream.android.video.api.generated.models.Quality,

    @Json(name = "segments")
    public val segments: io.getstream.android.video.api.generated.models.Segments,

    @Json(name = "source")
    public val source: io.getstream.android.video.api.generated.models.SourceHealth,

    @Json(name = "viewers")
    public val viewers: io.getstream.android.video.api.generated.models.ViewerBehavior
)
