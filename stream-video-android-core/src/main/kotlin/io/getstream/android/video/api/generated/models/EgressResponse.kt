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
public data class EgressResponse (
    @Json(name = "broadcasting")
    public val broadcasting: Boolean,

    @Json(name = "rtmps")
    public val rtmps: List<io.getstream.android.video.api.generated.models.EgressRTMPResponse> = emptyList(),

    @Json(name = "composite_recording")
    public val compositeRecording: io.getstream.android.video.api.generated.models.CompositeRecordingResponse? = null,

    @Json(name = "frame_recording")
    public val frameRecording: io.getstream.android.video.api.generated.models.FrameRecordingResponse? = null,

    @Json(name = "hls")
    public val hls: io.getstream.android.video.api.generated.models.EgressHLSResponse? = null,

    @Json(name = "individual_recording")
    public val individualRecording: io.getstream.android.video.api.generated.models.IndividualRecordingResponse? = null,

    @Json(name = "raw_recording")
    public val rawRecording: io.getstream.android.video.api.generated.models.RawRecordingResponse? = null
)
