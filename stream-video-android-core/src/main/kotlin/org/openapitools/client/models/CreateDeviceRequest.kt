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

import org.openapitools.client.models.UserRequest




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
 * @param id
 * @param pushProvider
 * @param pushProviderName
 * @param user
 * @param userId
 */


data class CreateDeviceRequest (

    @Json(name = "id")
    val id: kotlin.String? = null,

    @Json(name = "push_provider")
    val pushProvider: CreateDeviceRequest.PushProvider? = null,

    @Json(name = "push_provider_name")
    val pushProviderName: kotlin.String? = null,

    @Json(name = "user")
    val user: UserRequest? = null,

    @Json(name = "user_id")
    val userId: kotlin.String? = null

)

{

    /**
     *
     *
     * Values: firebase,apn,huawei,xiaomi
     */

    sealed class PushProvider(val value: kotlin.String) {
        override fun toString(): String = value

        companion object {
            fun fromString(s: kotlin.String): PushProvider = when (s) {
                "firebase" -> Firebase
                "apn" -> Apn
                "huawei" -> Huawei
                "xiaomi" -> Xiaomi
                else -> Unknown(s)
            }
        }

        object Firebase : PushProvider("firebase")
        object Apn : PushProvider("apn")
        object Huawei : PushProvider("huawei")
        object Xiaomi : PushProvider("xiaomi")
        data class Unknown(val unknownValue: kotlin.String) : PushProvider(unknownValue)

        class PushProviderAdapter : JsonAdapter<PushProvider>() {
            @FromJson
            override fun fromJson(reader: JsonReader): PushProvider? {
                val s = reader.nextString() ?: return null
                return fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: PushProvider?) {
                writer.value(value?.value)
            }
        }
    }



}
