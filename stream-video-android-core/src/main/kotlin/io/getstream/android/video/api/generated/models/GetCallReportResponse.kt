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
public data class GetCallReportResponse (
    @Json(name = "duration")
    public val duration: String,

    @Json(name = "session_id")
    public val sessionId: String,

    @Json(name = "report")
    public val report: io.getstream.android.video.api.generated.models.ReportResponse,

    @Json(name = "video_reactions")
    public val videoReactions: List<io.getstream.android.video.api.generated.models.VideoReactionsResponse>? = null,

    @Json(name = "chat_activity")
    public val chatActivity: io.getstream.android.video.api.generated.models.ChatActivityStatsResponse? = null,

    @Json(name = "digest")
    public val digest: io.getstream.android.video.api.generated.models.BroadcastDigest? = null,

    @Json(name = "session")
    public val session: io.getstream.android.video.api.generated.models.CallSessionResponse? = null
)
