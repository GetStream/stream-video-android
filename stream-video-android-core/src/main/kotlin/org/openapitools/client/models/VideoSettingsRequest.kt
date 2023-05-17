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




import com.squareup.moshi.Json

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
    enum class CameraFacing(val value: kotlin.String) {
        @Json(name = "front") front("front"),
        @Json(name = "back") back("back"),
        @Json(name = "external") `external`("external");
    }

}
