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

data class IngressVideoLayerRequest (
    @Json(name = "bitrate")
    val bitrate: kotlin.Int,

    @Json(name = "codec")
    val codec: Codec,

    @Json(name = "frame_rate_limit")
    val frameRateLimit: kotlin.Int,

    @Json(name = "max_dimension")
    val maxDimension: kotlin.Int,

    @Json(name = "min_dimension")
    val minDimension: kotlin.Int
)
{
    
    /**
    * Codec Enum
    */
    sealed class Codec(val value: kotlin.String) {
            override fun toString(): String = value

            companion object {
                fun fromString(s: kotlin.String): Codec = when (s) {
                    "h264" -> H264
                    "vp8" -> Vp8
                    else -> Unknown(s)
                }
            }
            object H264 : Codec("h264")
            object Vp8 : Codec("vp8")
            data class Unknown(val unknownValue: kotlin.String) : Codec(unknownValue)
        

        class CodecAdapter : JsonAdapter<Codec>() {
            @FromJson
            override fun fromJson(reader: JsonReader): Codec? {
                val s = reader.nextString() ?: return null
                return Codec.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: Codec?) {
                writer.value(value?.value)
            }
        }
    }    
}
