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

package io.getstream.android.video.generated.models

import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.*
import kotlin.io.*
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * 
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

    @Json(name = "shadow")
    val shadow: kotlin.Boolean,

    @Json(name = "created_by")
    val createdBy: io.getstream.android.video.generated.models.User,

    @Json(name = "type")
    val type: kotlin.String,

    @Json(name = "expiration")
    val expiration: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "reason")
    val reason: kotlin.String? = null,

    @Json(name = "team")
    val team: kotlin.String? = null,

    @Json(name = "user")
    val user: io.getstream.android.video.generated.models.User? = null
)
: io.getstream.android.video.generated.models.VideoEvent()
{
    
    override fun getEventType(): kotlin.String {
        return type
    }    
}
