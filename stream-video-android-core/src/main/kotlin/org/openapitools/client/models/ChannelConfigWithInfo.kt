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

import org.openapitools.client.models.BlockListOptions
import org.openapitools.client.models.Command
import org.openapitools.client.models.Thresholds




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
 * @param automod
 * @param automodBehavior
 * @param commands
 * @param connectEvents
 * @param createdAt
 * @param customEvents
 * @param markMessagesPending
 * @param maxMessageLength
 * @param mutes
 * @param name
 * @param polls
 * @param pushNotifications
 * @param quotes
 * @param reactions
 * @param readEvents
 * @param reminders
 * @param replies
 * @param search
 * @param typingEvents
 * @param updatedAt
 * @param uploads
 * @param urlEnrichment
 * @param allowedFlagReasons
 * @param automodThresholds
 * @param blocklist
 * @param blocklistBehavior
 * @param blocklists
 * @param grants
 */


data class ChannelConfigWithInfo (

    @Json(name = "automod")
    val automod: ChannelConfigWithInfo.Automod,

    @Json(name = "automod_behavior")
    val automodBehavior: ChannelConfigWithInfo.AutomodBehavior,

    @Json(name = "commands")
    val commands: kotlin.collections.List<Command>,

    @Json(name = "connect_events")
    val connectEvents: kotlin.Boolean,

    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "custom_events")
    val customEvents: kotlin.Boolean,

    @Json(name = "mark_messages_pending")
    val markMessagesPending: kotlin.Boolean,

    @Json(name = "max_message_length")
    val maxMessageLength: kotlin.Int,

    @Json(name = "mutes")
    val mutes: kotlin.Boolean,

    @Json(name = "name")
    val name: kotlin.String,

    @Json(name = "polls")
    val polls: kotlin.Boolean,

    @Json(name = "push_notifications")
    val pushNotifications: kotlin.Boolean,

    @Json(name = "quotes")
    val quotes: kotlin.Boolean,

    @Json(name = "reactions")
    val reactions: kotlin.Boolean,

    @Json(name = "read_events")
    val readEvents: kotlin.Boolean,

    @Json(name = "reminders")
    val reminders: kotlin.Boolean,

    @Json(name = "replies")
    val replies: kotlin.Boolean,

    @Json(name = "search")
    val search: kotlin.Boolean,

    @Json(name = "typing_events")
    val typingEvents: kotlin.Boolean,

    @Json(name = "updated_at")
    val updatedAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "uploads")
    val uploads: kotlin.Boolean,

    @Json(name = "url_enrichment")
    val urlEnrichment: kotlin.Boolean,

    @Json(name = "allowed_flag_reasons")
    val allowedFlagReasons: kotlin.collections.List<kotlin.String>? = null,

    @Json(name = "automod_thresholds")
    val automodThresholds: Thresholds? = null,

    @Json(name = "blocklist")
    val blocklist: kotlin.String? = null,

    @Json(name = "blocklist_behavior")
    val blocklistBehavior: ChannelConfigWithInfo.BlocklistBehavior? = null,

    @Json(name = "blocklists")
    val blocklists: kotlin.collections.List<BlockListOptions>? = null,

    @Json(name = "grants")
    val grants: kotlin.collections.Map<kotlin.String, kotlin.collections.List<kotlin.String>>? = null

)

{

    /**
     *
     *
     * Values: disabled,simple,aI
     */

    sealed class Automod(val value: kotlin.String) {
        override fun toString(): String = value

        companion object {
            fun fromString(s: kotlin.String): Automod = when (s) {
                "disabled" -> Disabled
                "simple" -> Simple
                "AI" -> AI
                else -> Unknown(s)
            }
        }

        object Disabled : Automod("disabled")
        object Simple : Automod("simple")
        object AI : Automod("AI")
        data class Unknown(val unknownValue: kotlin.String) : Automod(unknownValue)

        class AutomodAdapter : JsonAdapter<Automod>() {
            @FromJson
            override fun fromJson(reader: JsonReader): Automod? {
                val s = reader.nextString() ?: return null
                return fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: Automod?) {
                writer.value(value?.value)
            }
        }
    }


    /**
     *
     *
     * Values: flag,block,shadowBlock
     */

    sealed class AutomodBehavior(val value: kotlin.String) {
        override fun toString(): String = value

        companion object {
            fun fromString(s: kotlin.String): AutomodBehavior = when (s) {
                "flag" -> Flag
                "block" -> Block
                "shadow_block" -> ShadowBlock
                else -> Unknown(s)
            }
        }

        object Flag : AutomodBehavior("flag")
        object Block : AutomodBehavior("block")
        object ShadowBlock : AutomodBehavior("shadow_block")
        data class Unknown(val unknownValue: kotlin.String) : AutomodBehavior(unknownValue)

        class AutomodBehaviorAdapter : JsonAdapter<AutomodBehavior>() {
            @FromJson
            override fun fromJson(reader: JsonReader): AutomodBehavior? {
                val s = reader.nextString() ?: return null
                return fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: AutomodBehavior?) {
                writer.value(value?.value)
            }
        }
    }


    /**
     *
     *
     * Values: flag,block,shadowBlock
     */

    sealed class BlocklistBehavior(val value: kotlin.String) {
        override fun toString(): String = value

        companion object {
            fun fromString(s: kotlin.String): BlocklistBehavior = when (s) {
                "flag" -> Flag
                "block" -> Block
                "shadow_block" -> ShadowBlock
                else -> Unknown(s)
            }
        }

        object Flag : BlocklistBehavior("flag")
        object Block : BlocklistBehavior("block")
        object ShadowBlock : BlocklistBehavior("shadow_block")
        data class Unknown(val unknownValue: kotlin.String) : BlocklistBehavior(unknownValue)

        class BlocklistBehaviorAdapter : JsonAdapter<BlocklistBehavior>() {
            @FromJson
            override fun fromJson(reader: JsonReader): BlocklistBehavior? {
                val s = reader.nextString() ?: return null
                return fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: BlocklistBehavior?) {
                writer.value(value?.value)
            }
        }
    }



}
