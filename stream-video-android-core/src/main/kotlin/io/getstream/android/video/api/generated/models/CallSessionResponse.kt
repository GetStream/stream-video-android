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
public data class CallSessionResponse (
    @Json(name = "anonymous_participant_count")
    public val anonymousParticipantCount: Int,

    @Json(name = "id")
    public val id: String,

    @Json(name = "participants")
    public val participants: List<io.getstream.android.video.api.generated.models.CallParticipantResponse> = emptyList(),

    @Json(name = "accepted_by")
    public val acceptedBy: Map<String, kotlin.time.Instant> = emptyMap(),

    @Json(name = "missed_by")
    public val missedBy: Map<String, kotlin.time.Instant> = emptyMap(),

    @Json(name = "participants_count_by_role")
    public val participantsCountByRole: Map<String, Int> = emptyMap(),

    @Json(name = "rejected_by")
    public val rejectedBy: Map<String, kotlin.time.Instant> = emptyMap(),

    @Json(name = "ended_at")
    public val endedAt: kotlin.time.Instant? = null,

    @Json(name = "live_ended_at")
    public val liveEndedAt: kotlin.time.Instant? = null,

    @Json(name = "live_started_at")
    public val liveStartedAt: kotlin.time.Instant? = null,

    @Json(name = "started_at")
    public val startedAt: kotlin.time.Instant? = null,

    @Json(name = "timer_ends_at")
    public val timerEndsAt: kotlin.time.Instant? = null
)
