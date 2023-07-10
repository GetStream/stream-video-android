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

import org.openapitools.client.models.TargetResolutionRequest




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
 * @param accessRequestEnabled
 * @param cameraDefaultOn
 * @param cameraFacing
 * @param enabled
 * @param targetResolution
 */


data class VideoSettingsRequest (

    @Json(name = "access_request_enabled")
    val accessRequestEnabled: kotlin.Boolean? = null,

    @Json(name = "camera_default_on")
    val cameraDefaultOn: kotlin.Boolean? = null,

    @Json(name = "camera_facing")
    val cameraFacing: VideoSettingsRequest.CameraFacing? = null,

    @Json(name = "enabled")
    val enabled: kotlin.Boolean? = null,

    @Json(name = "target_resolution")
    val targetResolution: TargetResolutionRequest? = null

)

{

    /**
     *
     *
     * Values: front,back,`external`
     */

    sealed class CameraFacing(val value: kotlin.String) {
        override fun toString(): String = value

        companion object {
            fun fromString(s: kotlin.String): CameraFacing = when (s) {
                "front" -> Front
                "back" -> Back
                "external" -> External
                else -> Unknown(s)
            }
        }

        object Front : CameraFacing("front")
        object Back : CameraFacing("back")
        object External : CameraFacing("external")
        data class Unknown(val unknownValue: kotlin.String) : CameraFacing(unknownValue)

        class CameraFacingAdapter : JsonAdapter<CameraFacing>() {
            @FromJson
            override fun fromJson(reader: JsonReader): CameraFacing? {
                val s = reader.nextString() ?: return null
                return fromString(s)
            }

            @ToJson
            override fun toJson(writer: JsonWriter, value: CameraFacing?) {
                writer.value(value?.value)
            }
        }
    }



}
