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
public data class VideoSettingsRequest (
    @Json(name = "access_request_enabled")
    public val accessRequestEnabled: Boolean? = null,

    @Json(name = "camera_default_on")
    public val cameraDefaultOn: Boolean? = null,

    @Json(name = "camera_facing")
    public val cameraFacing: CameraFacing? = null,

    @Json(name = "enabled")
    public val enabled: Boolean? = null,

    @Json(name = "target_resolution")
    public val targetResolution: io.getstream.android.video.api.generated.models.TargetResolution? = null
)
{
    
    /**
    * CameraFacing Enum
    */
    public sealed class CameraFacing(public val value: String) {
            override fun toString(): String = value

            public companion object {
                public fun fromString(s: String): CameraFacing = when (s) {
                    "back" -> Back
                    "external" -> External
                    "front" -> Front
                    else -> Unknown(s)
                }
            }
            public object Back : CameraFacing("back")
            public object External : CameraFacing("external")
            public object Front : CameraFacing("front")
            public data class Unknown(val unknownValue: String) : CameraFacing(unknownValue)
        

        public class CameraFacingAdapter : JsonAdapter<CameraFacing>() {
            @FromJson
            override fun fromJson(reader: JsonReader): CameraFacing? {
                val s = reader.nextString() ?: return null
                return CameraFacing.fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: CameraFacing?) {
                writer.value(value?.value)
            }
        }
    }    
}
