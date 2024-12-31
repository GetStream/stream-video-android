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
    "UnusedImport"
)

package org.openapitools.client.models

import org.openapitools.client.models.CallParticipantResponse




import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.openapitools.client.infrastructure.Serializer

/**
 *
 *
 * @param acceptedBy
 * @param anonymousParticipantCount
 * @param id
 * @param missedBy
 * @param participants
 * @param participantsCountByRole
 * @param rejectedBy
 * @param endedAt
 * @param liveEndedAt
 * @param liveStartedAt
 * @param startedAt
 * @param timerEndsAt
 */


data class CallSessionResponse (

    @Json(name = "accepted_by")
    val acceptedBy: kotlin.collections.Map<kotlin.String, org.threeten.bp.OffsetDateTime>,

    @Json(name = "anonymous_participant_count")
    val anonymousParticipantCount: kotlin.Int,

    @Json(name = "id")
    val id: kotlin.String,

    @Json(name = "missed_by")
    val missedBy: kotlin.collections.Map<kotlin.String, org.threeten.bp.OffsetDateTime>,

    @Json(name = "participants")
    val participants: kotlin.collections.List<CallParticipantResponse>,

    @Json(name = "participants_count_by_role")
    val participantsCountByRole: kotlin.collections.Map<kotlin.String, kotlin.Int>,

    @Json(name = "rejected_by")
    val rejectedBy: kotlin.collections.Map<kotlin.String, org.threeten.bp.OffsetDateTime>,

    @Json(name = "ended_at")
    val endedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "live_ended_at")
    val liveEndedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "live_started_at")
    val liveStartedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "started_at")
    val startedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "timer_ends_at")
    val timerEndsAt: org.threeten.bp.OffsetDateTime? = null

)
