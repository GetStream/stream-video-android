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
 * Basic response information
 */
data class QueryCallParticipantSessionsResponse (
    @Json(name = "call_id")
    val callId: String,

    @Json(name = "call_session_id")
    val callSessionId: String,

    @Json(name = "call_type")
    val callType: String,

    @Json(name = "duration")
    val duration: Int,

    @Json(name = "total_participant_duration")
    val totalParticipantDuration: Int,

    @Json(name = "total_participant_sessions")
    val totalParticipantSessions: Int,

    @Json(name = "participants_sessions")
    val participantsSessions: List<io.getstream.android.video.api.generated.models.ParticipantSessionDetails> = emptyList(),

    @Json(name = "next")
    val next: String? = null,

    @Json(name = "prev")
    val prev: String? = null,

    @Json(name = "session")
    val session: io.getstream.android.video.api.generated.models.CallSessionResponse? = null
)
