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

package org.openapitools.client.models

import org.openapitools.client.models.ChannelMute
import org.openapitools.client.models.Device
import org.openapitools.client.models.PrivacySettings
import org.openapitools.client.models.PushNotificationSettings
import org.openapitools.client.models.UserMute




import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.openapitools.client.infrastructure.Serializer

/**
 *
 *
 * @param banned
 * @param channelMutes
 * @param createdAt
 * @param custom
 * @param devices
 * @param id
 * @param invisible
 * @param language
 * @param mutes
 * @param online
 * @param role
 * @param teams
 * @param totalUnreadCount
 * @param unreadChannels
 * @param unreadThreads
 * @param updatedAt
 * @param deactivatedAt
 * @param deletedAt
 * @param image
 * @param lastActive
 * @param latestHiddenChannels
 * @param name
 * @param privacySettings
 * @param pushNotifications
 * @param revokeTokensIssuedBefore
 */


data class OwnUserResponse (

    @Json(name = "banned")
    val banned: kotlin.Boolean,

    @Json(name = "channel_mutes")
    val channelMutes: kotlin.collections.List<ChannelMute>,

    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "custom")
    val custom: kotlin.collections.Map<kotlin.String, kotlin.Any?>,

    @Json(name = "devices")
    val devices: kotlin.collections.List<Device>,

    @Json(name = "id")
    val id: kotlin.String,

    @Json(name = "invisible")
    val invisible: kotlin.Boolean,

    @Json(name = "language")
    val language: kotlin.String,

    @Json(name = "mutes")
    val mutes: kotlin.collections.List<UserMute>,

    @Json(name = "online")
    val online: kotlin.Boolean,

    @Json(name = "role")
    val role: kotlin.String,

    @Json(name = "teams")
    val teams: kotlin.collections.List<kotlin.String>,

    @Json(name = "total_unread_count")
    val totalUnreadCount: kotlin.Int,

    @Json(name = "unread_channels")
    val unreadChannels: kotlin.Int,

    @Json(name = "unread_threads")
    val unreadThreads: kotlin.Int,

    @Json(name = "updated_at")
    val updatedAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "deactivated_at")
    val deactivatedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "deleted_at")
    val deletedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "image")
    val image: kotlin.String? = null,

    @Json(name = "last_active")
    val lastActive: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "latest_hidden_channels")
    val latestHiddenChannels: kotlin.collections.List<kotlin.String>? = null,

    @Json(name = "name")
    val name: kotlin.String? = null,

    @Json(name = "privacy_settings")
    val privacySettings: PrivacySettings? = null,

    @Json(name = "push_notifications")
    val pushNotifications: PushNotificationSettings? = null,

    @Json(name = "revoke_tokens_issued_before")
    val revokeTokensIssuedBefore: org.threeten.bp.OffsetDateTime? = null

)
