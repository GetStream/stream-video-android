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
public data class RecordSettingsRequest (
    @Json(name = "mode")
    public val mode: Mode,

    @Json(name = "audio_only")
    public val audioOnly: Boolean? = null,

    @Json(name = "quality")
    public val quality: Quality? = null
)
{
    
    /**
    * Mode Enum
    */
    public sealed class Mode(public val value: String) {
            override fun toString(): String = value

            public companion object {
                public fun fromString(s: String): Mode = when (s) {
                    "auto-on" -> AutoOn
                    "available" -> Available
                    "disabled" -> Disabled
                    else -> Unknown(s)
                }
            }
            public object AutoOn : Mode("auto-on")
            public object Available : Mode("available")
            public object Disabled : Mode("disabled")
            public data class Unknown(val unknownValue: String) : Mode(unknownValue)
        

        public class ModeAdapter : JsonAdapter<Mode>() {
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
    public sealed class Quality(public val value: String) {
            override fun toString(): String = value

            public companion object {
                public fun fromString(s: String): Quality = when (s) {
                    "portrait-1080x1920" -> Portrait1080x1920
                    "portrait-1440x2560" -> Portrait1440x2560
                    "portrait-360x640" -> Portrait360x640
                    "portrait-480x854" -> Portrait480x854
                    "portrait-720x1280" -> Portrait720x1280
                    "1080p" -> Quality1080p
                    "1440p" -> Quality1440p
                    "360p" -> Quality360p
                    "480p" -> Quality480p
                    "720p" -> Quality720p
                    else -> Unknown(s)
                }
            }
            public object Portrait1080x1920 : Quality("portrait-1080x1920")
            public object Portrait1440x2560 : Quality("portrait-1440x2560")
            public object Portrait360x640 : Quality("portrait-360x640")
            public object Portrait480x854 : Quality("portrait-480x854")
            public object Portrait720x1280 : Quality("portrait-720x1280")
            public object Quality1080p : Quality("1080p")
            public object Quality1440p : Quality("1440p")
            public object Quality360p : Quality("360p")
            public object Quality480p : Quality("480p")
            public object Quality720p : Quality("720p")
            public data class Unknown(val unknownValue: String) : Quality(unknownValue)
        

        public class QualityAdapter : JsonAdapter<Quality>() {
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
