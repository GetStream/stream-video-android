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

data class JoinCallRequest (
    @Json(name = "location")
    val location: kotlin.String,

    @Json(name = "create")
    val create: kotlin.Boolean? = null,

    @Json(name = "e2ee")
    val e2ee: kotlin.Boolean? = null,

    @Json(name = "hint_high_scale_livestream_publisher")
    val hintHighScaleLivestreamPublisher: kotlin.Boolean? = null,

    @Json(name = "members_limit")
    val membersLimit: kotlin.Int? = null,

    @Json(name = "migrating_from")
    val migratingFrom: kotlin.String? = null,

    @Json(name = "notify")
    val notify: kotlin.Boolean? = null,

    @Json(name = "ring")
    val ring: kotlin.Boolean? = null,

    @Json(name = "video")
    val video: kotlin.Boolean? = null,

    @Json(name = "migrating_from_list")
    val migratingFromList: kotlin.collections.List<kotlin.String>? = emptyList(),

    @Json(name = "data")
    val data: io.getstream.android.video.generated.models.CallRequest? = null,

    @Json(name = "prefer")
    val prefer: Prefer? = null,

    @Json(name = "playback_capabilities")
    val playbackCapabilities: kotlin.collections.List<kotlin.String>? = emptyList()
)
{
    
    /**
    * Prefer Enum
    */
    sealed class Prefer(val value: kotlin.String) {
            override fun toString(): String = value

            companion object {
                fun fromString(s: kotlin.String): Prefer = when (s) {
                    "hls" -> Hls
                    else -> Unknown(s)
                }
            }
            object Hls : Prefer("hls")
            data class Unknown(val unknownValue: kotlin.String) : Prefer(unknownValue)
        

        class PreferAdapter : JsonAdapter<Prefer>() {
            @FromJson
            override fun fromJson(reader: JsonReader): Prefer? {
                val s = reader.nextString() ?: return null
                return Prefer.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: Prefer?) {
                writer.value(value?.value)
            }
        }
    }    
}
