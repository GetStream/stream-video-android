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
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * 
 */

data class APIError(
    @Json(name = "code")
    val code: kotlin.Int,

    @Json(name = "duration")
    val duration: kotlin.String,

    @Json(name = "message")
    val message: kotlin.String,

    @Json(name = "more_info")
    val moreInfo: kotlin.String,

    @Json(name = "StatusCode")
    val statusCode: kotlin.Int,

    @Json(name = "details")
    val details: kotlin.collections.List<kotlin.Int> = emptyList(),

    @Json(name = "unrecoverable")
    val unrecoverable: kotlin.Boolean? = null,

    @Json(name = "exception_fields")
    val exceptionFields: kotlin.collections.Map<kotlin.String, kotlin.String>? = null
)
