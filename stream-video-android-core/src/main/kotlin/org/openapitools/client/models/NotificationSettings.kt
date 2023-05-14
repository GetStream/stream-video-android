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

import org.openapitools.client.models.EventNotificationSettings




import com.squareup.moshi.Json

/**
 *
 *
 * @param callCreated
 * @param callLiveStarted
 * @param enabled
 * @param sessionStarted
 */


data class NotificationSettings (

    @Json(name = "call_created")
    val callCreated: EventNotificationSettings,

    @Json(name = "call_live_started")
    val callLiveStarted: EventNotificationSettings,

    @Json(name = "enabled")
    val enabled: kotlin.Boolean,

    @Json(name = "session_started")
    val sessionStarted: EventNotificationSettings

)
