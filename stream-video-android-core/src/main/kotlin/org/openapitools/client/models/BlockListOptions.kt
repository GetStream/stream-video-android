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
 * @param behavior
 * @param blocklist
 */


data class BlockListOptions (

    @Json(name = "behavior")
    val behavior: BlockListOptions.Behavior,

    @Json(name = "blocklist")
    val blocklist: kotlin.String

)

{

    /**
     *
     *
     * Values: flag,block,shadowBlock
     */

    sealed class Behavior(val value: kotlin.String) {
        override fun toString(): String = value

        companion object {
            fun fromString(s: kotlin.String): Behavior = when (s) {
                "flag" -> Flag
                "block" -> Block
                "shadow_block" -> ShadowBlock
                else -> Unknown(s)
            }
        }

        object Flag : Behavior("flag")
        object Block : Behavior("block")
        object ShadowBlock : Behavior("shadow_block")
        data class Unknown(val unknownValue: kotlin.String) : Behavior(unknownValue)

        class BehaviorAdapter : JsonAdapter<Behavior>() {
            @FromJson
            override fun fromJson(reader: JsonReader): Behavior? {
                val s = reader.nextString() ?: return null
                return fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: Behavior?) {
                writer.value(value?.value)
            }
        }
    }



}
