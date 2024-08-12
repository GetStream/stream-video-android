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

import org.openapitools.client.models.PrivacySettings
import org.openapitools.client.models.PushNotificationSettings




import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.openapitools.client.infrastructure.Serializer

/**
 * Represents chat user
 *
 * @param banned Whether a user is banned or not
 * @param custom
 * @param id Unique user identifier
 * @param online Whether a user online or not
 * @param role Determines the set of user permissions
 * @param banExpires Expiration date of the ban
 * @param createdAt Date/time of creation
 * @param deactivatedAt Date of deactivation
 * @param deletedAt Date/time of deletion
 * @param invisible
 * @param language Preferred language of a user
 * @param lastActive Date of last activity
 * @param privacySettings
 * @param pushNotifications
 * @param revokeTokensIssuedBefore Revocation date for tokens
 * @param teams List of teams user is a part of
 * @param updatedAt Date/time of the last update
 */


data class UserObject (

    /* Whether a user is banned or not */
    @Json(name = "banned")
    val banned: kotlin.Boolean,

    @Json(name = "custom")
    val custom: kotlin.collections.Map<kotlin.String, kotlin.Any?>,

    /* Unique user identifier */
    @Json(name = "id")
    val id: kotlin.String,

    /* Whether a user online or not */
    @Json(name = "online")
    val online: kotlin.Boolean,

    /* Determines the set of user permissions */
    @Json(name = "role")
    val role: kotlin.String,

    /* Expiration date of the ban */
    @Json(name = "ban_expires")
    val banExpires: org.threeten.bp.OffsetDateTime? = null,

    /* Date/time of creation */
    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime? = null,

    /* Date of deactivation */
    @Json(name = "deactivated_at")
    val deactivatedAt: org.threeten.bp.OffsetDateTime? = null,

    /* Date/time of deletion */
    @Json(name = "deleted_at")
    val deletedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "invisible")
    val invisible: kotlin.Boolean? = null,

    /* Preferred language of a user */
    @Json(name = "language")
    val language: kotlin.String? = null,

    /* Date of last activity */
    @Json(name = "last_active")
    val lastActive: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "privacy_settings")
    val privacySettings: PrivacySettings? = null,

    @Json(name = "push_notifications")
    val pushNotifications: PushNotificationSettings? = null,

    /* Revocation date for tokens */
    @Json(name = "revoke_tokens_issued_before")
    val revokeTokensIssuedBefore: org.threeten.bp.OffsetDateTime? = null,

    /* List of teams user is a part of */
    @Json(name = "teams")
    val teams: kotlin.collections.List<kotlin.String>? = null,

    /* Date/time of the last update */
    @Json(name = "updated_at")
    val updatedAt: org.threeten.bp.OffsetDateTime? = null

)
