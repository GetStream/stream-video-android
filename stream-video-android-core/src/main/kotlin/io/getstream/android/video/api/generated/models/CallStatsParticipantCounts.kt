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
data class CallStatsParticipantCounts (
    @Json(name = "live_sessions")
    val liveSessions: Int,

    @Json(name = "participants")
    val participants: Int,

    @Json(name = "peak_concurrent_sessions")
    val peakConcurrentSessions: Int,

    @Json(name = "peak_concurrent_users")
    val peakConcurrentUsers: Int,

    @Json(name = "publishers")
    val publishers: Int,

    @Json(name = "sessions")
    val sessions: Int,

    @Json(name = "sfus_used")
    val sfusUsed: Int,

    @Json(name = "average_jitter_ms")
    val averageJitterMs: Int? = null,

    @Json(name = "average_latency_ms")
    val averageLatencyMs: Int? = null,

    @Json(name = "avg_user_rating")
    val avgUserRating: Float? = null,

    @Json(name = "call_event_count")
    val callEventCount: Int? = null,

    @Json(name = "cq_score")
    val cqScore: Int? = null,

    @Json(name = "max_freezes_duration_ms")
    val maxFreezesDurationMs: Int? = null,

    @Json(name = "min_user_rating")
    val minUserRating: Int? = null,

    @Json(name = "total_participant_duration")
    val totalParticipantDuration: Int? = null
)
