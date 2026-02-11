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
 * PIN routing rule call configuration response
 */

data class SIPInboundRoutingRulePinConfigsResponse (
    @Json(name = "custom_webhook_url")
    val customWebhookUrl: kotlin.String? = null,

    @Json(name = "pin_failed_attempt_prompt")
    val pinFailedAttemptPrompt: kotlin.String? = null,

    @Json(name = "pin_hangup_prompt")
    val pinHangupPrompt: kotlin.String? = null,

    @Json(name = "pin_prompt")
    val pinPrompt: kotlin.String? = null,

    @Json(name = "pin_success_prompt")
    val pinSuccessPrompt: kotlin.String? = null
)
