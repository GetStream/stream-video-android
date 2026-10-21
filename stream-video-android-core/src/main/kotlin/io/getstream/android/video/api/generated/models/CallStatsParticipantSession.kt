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
public data class CallStatsParticipantSession (
    @Json(name = "is_live")
    public val isLive: Boolean,

    @Json(name = "user_session_id")
    public val userSessionId: String,

    @Json(name = "published_tracks")
    public val publishedTracks: io.getstream.android.video.api.generated.models.PublishedTrackFlags,

    @Json(name = "browser")
    public val browser: String? = null,

    @Json(name = "browser_version")
    public val browserVersion: String? = null,

    @Json(name = "cq_score")
    public val cqScore: Int? = null,

    @Json(name = "current_ip")
    public val currentIp: String? = null,

    @Json(name = "current_sfu")
    public val currentSfu: String? = null,

    @Json(name = "distance_to_sfu_kilometers")
    public val distanceToSfuKilometers: Double? = null,

    @Json(name = "ended_at")
    public val endedAt: kotlin.time.Instant? = null,

    @Json(name = "freezes_duration_ms")
    public val freezesDurationMs: Int? = null,

    @Json(name = "ingress")
    public val ingress: String? = null,

    @Json(name = "jitter_ms")
    public val jitterMs: Int? = null,

    @Json(name = "latency_ms")
    public val latencyMs: Int? = null,

    @Json(name = "os")
    public val os: String? = null,

    @Json(name = "publisher_type")
    public val publisherType: String? = null,

    @Json(name = "sdk")
    public val sdk: String? = null,

    @Json(name = "sdk_version")
    public val sdkVersion: String? = null,

    @Json(name = "started_at")
    public val startedAt: kotlin.time.Instant? = null,

    @Json(name = "unified_session_id")
    public val unifiedSessionId: String? = null,

    @Json(name = "webrtc_version")
    public val webrtcVersion: String? = null,

    @Json(name = "location")
    public val location: io.getstream.android.video.api.generated.models.CallStatsLocation? = null
)
