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
public data class IngressVideoLayerRequest (
    @Json(name = "bitrate")
    public val bitrate: Int,

    @Json(name = "codec")
    public val codec: Codec,

    @Json(name = "frame_rate_limit")
    public val frameRateLimit: Int,

    @Json(name = "max_dimension")
    public val maxDimension: Int,

    @Json(name = "min_dimension")
    public val minDimension: Int
)
{
    
    /**
    * Codec Enum
    */
    public sealed class Codec(public val value: String) {
            override fun toString(): String = value

            public companion object {
                public fun fromString(s: String): Codec = when (s) {
                    "h264" -> H264
                    "vp8" -> Vp8
                    else -> Unknown(s)
                }
            }
            public object H264 : Codec("h264")
            public object Vp8 : Codec("vp8")
            public data class Unknown(val unknownValue: String) : Codec(unknownValue)
        

        public class CodecAdapter : JsonAdapter<Codec>() {
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
