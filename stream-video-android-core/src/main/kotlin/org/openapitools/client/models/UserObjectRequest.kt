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

import com.squareup.moshi.Json

/**
 * Represents chat user
 *
 * @param id Unique user identifier
 * @param banExpires Expiration date of the ban
 * @param banned Whether a user is banned or not
 * @param invisible * @param language Preferred language of a user
 * @param pushNotifications * @param revokeTokensIssuedBefore Revocation date for tokens
 * @param role Determines the set of user permissions
 * @param teams List of teams user is a part of
 */

internal data class UserObjectRequest(

    /* Unique user identifier */
    @Json(name = "id")
    val id: kotlin.String,

    /* Expiration date of the ban */
    @Json(name = "ban_expires")
    val banExpires: java.time.OffsetDateTime? = null,

    /* Whether a user is banned or not */
    @Json(name = "banned")
    val banned: kotlin.Boolean? = null,

    @Json(name = "invisible")
    val invisible: kotlin.Boolean? = null,

    /* Preferred language of a user */
    @Json(name = "language")
    val language: kotlin.String? = null,

    @Json(name = "push_notifications")
    val pushNotifications: PushNotificationSettingsRequest? = null,

    /* Revocation date for tokens */
    @Json(name = "revoke_tokens_issued_before")
    val revokeTokensIssuedBefore: java.time.OffsetDateTime? = null,

    /* Determines the set of user permissions */
    @Json(name = "role")
    val role: kotlin.String? = null,

    /* List of teams user is a part of */
    @Json(name = "teams")
    val teams: kotlin.collections.List<kotlin.String>? = null

) : kotlin.collections.HashMap<String, kotlin.Any>()
