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

import org.openapitools.client.models.AudioSettingsResponse
import org.openapitools.client.models.BackstageSettingsResponse
import org.openapitools.client.models.BroadcastSettingsResponse
import org.openapitools.client.models.GeofenceSettingsResponse
import org.openapitools.client.models.LimitsSettingsResponse
import org.openapitools.client.models.RecordSettingsResponse
import org.openapitools.client.models.RingSettingsResponse
import org.openapitools.client.models.ScreensharingSettingsResponse
import org.openapitools.client.models.ThumbnailsSettingsResponse
import org.openapitools.client.models.TranscriptionSettingsResponse
import org.openapitools.client.models.VideoSettingsResponse




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
 * @param audio
 * @param backstage
 * @param broadcasting
 * @param geofencing
 * @param limits
 * @param recording
 * @param ring
 * @param screensharing
 * @param thumbnails
 * @param transcription
 * @param video
 */


data class CallSettingsResponse (

    @Json(name = "audio")
    val audio: AudioSettingsResponse,

    @Json(name = "backstage")
    val backstage: BackstageSettingsResponse,

    @Json(name = "broadcasting")
    val broadcasting: BroadcastSettingsResponse,

    @Json(name = "geofencing")
    val geofencing: GeofenceSettingsResponse,

    @Json(name = "limits")
    val limits: LimitsSettingsResponse,

    @Json(name = "recording")
    val recording: RecordSettingsResponse,

    @Json(name = "ring")
    val ring: RingSettingsResponse,

    @Json(name = "screensharing")
    val screensharing: ScreensharingSettingsResponse,

    @Json(name = "thumbnails")
    val thumbnails: ThumbnailsSettingsResponse,

    @Json(name = "transcription")
    val transcription: TranscriptionSettingsResponse,

    @Json(name = "video")
    val video: VideoSettingsResponse

)
