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

data class FrameRecordingSettingsRequest (
    @Json(name = "capture_interval_in_seconds")
    val captureIntervalInSeconds: kotlin.Int,

    @Json(name = "mode")
    val mode: Mode,

    @Json(name = "quality")
    val quality: Quality? = null
)
{
    
    /**
    * Mode Enum
    */
    sealed class Mode(val value: kotlin.String) {
            override fun toString(): String = value

            companion object {
                fun fromString(s: kotlin.String): Mode = when (s) {
                    "auto-on" -> AutoOn
                    "available" -> Available
                    "disabled" -> Disabled
                    else -> Unknown(s)
                }
            }
            object AutoOn : Mode("auto-on")
            object Available : Mode("available")
            object Disabled : Mode("disabled")
            data class Unknown(val unknownValue: kotlin.String) : Mode(unknownValue)
        

        class ModeAdapter : JsonAdapter<Mode>() {
            @FromJson
            override fun fromJson(reader: JsonReader): Mode? {
                val s = reader.nextString() ?: return null
                return Mode.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: Mode?) {
                writer.value(value?.value)
            }
        }
    }
    /**
    * Quality Enum
    */
    sealed class Quality(val value: kotlin.String) {
            override fun toString(): String = value

            companion object {
                fun fromString(s: kotlin.String): Quality = when (s) {
                    "1080p" -> `1080p`
                    "1440p" -> `1440p`
                    "360p" -> `360p`
                    "480p" -> `480p`
                    "720p" -> `720p`
                    else -> Unknown(s)
                }
            }
            object `1080p` : Quality("1080p")
            object `1440p` : Quality("1440p")
            object `360p` : Quality("360p")
            object `480p` : Quality("480p")
            object `720p` : Quality("720p")
            data class Unknown(val unknownValue: kotlin.String) : Quality(unknownValue)
        

        class QualityAdapter : JsonAdapter<Quality>() {
            @FromJson
            override fun fromJson(reader: JsonReader): Quality? {
                val s = reader.nextString() ?: return null
                return Quality.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: Quality?) {
                writer.value(value?.value)
            }
        }
    }    
}
