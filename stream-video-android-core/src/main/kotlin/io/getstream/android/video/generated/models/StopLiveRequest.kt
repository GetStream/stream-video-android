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
    "UnusedImport",
)

package io.getstream.android.video.generated.models

import com.squareup.moshi.Json

/**
 *
 */

data class StopLiveRequest(
    @Json(name = "continue_closed_caption")
    val continueClosedCaption: kotlin.Boolean? = null,

    @Json(name = "continue_hls")
    val continueHls: kotlin.Boolean? = null,

    @Json(name = "continue_recording")
    val continueRecording: kotlin.Boolean? = null,

    @Json(name = "continue_rtmp_broadcasts")
    val continueRtmpBroadcasts: kotlin.Boolean? = null,

    @Json(name = "continue_transcription")
    val continueTranscription: kotlin.Boolean? = null,
)
