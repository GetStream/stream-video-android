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
 * *
 * @param audio * @param backstage * @param geofencing * @param recording * @param ring * @param screensharing * @param transcription * @param video */

data class CallSettingsRequest(

    @Json(name = "audio")
    val audio: AudioSettingsRequest? = null,

    @Json(name = "backstage")
    val backstage: BackstageSettingsRequest? = null,

    @Json(name = "geofencing")
    val geofencing: GeofenceSettingsRequest? = null,

    @Json(name = "recording")
    val recording: RecordSettingsRequest? = null,

    @Json(name = "ring")
    val ring: RingSettingsRequest? = null,

    @Json(name = "screensharing")
    val screensharing: ScreensharingSettingsRequest? = null,

    @Json(name = "transcription")
    val transcription: TranscriptionSettingsRequest? = null,

    @Json(name = "video")
    val video: VideoSettingsRequest? = null

)
