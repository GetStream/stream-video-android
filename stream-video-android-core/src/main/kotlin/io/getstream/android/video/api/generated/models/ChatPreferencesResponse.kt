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
 * 
 */
public data class ChatPreferencesResponse (
    @Json(name = "channel_mentions")
    public val channelMentions: String? = null,

    @Json(name = "default_preference")
    public val defaultPreference: String? = null,

    @Json(name = "direct_mentions")
    public val directMentions: String? = null,

    @Json(name = "group_mentions")
    public val groupMentions: String? = null,

    @Json(name = "here_mentions")
    public val hereMentions: String? = null,

    @Json(name = "role_mentions")
    public val roleMentions: String? = null,

    @Json(name = "thread_replies")
    public val threadReplies: String? = null
)
