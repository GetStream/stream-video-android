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
public data class AudioSettingsRequest (
    @Json(name = "default_device")
    public val defaultDevice: DefaultDevice,

    @Json(name = "access_request_enabled")
    public val accessRequestEnabled: Boolean? = null,

    @Json(name = "hifi_audio_enabled")
    public val hifiAudioEnabled: Boolean? = null,

    @Json(name = "mic_default_on")
    public val micDefaultOn: Boolean? = null,

    @Json(name = "opus_dtx_enabled")
    public val opusDtxEnabled: Boolean? = null,

    @Json(name = "redundant_coding_enabled")
    public val redundantCodingEnabled: Boolean? = null,

    @Json(name = "speaker_default_on")
    public val speakerDefaultOn: Boolean? = null,

    @Json(name = "noise_cancellation")
    public val noiseCancellation: io.getstream.android.video.api.generated.models.NoiseCancellationSettings? = null
)
{
    
    /**
    * DefaultDevice Enum
    */
    public sealed class DefaultDevice(public val value: String) {
            override fun toString(): String = value

            public companion object {
                public fun fromString(s: String): DefaultDevice = when (s) {
                    "earpiece" -> Earpiece
                    "speaker" -> Speaker
                    else -> Unknown(s)
                }
            }
            public object Earpiece : DefaultDevice("earpiece")
            public object Speaker : DefaultDevice("speaker")
            public data class Unknown(val unknownValue: String) : DefaultDevice(unknownValue)
        

        public class DefaultDeviceAdapter : JsonAdapter<DefaultDevice>() {
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
