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
 * Create device request
 */
public data class CreateDeviceRequest (
    @Json(name = "id")
    public val id: String,

    @Json(name = "push_provider")
    public val pushProvider: PushProvider,

    @Json(name = "hardware_id")
    public val hardwareId: String? = null,

    @Json(name = "push_provider_name")
    public val pushProviderName: String? = null,

    @Json(name = "voip_token")
    public val voipToken: Boolean? = null
)
{
    
    /**
    * PushProvider Enum
    */
    public sealed class PushProvider(public val value: String) {
            override fun toString(): String = value

            public companion object {
                public fun fromString(s: String): PushProvider = when (s) {
                    "apn" -> Apn
                    "firebase" -> Firebase
                    "huawei" -> Huawei
                    "xiaomi" -> Xiaomi
                    else -> Unknown(s)
                }
            }
            public object Apn : PushProvider("apn")
            public object Firebase : PushProvider("firebase")
            public object Huawei : PushProvider("huawei")
            public object Xiaomi : PushProvider("xiaomi")
            public data class Unknown(val unknownValue: String) : PushProvider(unknownValue)
        

        public class PushProviderAdapter : JsonAdapter<PushProvider>() {
            @FromJson
            override fun fromJson(reader: JsonReader): PushProvider? {
                val s = reader.nextString() ?: return null
                return PushProvider.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: PushProvider?) {
                writer.value(value?.value)
            }
        }
    }    
}
