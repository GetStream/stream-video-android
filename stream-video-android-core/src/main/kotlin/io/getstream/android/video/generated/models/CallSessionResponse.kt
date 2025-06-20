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
import kotlin.collections.List
import kotlin.collections.Map

/**
 *
 */

data class CallSessionResponse(
    @Json(name = "anonymous_participant_count")
    val anonymousParticipantCount: kotlin.Int,

    @Json(name = "id")
    val id: kotlin.String,

    @Json(name = "participants")
    val participants: kotlin.collections.List<io.getstream.android.video.generated.models.CallParticipantResponse> = emptyList(),

    @Json(name = "accepted_by")
    val acceptedBy: kotlin.collections.Map<kotlin.String, org.threeten.bp.OffsetDateTime> = emptyMap(),

    @Json(name = "missed_by")
    val missedBy: kotlin.collections.Map<kotlin.String, org.threeten.bp.OffsetDateTime> = emptyMap(),

    @Json(name = "participants_count_by_role")
    val participantsCountByRole: kotlin.collections.Map<kotlin.String, kotlin.Int> = emptyMap(),

    @Json(name = "rejected_by")
    val rejectedBy: kotlin.collections.Map<kotlin.String, org.threeten.bp.OffsetDateTime> = emptyMap(),

    @Json(name = "ended_at")
    val endedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "live_ended_at")
    val liveEndedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "live_started_at")
    val liveStartedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "started_at")
    val startedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "timer_ends_at")
    val timerEndsAt: org.threeten.bp.OffsetDateTime? = null,
)
