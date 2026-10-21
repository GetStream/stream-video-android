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
public data class QueryCallStatsMapResponse (
    @Json(name = "call_id")
    public val callId: String,

    @Json(name = "call_session_id")
    public val callSessionId: String,

    @Json(name = "call_type")
    public val callType: String,

    @Json(name = "duration")
    public val duration: String,

    @Json(name = "counts")
    public val counts: io.getstream.android.video.api.generated.models.CallStatsParticipantCounts,

    @Json(name = "call_ended_at")
    public val callEndedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "call_started_at")
    public val callStartedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "data_source")
    public val dataSource: String? = null,

    @Json(name = "end_time")
    public val endTime: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "generated_at")
    public val generatedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "start_time")
    public val startTime: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "publishers")
    public val publishers: io.getstream.android.video.api.generated.models.CallStatsMapPublishers? = null,

    @Json(name = "sfus")
    public val sfus: io.getstream.android.video.api.generated.models.CallStatsMapSFUs? = null,

    @Json(name = "subscribers")
    public val subscribers: io.getstream.android.video.api.generated.models.CallStatsMapSubscribers? = null
)
