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

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * The ring state of a call session: who accepted, rejected, or missed the ring.
 */
data class GetCallRingStateResponse (
    @Json(name = "call_cid")
    val callCid: kotlin.String,

    @Json(name = "created_by_user_id")
    val createdByUserId: kotlin.String,

    @Json(name = "duration")
    val duration: kotlin.String,

    @Json(name = "session_id")
    val sessionId: kotlin.String,

    @Json(name = "accepted_by")
    val acceptedBy: kotlin.collections.Map<kotlin.String, org.threeten.bp.OffsetDateTime> = emptyMap(),

    @Json(name = "missed_by")
    val missedBy: kotlin.collections.Map<kotlin.String, org.threeten.bp.OffsetDateTime> = emptyMap(),

    @Json(name = "rejected_by")
    val rejectedBy: kotlin.collections.Map<kotlin.String, org.threeten.bp.OffsetDateTime> = emptyMap(),

    @Json(name = "call_ended_at")
    val callEndedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "session_ended_at")
    val sessionEndedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "session_started_at")
    val sessionStartedAt: org.threeten.bp.OffsetDateTime? = null
)
