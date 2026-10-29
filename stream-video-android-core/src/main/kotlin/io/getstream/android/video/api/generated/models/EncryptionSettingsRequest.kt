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
public data class EncryptionSettingsRequest (
    @Json(name = "mode")
    public val mode: Mode? = null
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
}
