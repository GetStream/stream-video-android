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

import org.openapitools.client.models.ChannelConfigWithInfo
import org.openapitools.client.models.ChannelMember
import org.openapitools.client.models.UserObject




import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.openapitools.client.infrastructure.Serializer

/**
 * Represents channel in chat
 *
 * @param cid Channel CID (<type>:<id>)
 * @param createdAt Date/time of creation
 * @param custom
 * @param disabled
 * @param frozen Whether channel is frozen or not
 * @param id Channel unique ID
 * @param type Type of the channel
 * @param updatedAt Date/time of the last update
 * @param autoTranslationEnabled Whether auto translation is enabled or not
 * @param autoTranslationLanguage Language to translate to when auto translation is active
 * @param blocked Whether this channel is blocked by current user or not
 * @param config
 * @param cooldown Cooldown period after sending each message
 * @param createdBy
 * @param deletedAt Date/time of deletion
 * @param hidden Whether this channel is hidden by current user or not
 * @param hideMessagesBefore Date since when the message history is accessible
 * @param lastMessageAt Date of the last message sent
 * @param memberCount Number of members in the channel
 * @param members List of channel members (max 100)
 * @param muteExpiresAt Date of mute expiration
 * @param muted Whether this channel is muted or not
 * @param ownCapabilities List of channel capabilities of authenticated user
 * @param team Team the channel belongs to (multi-tenant only)
 * @param truncatedAt Date of the latest truncation of the channel
 * @param truncatedBy
 */


data class ChannelResponse (

    /* Channel CID (<type>:<id>) */
    @Json(name = "cid")
    val cid: kotlin.String,

    /* Date/time of creation */
    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "custom")
    val custom: kotlin.collections.Map<kotlin.String, kotlin.Any?>,

    @Json(name = "disabled")
    val disabled: kotlin.Boolean,

    /* Whether channel is frozen or not */
    @Json(name = "frozen")
    val frozen: kotlin.Boolean,

    /* Channel unique ID */
    @Json(name = "id")
    val id: kotlin.String,

    /* Type of the channel */
    @Json(name = "type")
    val type: kotlin.String,

    /* Date/time of the last update */
    @Json(name = "updated_at")
    val updatedAt: org.threeten.bp.OffsetDateTime,

    /* Whether auto translation is enabled or not */
    @Json(name = "auto_translation_enabled")
    val autoTranslationEnabled: kotlin.Boolean? = null,

    /* Language to translate to when auto translation is active */
    @Json(name = "auto_translation_language")
    val autoTranslationLanguage: kotlin.String? = null,

    /* Whether this channel is blocked by current user or not */
    @Json(name = "blocked")
    val blocked: kotlin.Boolean? = null,

    @Json(name = "config")
    val config: ChannelConfigWithInfo? = null,

    /* Cooldown period after sending each message */
    @Json(name = "cooldown")
    val cooldown: kotlin.Int? = null,

    @Json(name = "created_by")
    val createdBy: UserObject? = null,

    /* Date/time of deletion */
    @Json(name = "deleted_at")
    val deletedAt: org.threeten.bp.OffsetDateTime? = null,

    /* Whether this channel is hidden by current user or not */
    @Json(name = "hidden")
    val hidden: kotlin.Boolean? = null,

    /* Date since when the message history is accessible */
    @Json(name = "hide_messages_before")
    val hideMessagesBefore: org.threeten.bp.OffsetDateTime? = null,

    /* Date of the last message sent */
    @Json(name = "last_message_at")
    val lastMessageAt: org.threeten.bp.OffsetDateTime? = null,

    /* Number of members in the channel */
    @Json(name = "member_count")
    val memberCount: kotlin.Int? = null,

    /* List of channel members (max 100) */
    @Json(name = "members")
    val members: kotlin.collections.List<ChannelMember>? = null,

    /* Date of mute expiration */
    @Json(name = "mute_expires_at")
    val muteExpiresAt: org.threeten.bp.OffsetDateTime? = null,

    /* Whether this channel is muted or not */
    @Json(name = "muted")
    val muted: kotlin.Boolean? = null,

    /* List of channel capabilities of authenticated user */
    @Json(name = "own_capabilities")
    val ownCapabilities: kotlin.collections.List<kotlin.String>? = null,

    /* Team the channel belongs to (multi-tenant only) */
    @Json(name = "team")
    val team: kotlin.String? = null,

    /* Date of the latest truncation of the channel */
    @Json(name = "truncated_at")
    val truncatedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "truncated_by")
    val truncatedBy: UserObject? = null

)
