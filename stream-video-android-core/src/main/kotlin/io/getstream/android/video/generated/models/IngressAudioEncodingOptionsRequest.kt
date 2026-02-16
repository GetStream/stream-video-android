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

data class IngressAudioEncodingOptionsRequest (
    @Json(name = "bitrate")
    val bitrate: kotlin.Int,

    @Json(name = "channels")
    val channels: Channels,

    @Json(name = "enable_dtx")
    val enableDtx: kotlin.Boolean? = null
)
{
    
    /**
    * Channels Enum
    */
    sealed class Channels(val value: kotlin.String) {
            override fun toString(): String = value

            companion object {
                fun fromString(s: kotlin.String): Channels = when (s) {
                    "1" -> Channels1
                    "2" -> Channels2
                    else -> Unknown(s)
                }
            }
            object Channels1 : Channels("1")
            object Channels2 : Channels("2")
            data class Unknown(val unknownValue: kotlin.String) : Channels(unknownValue)
        

        class ChannelsAdapter : JsonAdapter<Channels>() {
            @FromJson
            override fun fromJson(reader: JsonReader): Channels? {
                val s = reader.nextString() ?: return null
                return Channels.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: Channels?) {
                writer.value(value?.value)
            }
        }
    }    
}
