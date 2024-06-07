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

import org.openapitools.client.models.UserObject




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
 * @param banned Whether member is banned this channel or not
 * @param channelRole Role of the member in the channel
 * @param createdAt Date/time of creation
 * @param notificationsMuted
 * @param shadowBanned Whether member is shadow banned in this channel or not
 * @param updatedAt Date/time of the last update
 * @param banExpires Expiration date of the ban
 * @param deletedAt
 * @param inviteAcceptedAt Date when invite was accepted
 * @param inviteRejectedAt Date when invite was rejected
 * @param invited Whether member was invited or not
 * @param isModerator Whether member is channel moderator or not
 * @param status
 * @param user
 * @param userId
 */


data class ChannelMember (

    /* Whether member is banned this channel or not */
    @Json(name = "banned")
    val banned: kotlin.Boolean,

    /* Role of the member in the channel */
    @Json(name = "channel_role")
    val channelRole: kotlin.String,

    /* Date/time of creation */
    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "notifications_muted")
    val notificationsMuted: kotlin.Boolean,

    /* Whether member is shadow banned in this channel or not */
    @Json(name = "shadow_banned")
    val shadowBanned: kotlin.Boolean,

    /* Date/time of the last update */
    @Json(name = "updated_at")
    val updatedAt: org.threeten.bp.OffsetDateTime,

    /* Expiration date of the ban */
    @Json(name = "ban_expires")
    val banExpires: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "deleted_at")
    val deletedAt: org.threeten.bp.OffsetDateTime? = null,

    /* Date when invite was accepted */
    @Json(name = "invite_accepted_at")
    val inviteAcceptedAt: org.threeten.bp.OffsetDateTime? = null,

    /* Date when invite was rejected */
    @Json(name = "invite_rejected_at")
    val inviteRejectedAt: org.threeten.bp.OffsetDateTime? = null,

    /* Whether member was invited or not */
    @Json(name = "invited")
    val invited: kotlin.Boolean? = null,

    /* Whether member is channel moderator or not */
    @Json(name = "is_moderator")
    val isModerator: kotlin.Boolean? = null,

    @Json(name = "status")
    val status: kotlin.String? = null,

    @Json(name = "user")
    val user: UserObject? = null,

    @Json(name = "user_id")
    val userId: kotlin.String? = null

)
