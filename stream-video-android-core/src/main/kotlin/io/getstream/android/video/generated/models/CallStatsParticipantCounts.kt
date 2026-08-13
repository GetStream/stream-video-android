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

data class CallStatsParticipantCounts (
    @Json(name = "live_sessions")
    val liveSessions: kotlin.Int,

    @Json(name = "participants")
    val participants: kotlin.Int,

    @Json(name = "peak_concurrent_sessions")
    val peakConcurrentSessions: kotlin.Int,

    @Json(name = "peak_concurrent_users")
    val peakConcurrentUsers: kotlin.Int,

    @Json(name = "publishers")
    val publishers: kotlin.Int,

    @Json(name = "sessions")
    val sessions: kotlin.Int,

    @Json(name = "sfus_used")
    val sfusUsed: kotlin.Int,

    @Json(name = "average_jitter_ms")
    val averageJitterMs: kotlin.Int? = null,

    @Json(name = "average_latency_ms")
    val averageLatencyMs: kotlin.Int? = null,

    @Json(name = "call_event_count")
    val callEventCount: kotlin.Int? = null,

    @Json(name = "cq_score")
    val cqScore: kotlin.Int? = null,

    @Json(name = "max_freezes_duration_ms")
    val maxFreezesDurationMs: kotlin.Int? = null,

    @Json(name = "total_participant_duration")
    val totalParticipantDuration: kotlin.Int? = null
)
