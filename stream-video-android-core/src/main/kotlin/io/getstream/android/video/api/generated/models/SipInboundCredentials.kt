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
 * Credentials for SIP inbound call authentication
 */
data class SipInboundCredentials (
    @Json(name = "api_key")
    val apiKey: String,

    @Json(name = "call_id")
    val callId: String,

    @Json(name = "call_type")
    val callType: String,

    @Json(name = "token")
    val token: String,

    @Json(name = "user_id")
    val userId: String,

    @Json(name = "call_custom_data")
    val callCustomData: Map<String, Any?> = emptyMap(),

    @Json(name = "user_custom_data")
    val userCustomData: Map<String, Any?> = emptyMap()
)
