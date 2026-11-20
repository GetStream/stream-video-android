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
 * A playback report from a viewer that join call sent to HLS playback
 */

@JsonClass(generateAdapter = true)
data class ReportHLSStatsRequest (
    @Json(name = "stats_token")
    val statsToken: kotlin.String,

    @Json(name = "call_session_id")
    val callSessionId: kotlin.String,

    @Json(name = "user_session_id")
    val userSessionId: kotlin.String,

    @Json(name = "unified_session_id")
    val unifiedSessionId: kotlin.String,

    @Json(name = "type")
    val type: kotlin.String,

    @Json(name = "timestamp")
    val timestamp: kotlin.Long,

    @Json(name = "sdk")
    val sdk: kotlin.String? = null,

    @Json(name = "sdk_version")
    val sdkVersion: kotlin.String? = null,

    @Json(name = "os")
    val os: kotlin.String? = null,

    @Json(name = "os_version")
    val osVersion: kotlin.String? = null,

    @Json(name = "end_reason")
    val endReason: kotlin.String? = null,

    @Json(name = "error_code")
    val errorCode: kotlin.String? = null,

    @Json(name = "stats")
    val stats: io.getstream.android.video.generated.models.HLSPlaybackStats? = null,

    @Json(name = "events")
    val events: kotlin.collections.List<io.getstream.android.video.generated.models.HLSPlayerEvent>? = null
)
