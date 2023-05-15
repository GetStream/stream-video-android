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





import com.squareup.moshi.Json

/**
 *
 *
 * @param audioOnly
 * @param mode
 * @param quality
 */


data class RecordSettingsRequest (

    @Json(name = "audio_only")
    val audioOnly: kotlin.Boolean? = null,

    @Json(name = "mode")
    val mode: RecordSettingsRequest.Mode? = null,

    @Json(name = "quality")
    val quality: RecordSettingsRequest.Quality? = null

)

{

    /**
     *
     *
     * Values: available,disabled,autoOn
     */
    enum class Mode(val value: kotlin.String) {
        @Json(name = "available") available("available"),
        @Json(name = "disabled") disabled("disabled"),
        @Json(name = "auto-on") autoOn("auto-on");
    }
    /**
     *
     *
     * Values: audioOnly,_360p,_480p,_720p,_1080p,_1440p
     */
    enum class Quality(val value: kotlin.String) {
        @Json(name = "audio-only") audioOnly("audio-only"),
        @Json(name = "360p") _360p("360p"),
        @Json(name = "480p") _480p("480p"),
        @Json(name = "720p") _720p("720p"),
        @Json(name = "1080p") _1080p("1080p"),
        @Json(name = "1440p") _1440p("1440p");
    }

}
