/*
 * Copyright (c) 2014-2023 Stream.io Inc. All rights reserved.
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

import org.openapitools.client.models.CallSettingsResponse
import org.openapitools.client.models.NotificationSettings




import com.squareup.moshi.Json

/**
 *
 *
 * @param createdAt
 * @param grants
 * @param name
 * @param notificationSettings
 * @param settings
 * @param updatedAt
 */


data class CallTypeResponse (

    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "grants")
    val grants: kotlin.collections.Map<kotlin.String, kotlin.collections.List<kotlin.String>>,

    @Json(name = "name")
    val name: kotlin.String,

    @Json(name = "notification_settings")
    val notificationSettings: NotificationSettings,

    @Json(name = "settings")
    val settings: CallSettingsResponse,

    @Json(name = "updated_at")
    val updatedAt: org.threeten.bp.OffsetDateTime

)
