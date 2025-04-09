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
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * 
 */

data class AudioSettingsRequest(
    @Json(name = "default_device")
    val defaultDevice: DefaultDevice,

    @Json(name = "access_request_enabled")
    val accessRequestEnabled: kotlin.Boolean? = null,

    @Json(name = "mic_default_on")
    val micDefaultOn: kotlin.Boolean? = null,

    @Json(name = "opus_dtx_enabled")
    val opusDtxEnabled: kotlin.Boolean? = null,

    @Json(name = "redundant_coding_enabled")
    val redundantCodingEnabled: kotlin.Boolean? = null,

    @Json(name = "speaker_default_on")
    val speakerDefaultOn: kotlin.Boolean? = null,

    @Json(name = "noise_cancellation")
    val noiseCancellation: io.getstream.android.video.generated.models.NoiseCancellationSettings? = null
)
{
    
    /**
    * DefaultDevice Enum
    */
    sealed class DefaultDevice(val value: kotlin.String) {
        override fun toString(): String = value

        companion object {
            fun fromString(s: kotlin.String): DefaultDevice = when (s) {
                "earpiece" -> Earpiece
                "speaker" -> Speaker
                else -> Unknown(s)
            }
        }
        object Earpiece : DefaultDevice("earpiece")
        object Speaker : DefaultDevice("speaker")
        data class Unknown(val unknownValue: kotlin.String) : DefaultDevice(unknownValue)

        class DefaultDeviceAdapter : JsonAdapter<DefaultDevice>() {
            @FromJson
            override fun fromJson(reader: JsonReader): DefaultDevice? {
                val s = reader.nextString() ?: return null
                return DefaultDevice.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: DefaultDevice?) {
                writer.value(value?.value)
            }
        }
    }
}
