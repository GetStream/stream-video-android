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
 * An object representing a device which can receive push notifications.
 */
data class DeviceResponse (
    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "id")
    val id: String,

    @Json(name = "push_provider")
    val pushProvider: String,

    @Json(name = "user_id")
    val userId: String,

    @Json(name = "disabled")
    val disabled: Boolean? = null,

    @Json(name = "disabled_reason")
    val disabledReason: String? = null,

    @Json(name = "hardware_id")
    val hardwareId: String? = null,

    @Json(name = "push_provider_name")
    val pushProviderName: String? = null,

    @Json(name = "voip")
    val voip: Boolean? = null
)
