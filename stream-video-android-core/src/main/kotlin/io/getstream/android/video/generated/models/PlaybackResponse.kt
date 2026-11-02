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
 * How the client should play the call when it is not given SFU credentials
 */

data class PlaybackResponse (
    @Json(name = "playlist_url")
    val playlistUrl: kotlin.String,

    @Json(name = "reason")
    val reason: Reason,

    @Json(name = "type")
    val type: Type
)
{
    
    /**
    * Reason Enum
    */
    sealed class Reason(val value: kotlin.String) {
            override fun toString(): String = value

            companion object {
                fun fromString(s: kotlin.String): Reason = when (s) {
                    "hint" -> Hint
                    "percentage" -> Percentage
                    else -> Unknown(s)
                }
            }
            object Hint : Reason("hint")
            object Percentage : Reason("percentage")
            data class Unknown(val unknownValue: kotlin.String) : Reason(unknownValue)
        

        class ReasonAdapter : JsonAdapter<Reason>() {
            @FromJson
            override fun fromJson(reader: JsonReader): Reason? {
                val s = reader.nextString() ?: return null
                return Reason.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: Reason?) {
                writer.value(value?.value)
            }
        }
    }
    /**
    * Type Enum
    */
    sealed class Type(val value: kotlin.String) {
            override fun toString(): String = value

            companion object {
                fun fromString(s: kotlin.String): Type = when (s) {
                    "hls" -> Hls
                    else -> Unknown(s)
                }
            }
            object Hls : Type("hls")
            data class Unknown(val unknownValue: kotlin.String) : Type(unknownValue)
        

        class TypeAdapter : JsonAdapter<Type>() {
            @FromJson
            override fun fromJson(reader: JsonReader): Type? {
                val s = reader.nextString() ?: return null
                return Type.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: Type?) {
                writer.value(value?.value)
            }
        }
    }    
}
