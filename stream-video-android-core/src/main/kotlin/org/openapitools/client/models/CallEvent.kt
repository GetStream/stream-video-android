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

package org.openapitools.client.models

import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.*
import kotlin.io.*
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 *
 */

data class CallEvent (
    @Json(name = "description")
    val description: kotlin.String,

    @Json(name = "end_timestamp")
    val endTimestamp: kotlin.Int,

    @Json(name = "internal")
    val internal: kotlin.Boolean,

    @Json(name = "kind")
    val kind: kotlin.String,

    @Json(name = "severity")
    val severity: kotlin.Int,

    @Json(name = "timestamp")
    val timestamp: kotlin.Int,

    @Json(name = "type")
    val type: kotlin.String,

    @Json(name = "category")
    val category: kotlin.String? = null,

    @Json(name = "component")
    val component: kotlin.String? = null,

    @Json(name = "issue_tags")
    val issueTags: kotlin.collections.List<kotlin.String>? = null
)
