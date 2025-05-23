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

/**
 *
 */

data class NetworkMetricsReportResponse(
    @Json(name = "average_connection_time")
    val averageConnectionTime: kotlin.Float? = null,

    @Json(name = "average_jitter")
    val averageJitter: kotlin.Float? = null,

    @Json(name = "average_latency")
    val averageLatency: kotlin.Float? = null,

    @Json(name = "average_time_to_reconnect")
    val averageTimeToReconnect: kotlin.Float? = null,
)
