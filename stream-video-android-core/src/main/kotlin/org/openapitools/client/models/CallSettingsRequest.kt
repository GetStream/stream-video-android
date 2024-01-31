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

import org.openapitools.client.models.AudioSettingsRequest
import org.openapitools.client.models.BackstageSettingsRequest
import org.openapitools.client.models.BroadcastSettingsRequest
import org.openapitools.client.models.GeofenceSettingsRequest
import org.openapitools.client.models.RecordSettingsRequest
import org.openapitools.client.models.RingSettingsRequest
import org.openapitools.client.models.ScreensharingSettingsRequest
import org.openapitools.client.models.TranscriptionSettingsRequest
import org.openapitools.client.models.VideoSettingsRequest




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
 * @param recording
 * @param ring
 * @param screensharing
 * @param transcription
 * @param video
 */


data class CallSettingsRequest (

    @Json(name = "audio")
    val audio: AudioSettingsRequest? = null,

    @Json(name = "backstage")
    val backstage: BackstageSettingsRequest? = null,

    @Json(name = "broadcasting")
    val broadcasting: BroadcastSettingsRequest? = null,

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
