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
data class StopLiveRequest (
    @Json(name = "continue_closed_caption")
    val continueClosedCaption: Boolean? = null,

    @Json(name = "continue_composite_recording")
    val continueCompositeRecording: Boolean? = null,

    @Json(name = "continue_hls")
    val continueHls: Boolean? = null,

    @Json(name = "continue_individual_recording")
    val continueIndividualRecording: Boolean? = null,

    @Json(name = "continue_raw_recording")
    val continueRawRecording: Boolean? = null,

    @Json(name = "continue_recording")
    val continueRecording: Boolean? = null,

    @Json(name = "continue_rtmp_broadcasts")
    val continueRtmpBroadcasts: Boolean? = null,

    @Json(name = "continue_transcription")
    val continueTranscription: Boolean? = null
)
