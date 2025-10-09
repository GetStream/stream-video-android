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
 * Basic response information
 */

data class QueryCallSessionParticipantStatsResponse (
    @Json(name = "call_id")
    val callId: kotlin.String,

    @Json(name = "call_session_id")
    val callSessionId: kotlin.String,

    @Json(name = "call_type")
    val callType: kotlin.String,

    @Json(name = "duration")
    val duration: kotlin.String,

    @Json(name = "participants")
    val participants: kotlin.collections.List<io.getstream.android.video.generated.models.CallStatsParticipant> = emptyList(),

    @Json(name = "counts")
    val counts: io.getstream.android.video.generated.models.CallStatsParticipantCounts,

    @Json(name = "call_ended_at")
    val callEndedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "call_started_at")
    val callStartedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "next")
    val next: kotlin.String? = null,

    @Json(name = "prev")
    val prev: kotlin.String? = null,

    @Json(name = "tmp_data_source")
    val tmpDataSource: kotlin.String? = null
)
