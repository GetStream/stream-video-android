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
 * 
 */

data class CallStatsParticipantSession (
    @Json(name = "is_live")
    val isLive: kotlin.Boolean,

    @Json(name = "user_session_id")
    val userSessionId: kotlin.String,

    @Json(name = "published_tracks")
    val publishedTracks: io.getstream.android.video.generated.models.PublishedTrackFlags,

    @Json(name = "browser")
    val browser: kotlin.String? = null,

    @Json(name = "browser_version")
    val browserVersion: kotlin.String? = null,

    @Json(name = "cq_score")
    val cqScore: kotlin.Int? = null,

    @Json(name = "current_ip")
    val currentIp: kotlin.String? = null,

    @Json(name = "current_sfu")
    val currentSfu: kotlin.String? = null,

    @Json(name = "distance_to_sfu_kilometers")
    val distanceToSfuKilometers: kotlin.Float? = null,

    @Json(name = "ended_at")
    val endedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "publisher_type")
    val publisherType: kotlin.String? = null,

    @Json(name = "sdk")
    val sdk: kotlin.String? = null,

    @Json(name = "sdk_version")
    val sdkVersion: kotlin.String? = null,

    @Json(name = "started_at")
    val startedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "unified_session_id")
    val unifiedSessionId: kotlin.String? = null,

    @Json(name = "webrtc_version")
    val webrtcVersion: kotlin.String? = null,

    @Json(name = "location")
    val location: io.getstream.android.video.generated.models.CallStatsLocation? = null
)
