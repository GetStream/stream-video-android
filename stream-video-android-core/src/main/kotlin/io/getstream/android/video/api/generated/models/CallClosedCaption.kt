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
 * CallClosedCaption represents a closed caption of a call.
 */
public data class CallClosedCaption (
    @Json(name = "end_time")
    public val endTime: kotlin.time.Instant,

    @Json(name = "id")
    public val id: String,

    @Json(name = "language")
    public val language: String,

    @Json(name = "speaker_id")
    public val speakerId: String,

    @Json(name = "start_time")
    public val startTime: kotlin.time.Instant,

    @Json(name = "text")
    public val text: String,

    @Json(name = "translated")
    public val translated: Boolean,

    @Json(name = "user")
    public val user: io.getstream.android.video.api.generated.models.UserResponse,

    @Json(name = "service")
    public val service: String? = null
)
