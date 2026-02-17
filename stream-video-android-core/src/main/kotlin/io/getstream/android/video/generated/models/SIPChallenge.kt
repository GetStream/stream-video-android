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

package io.getstream.android.video.generated.models

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

data class SIPChallenge (
    @Json(name = "a1")
    val a1: kotlin.String? = null,

    @Json(name = "algorithm")
    val algorithm: kotlin.String? = null,

    @Json(name = "charset")
    val charset: kotlin.String? = null,

    @Json(name = "cnonce")
    val cnonce: kotlin.String? = null,

    @Json(name = "method")
    val method: kotlin.String? = null,

    @Json(name = "nc")
    val nc: kotlin.String? = null,

    @Json(name = "nonce")
    val nonce: kotlin.String? = null,

    @Json(name = "opaque")
    val opaque: kotlin.String? = null,

    @Json(name = "realm")
    val realm: kotlin.String? = null,

    @Json(name = "response")
    val response: kotlin.String? = null,

    @Json(name = "stale")
    val stale: kotlin.Boolean? = null,

    @Json(name = "uri")
    val uri: kotlin.String? = null,

    @Json(name = "userhash")
    val userhash: kotlin.Boolean? = null,

    @Json(name = "username")
    val username: kotlin.String? = null,

    @Json(name = "domain")
    val domain: kotlin.collections.List<kotlin.String>? = emptyList(),

    @Json(name = "qop")
    val qop: kotlin.collections.List<kotlin.String>? = emptyList()
)
