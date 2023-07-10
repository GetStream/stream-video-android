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
 * @param audio
 * @param muteAllUsers
 * @param screenshare
 * @param userIds
 * @param video
 */


data class MuteUsersRequest (

    @Json(name = "audio")
    val audio: kotlin.Boolean? = null,

    @Json(name = "mute_all_users")
    val muteAllUsers: kotlin.Boolean? = null,

    @Json(name = "screenshare")
    val screenshare: kotlin.Boolean? = null,

    @Json(name = "user_ids")
    val userIds: kotlin.collections.List<kotlin.String>? = null,

    @Json(name = "video")
    val video: kotlin.Boolean? = null

)
