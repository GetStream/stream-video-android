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
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * A full snapshot of the HLS player's stats, so the latest report replaces earlier ones
 */

@JsonClass(generateAdapter = true)
data class HLSPlaybackStats (
    @Json(name = "watch_duration_ms")
    val watchDurationMs: kotlin.Long,

    @Json(name = "stall_count")
    val stallCount: kotlin.Int,

    @Json(name = "stall_duration_ms")
    val stallDurationMs: kotlin.Long,

    @Json(name = "max_stall_duration_ms")
    val maxStallDurationMs: kotlin.Long,

    @Json(name = "dropped_frames")
    val droppedFrames: kotlin.Int? = null,

    @Json(name = "bitrate_kbps")
    val bitrateKbps: kotlin.Int? = null,

    @Json(name = "buffer_length_ms")
    val bufferLengthMs: kotlin.Int? = null,

    @Json(name = "live_latency_ms")
    val liveLatencyMs: kotlin.Int? = null,

    @Json(name = "width")
    val width: kotlin.Int? = null,

    @Json(name = "height")
    val height: kotlin.Int? = null
)
