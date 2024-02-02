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
 * @param audioOnly
 * @param mode
 * @param quality
 */


data class RecordSettingsRequest (

    @Json(name = "audio_only")
    val audioOnly: kotlin.Boolean? = null,

    @Json(name = "mode")
    val mode: RecordSettingsRequest.Mode? = null,

    @Json(name = "quality")
    val quality: RecordSettingsRequest.Quality? = null

)

{

    /**
     *
     *
     * Values: available,disabled,autoOn
     */

    sealed class Mode(val value: kotlin.String) {
        override fun toString(): String = value

        companion object {
            fun fromString(s: kotlin.String): Mode = when (s) {
                "available" -> Available
                "disabled" -> Disabled
                "auto-on" -> AutoOn
                else -> Unknown(s)
            }
        }

        object Available : Mode("available")
        object Disabled : Mode("disabled")
        object AutoOn : Mode("auto-on")
        data class Unknown(val unknownValue: kotlin.String) : Mode(unknownValue)

        class ModeAdapter : JsonAdapter<Mode>() {
            @FromJson
            override fun fromJson(reader: JsonReader): Mode? {
                val s = reader.nextString() ?: return null
                return fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: Mode?) {
                writer.value(value?.value)
            }
        }
    }


    /**
     *
     *
     * Values: audioOnly,_360p,_480p,_720p,_1080p,_1440p
     */

    sealed class Quality(val value: kotlin.String) {
        override fun toString(): String = value

        companion object {
            fun fromString(s: kotlin.String): Quality = when (s) {
                "audio-only" -> AudioOnly
                "360p" -> `360p`
                "480p" -> `480p`
                "720p" -> `720p`
                "1080p" -> `1080p`
                "1440p" -> `1440p`
                else -> Unknown(s)
            }
        }

        object AudioOnly : Quality("audio-only")
        object `360p` : Quality("360p")
        object `480p` : Quality("480p")
        object `720p` : Quality("720p")
        object `1080p` : Quality("1080p")
        object `1440p` : Quality("1440p")
        data class Unknown(val unknownValue: kotlin.String) : Quality(unknownValue)

        class QualityAdapter : JsonAdapter<Quality>() {
            @FromJson
            override fun fromJson(reader: JsonReader): Quality? {
                val s = reader.nextString() ?: return null
                return fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: Quality?) {
                writer.value(value?.value)
            }
        }
    }



}
