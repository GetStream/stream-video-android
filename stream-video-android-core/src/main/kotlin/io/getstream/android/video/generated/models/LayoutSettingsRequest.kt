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

data class LayoutSettingsRequest (
    @Json(name = "name")
    val name: Name,

    @Json(name = "detect_orientation")
    val detectOrientation: kotlin.Boolean? = null,

    @Json(name = "external_app_url")
    val externalAppUrl: kotlin.String? = null,

    @Json(name = "external_css_url")
    val externalCssUrl: kotlin.String? = null,

    @Json(name = "options")
    val options: kotlin.collections.Map<kotlin.String, Any?>? = emptyMap()
)
{
    
    /**
    * Name Enum
    */
    sealed class Name(val value: kotlin.String) {
            override fun toString(): String = value

            companion object {
                fun fromString(s: kotlin.String): Name = when (s) {
                    "custom" -> Custom
                    "grid" -> Grid
                    "mobile" -> Mobile
                    "single-participant" -> SingleParticipant
                    "spotlight" -> Spotlight
                    else -> Unknown(s)
                }
            }
            object Custom : Name("custom")
            object Grid : Name("grid")
            object Mobile : Name("mobile")
            object SingleParticipant : Name("single-participant")
            object Spotlight : Name("spotlight")
            data class Unknown(val unknownValue: kotlin.String) : Name(unknownValue)
        

        class NameAdapter : JsonAdapter<Name>() {
            @FromJson
            override fun fromJson(reader: JsonReader): Name? {
                val s = reader.nextString() ?: return null
                return Name.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: Name?) {
                writer.value(value?.value)
            }
        }
    }    
}
