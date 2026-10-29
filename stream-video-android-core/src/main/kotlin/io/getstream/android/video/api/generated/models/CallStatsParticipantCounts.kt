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
public data class CallStatsParticipantCounts (
    @Json(name = "live_sessions")
    public val liveSessions: Int,

    @Json(name = "participants")
    public val participants: Int,

    @Json(name = "peak_concurrent_sessions")
    public val peakConcurrentSessions: Int,

    @Json(name = "peak_concurrent_users")
    public val peakConcurrentUsers: Int,

    @Json(name = "publishers")
    public val publishers: Int,

    @Json(name = "sessions")
    public val sessions: Int,

    @Json(name = "sfus_used")
    public val sfusUsed: Int,

    @Json(name = "average_jitter_ms")
    public val averageJitterMs: Int? = null,

    @Json(name = "average_latency_ms")
    public val averageLatencyMs: Int? = null,

    @Json(name = "avg_user_rating")
    public val avgUserRating: Double? = null,

    @Json(name = "call_event_count")
    public val callEventCount: Int? = null,

    @Json(name = "cq_score")
    public val cqScore: Int? = null,

    @Json(name = "max_freezes_duration_ms")
    public val maxFreezesDurationMs: Int? = null,

    @Json(name = "min_user_rating")
    public val minUserRating: Int? = null,

    @Json(name = "total_participant_duration")
    public val totalParticipantDuration: Int? = null
)
