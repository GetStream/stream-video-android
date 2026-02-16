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

package io.getstream.android.video.generated.models

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

data class CallSettingsResponse (
    @Json(name = "audio")
    val audio: io.getstream.android.video.generated.models.AudioSettingsResponse,

    @Json(name = "backstage")
    val backstage: io.getstream.android.video.generated.models.BackstageSettingsResponse,

    @Json(name = "broadcasting")
    val broadcasting: io.getstream.android.video.generated.models.BroadcastSettingsResponse,

    @Json(name = "frame_recording")
    val frameRecording: io.getstream.android.video.generated.models.FrameRecordingSettingsResponse,

    @Json(name = "geofencing")
    val geofencing: io.getstream.android.video.generated.models.GeofenceSettingsResponse,

    @Json(name = "individual_recording")
    val individualRecording: io.getstream.android.video.generated.models.IndividualRecordingSettingsResponse,

    @Json(name = "limits")
    val limits: io.getstream.android.video.generated.models.LimitsSettingsResponse,

    @Json(name = "raw_recording")
    val rawRecording: io.getstream.android.video.generated.models.RawRecordingSettingsResponse,

    @Json(name = "recording")
    val recording: io.getstream.android.video.generated.models.RecordSettingsResponse,

    @Json(name = "ring")
    val ring: io.getstream.android.video.generated.models.RingSettingsResponse,

    @Json(name = "screensharing")
    val screensharing: io.getstream.android.video.generated.models.ScreensharingSettingsResponse,

    @Json(name = "session")
    val session: io.getstream.android.video.generated.models.SessionSettingsResponse,

    @Json(name = "thumbnails")
    val thumbnails: io.getstream.android.video.generated.models.ThumbnailsSettingsResponse,

    @Json(name = "transcription")
    val transcription: io.getstream.android.video.generated.models.TranscriptionSettingsResponse,

    @Json(name = "video")
    val video: io.getstream.android.video.generated.models.VideoSettingsResponse,

    @Json(name = "ingress")
    val ingress: io.getstream.android.video.generated.models.IngressSettingsResponse? = null
)
