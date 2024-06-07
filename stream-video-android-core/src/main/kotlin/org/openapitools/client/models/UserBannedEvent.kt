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
 * @param channelId
 * @param channelType
 * @param cid
 * @param createdAt
 * @param createdBy
 * @param shadow
 * @param type
 * @param expiration
 * @param reason
 * @param team
 * @param user
 */


data class UserBannedEvent (

    @Json(name = "channel_id")
    val channelId: kotlin.String,

    @Json(name = "channel_type")
    val channelType: kotlin.String,

    @Json(name = "cid")
    val cid: kotlin.String,

    @Json(name = "created_at")
    val createdAt: org.threeten.bp.OffsetDateTime,

    @Json(name = "created_by")
    val createdBy: UserObject,

    @Json(name = "shadow")
    val shadow: kotlin.Boolean,

    @Json(name = "type")
    val type: kotlin.String = "user.banned",

    @Json(name = "expiration")
    val expiration: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "reason")
    val reason: kotlin.String? = null,

    @Json(name = "team")
    val team: kotlin.String? = null,

    @Json(name = "user")
    val user: UserObject? = null

) : VideoEvent(), WSClientEvent {

    override fun getEventType(): String {
        return type
    }
}
