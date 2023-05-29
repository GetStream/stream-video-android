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

import org.openapitools.client.models.EventNotificationSettingsRequest




import com.squareup.moshi.Json

/**
 *
 *
 * @param callLiveStarted
 * @param callNotification
 * @param callRing
 * @param enabled
 * @param sessionStarted
 */


data class NotificationSettingsRequest (

    @Json(name = "call_live_started")
    val callLiveStarted: EventNotificationSettingsRequest? = null,

    @Json(name = "call_notification")
    val callNotification: EventNotificationSettingsRequest? = null,

    @Json(name = "call_ring")
    val callRing: EventNotificationSettingsRequest? = null,

    @Json(name = "enabled")
    val enabled: kotlin.Boolean? = null,

    @Json(name = "session_started")
    val sessionStarted: EventNotificationSettingsRequest? = null

)
