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
public data class FeedsPreferencesResponse (
    @Json(name = "comment")
    public val comment: String? = null,

    @Json(name = "comment_mention")
    public val commentMention: String? = null,

    @Json(name = "comment_reaction")
    public val commentReaction: String? = null,

    @Json(name = "comment_reply")
    public val commentReply: String? = null,

    @Json(name = "follow")
    public val follow: String? = null,

    @Json(name = "mention")
    public val mention: String? = null,

    @Json(name = "reaction")
    public val reaction: String? = null,

    @Json(name = "custom_activity_types")
    public val customActivityTypes: Map<String, String>? = null
)
