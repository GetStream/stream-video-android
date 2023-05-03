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
 * @param closedCaptionMode
 * @param mode
 */


data class TranscriptionSettings (

    @Json(name = "closed_caption_mode")
    val closedCaptionMode: kotlin.String,

    @Json(name = "mode")
    val mode: TranscriptionSettings.Mode

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



}
