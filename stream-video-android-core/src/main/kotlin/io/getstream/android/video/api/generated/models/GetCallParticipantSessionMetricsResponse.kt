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
data class GetCallParticipantSessionMetricsResponse (
    @Json(name = "duration")
    val duration: String,

    @Json(name = "is_publisher")
    val isPublisher: Boolean? = null,

    @Json(name = "is_subscriber")
    val isSubscriber: Boolean? = null,

    @Json(name = "joined_at")
    val joinedAt: org.threeten.bp.OffsetDateTime? = null,

    @Json(name = "publisher_type")
    val publisherType: String? = null,

    @Json(name = "user_id")
    val userId: String? = null,

    @Json(name = "user_session_id")
    val userSessionId: String? = null,

    @Json(name = "published_tracks")
    val publishedTracks: List<io.getstream.android.video.api.generated.models.PublishedTrackMetrics>? = null,

    @Json(name = "client")
    val client: io.getstream.android.video.api.generated.models.SessionClient? = null
)
