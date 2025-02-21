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
    "UnusedImport",
)

package io.getstream.android.video.generated.models

import com.squareup.moshi.Json

/**
 * Response for Device
 */

data class DeviceResponse(
    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "id")
    val id: kotlin.String,

    @Json(name = "push_provider")
    val pushProvider: kotlin.String,

    @Json(name = "user_id")
    val userId: kotlin.String,

    @Json(name = "disabled")
    val disabled: kotlin.Boolean? = null,

    @Json(name = "disabled_reason")
    val disabledReason: kotlin.String? = null,

    @Json(name = "push_provider_name")
    val pushProviderName: kotlin.String? = null,

    @Json(name = "voip")
    val voip: kotlin.Boolean? = null,
)
