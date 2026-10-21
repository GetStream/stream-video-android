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
 * SIP digest challenge authentication data
 */
public data class SIPChallengeRequest (
    @Json(name = "a1")
    public val a1: String? = null,

    @Json(name = "algorithm")
    public val algorithm: String? = null,

    @Json(name = "charset")
    public val charset: String? = null,

    @Json(name = "cnonce")
    public val cnonce: String? = null,

    @Json(name = "method")
    public val method: String? = null,

    @Json(name = "nc")
    public val nc: String? = null,

    @Json(name = "nonce")
    public val nonce: String? = null,

    @Json(name = "opaque")
    public val opaque: String? = null,

    @Json(name = "realm")
    public val realm: String? = null,

    @Json(name = "response")
    public val response: String? = null,

    @Json(name = "stale")
    public val stale: Boolean? = null,

    @Json(name = "uri")
    public val uri: String? = null,

    @Json(name = "userhash")
    public val userhash: Boolean? = null,

    @Json(name = "username")
    public val username: String? = null,

    @Json(name = "domain")
    public val domain: List<String>? = null,

    @Json(name = "qop")
    public val qop: List<String>? = null
)
