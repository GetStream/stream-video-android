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
 * CallTranscription represents a transcription of a call.
 */

data class CallTranscription(
    @Json(name = "end_time")
    val endTime: org.threeten.bp.OffsetDateTime,

    @Json(name = "filename")
    val filename: kotlin.String,

    @Json(name = "start_time")
    val startTime: org.threeten.bp.OffsetDateTime,

    @Json(name = "url")
    val url: kotlin.String,
)
