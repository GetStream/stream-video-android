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

import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.*
import kotlin.io.*
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 *
 */

data class CallSettingsRequest (
    @Json(name = "audio")
    val audio: org.openapitools.client.models.AudioSettingsRequest? = null,

    @Json(name = "backstage")
    val backstage: org.openapitools.client.models.BackstageSettingsRequest? = null,

    @Json(name = "broadcasting")
    val broadcasting: org.openapitools.client.models.BroadcastSettingsRequest? = null,

    @Json(name = "geofencing")
    val geofencing: org.openapitools.client.models.GeofenceSettingsRequest? = null,

    @Json(name = "limits")
    val limits: org.openapitools.client.models.LimitsSettingsRequest? = null,

    @Json(name = "recording")
    val recording: org.openapitools.client.models.RecordSettingsRequest? = null,

    @Json(name = "ring")
    val ring: org.openapitools.client.models.RingSettingsRequest? = null,

    @Json(name = "screensharing")
    val screensharing: org.openapitools.client.models.ScreensharingSettingsRequest? = null,

    @Json(name = "session")
    val session: org.openapitools.client.models.SessionSettingsRequest? = null,

    @Json(name = "thumbnails")
    val thumbnails: org.openapitools.client.models.ThumbnailsSettingsRequest? = null,

    @Json(name = "transcription")
    val transcription: org.openapitools.client.models.TranscriptionSettingsRequest? = null,

    @Json(name = "video")
    val video: org.openapitools.client.models.VideoSettingsRequest? = null
)
