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
import kotlin.collections.*
import kotlin.io.*
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * SIP Inbound Routing Rule response
 */

data class SIPInboundRoutingRuleResponse (
    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "duration")
    val duration: kotlin.String,

    @Json(name = "id")
    val id: kotlin.String,

    @Json(name = "name")
    val name: kotlin.String,

    @Json(name = "updated_at")
    val updatedAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "called_numbers")
    val calledNumbers: kotlin.collections.List<kotlin.String> = emptyList(),

    @Json(name = "trunk_ids")
    val trunkIds: kotlin.collections.List<kotlin.String> = emptyList(),

    @Json(name = "caller_numbers")
    val callerNumbers: kotlin.collections.List<kotlin.String>? = emptyList(),

    @Json(name = "call_configs")
    val callConfigs: io.getstream.android.video.generated.models.SIPCallConfigsResponse? = null,

    @Json(name = "caller_configs")
    val callerConfigs: io.getstream.android.video.generated.models.SIPCallerConfigsResponse? = null,

    @Json(name = "direct_routing_configs")
    val directRoutingConfigs: io.getstream.android.video.generated.models.SIPDirectRoutingRuleCallConfigsResponse? = null,

    @Json(name = "pin_protection_configs")
    val pinProtectionConfigs: io.getstream.android.video.generated.models.SIPPinProtectionConfigsResponse? = null,

    @Json(name = "pin_routing_configs")
    val pinRoutingConfigs: io.getstream.android.video.generated.models.SIPInboundRoutingRulePinConfigsResponse? = null
)
