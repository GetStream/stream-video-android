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
 * Request to resolve SIP inbound routing using challenge authentication
 */

data class ResolveSipInboundRequest (
    @Json(name = "sip_caller_number")
    val sipCallerNumber: kotlin.String,

    @Json(name = "sip_trunk_number")
    val sipTrunkNumber: kotlin.String,

    @Json(name = "challenge")
    val challenge: io.getstream.android.video.generated.models.SIPChallenge,

    @Json(name = "routing_number")
    val routingNumber: kotlin.String? = null,

    @Json(name = "sip_headers")
    val sipHeaders: kotlin.collections.Map<kotlin.String, kotlin.String>? = emptyMap()
)
