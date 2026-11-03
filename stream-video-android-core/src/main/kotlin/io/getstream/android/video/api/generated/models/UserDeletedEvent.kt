/*
 * Copyright (c) 2014-2026 Stream.io Inc. All rights reserved.
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

package io.getstream.android.video.api.generated.models

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * This event is sent when a user gets deleted. The event contains information about the user that was deleted and the deletion options that were used.
 */
public data class UserDeletedEvent (
    @Json(name = "created_at")
    public val createdAt: kotlin.time.Instant,

    @Json(name = "delete_conversation")
    public val deleteConversation: String,

    @Json(name = "delete_conversation_channels")
    public val deleteConversationChannels: Boolean,

    @Json(name = "delete_messages")
    public val deleteMessages: String,

    @Json(name = "delete_user")
    public val deleteUser: String,

    @Json(name = "hard_delete")
    public val hardDelete: Boolean,

    @Json(name = "mark_messages_deleted")
    public val markMessagesDeleted: Boolean,

    @Json(name = "custom")
    public val custom: Map<String, Any?> = emptyMap(),

    @Json(name = "user")
    public val user: io.getstream.android.video.api.generated.models.UserResponseCommonFields,

    @Json(name = "type")
    public val type: String = "user.deleted",

    @Json(name = "received_at")
    public val receivedAt: kotlin.time.Instant? = null
)
: io.getstream.android.video.api.generated.models.VideoEvent()
{
    
    override fun getEventType(): String {
        return type
    }    
}
