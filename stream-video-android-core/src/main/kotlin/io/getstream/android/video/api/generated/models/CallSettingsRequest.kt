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
public data class CallSettingsRequest (
    @Json(name = "audio")
    public val audio: io.getstream.android.video.api.generated.models.AudioSettingsRequest? = null,

    @Json(name = "backstage")
    public val backstage: io.getstream.android.video.api.generated.models.BackstageSettingsRequest? = null,

    @Json(name = "broadcasting")
    public val broadcasting: io.getstream.android.video.api.generated.models.BroadcastSettingsRequest? = null,

    @Json(name = "encryption")
    public val encryption: io.getstream.android.video.api.generated.models.EncryptionSettingsRequest? = null,

    @Json(name = "frame_recording")
    public val frameRecording: io.getstream.android.video.api.generated.models.FrameRecordingSettingsRequest? = null,

    @Json(name = "geofencing")
    public val geofencing: io.getstream.android.video.api.generated.models.GeofenceSettingsRequest? = null,

    @Json(name = "individual_recording")
    public val individualRecording: io.getstream.android.video.api.generated.models.IndividualRecordingSettingsRequest? = null,

    @Json(name = "ingress")
    public val ingress: io.getstream.android.video.api.generated.models.IngressSettingsRequest? = null,

    @Json(name = "lifecycle")
    public val lifecycle: io.getstream.android.video.api.generated.models.LifecycleSettingsRequest? = null,

    @Json(name = "limits")
    public val limits: io.getstream.android.video.api.generated.models.LimitsSettingsRequest? = null,

    @Json(name = "raw_recording")
    public val rawRecording: io.getstream.android.video.api.generated.models.RawRecordingSettingsRequest? = null,

    @Json(name = "recording")
    public val recording: io.getstream.android.video.api.generated.models.RecordSettingsRequest? = null,

    @Json(name = "ring")
    public val ring: io.getstream.android.video.api.generated.models.RingSettingsRequest? = null,

    @Json(name = "screensharing")
    public val screensharing: io.getstream.android.video.api.generated.models.ScreensharingSettingsRequest? = null,

    @Json(name = "session")
    public val session: io.getstream.android.video.api.generated.models.SessionSettingsRequest? = null,

    @Json(name = "thumbnails")
    public val thumbnails: io.getstream.android.video.api.generated.models.ThumbnailsSettingsRequest? = null,

    @Json(name = "transcription")
    public val transcription: io.getstream.android.video.api.generated.models.TranscriptionSettingsRequest? = null,

    @Json(name = "video")
    public val video: io.getstream.android.video.api.generated.models.VideoSettingsRequest? = null
)
