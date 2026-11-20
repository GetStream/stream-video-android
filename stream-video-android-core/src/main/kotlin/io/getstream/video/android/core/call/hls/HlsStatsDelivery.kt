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

package io.getstream.video.android.core.call.hls

import io.getstream.android.video.generated.models.ReportHLSStatsRequest
import io.getstream.result.Error
import io.getstream.result.Result
import io.getstream.video.android.core.StreamVideoClient
import retrofit2.HttpException

/** How the backend answered an HLS stats report. */
internal enum class HlsStatsDeliveryResult {
    /** Accepted (2xx). */
    Delivered,

    /** Invalid report (4xx other than 403 and 429). Sending it again cannot succeed. */
    Rejected,

    /** The stats token is invalid, expired, or for another session or user (403). */
    Forbidden,

    /** Rate limited, a server error, or no response. Worth retrying. */
    Failed,
}

/** Posts one HLS stats report. */
internal fun interface HlsStatsDelivery {
    suspend fun deliver(request: ReportHLSStatsRequest): HlsStatsDeliveryResult
}

/** Posts reports for the call [type]:[id] to `POST /video/call/{type}/{id}/hls_stats`. */
internal class CoordinatorHlsStatsDelivery(
    private val client: StreamVideoClient,
    private val type: String,
    private val id: String,
) : HlsStatsDelivery {

    override suspend fun deliver(request: ReportHLSStatsRequest): HlsStatsDeliveryResult =
        when (val result = client.reportHlsStats(type, id, request)) {
            is Result.Success -> HlsStatsDeliveryResult.Delivered
            is Result.Failure -> hlsStatsDeliveryResultOf(result.value)
        }
}

internal fun hlsStatsDeliveryResultOf(error: Error): HlsStatsDeliveryResult {
    val statusCode = when (error) {
        is Error.NetworkError -> error.statusCode
        is Error.ThrowableError -> (error.cause as? HttpException)?.code()
        else -> null
    }
    return when {
        statusCode == null -> HlsStatsDeliveryResult.Failed
        statusCode == 403 -> HlsStatsDeliveryResult.Forbidden
        statusCode == 429 || statusCode >= 500 -> HlsStatsDeliveryResult.Failed
        statusCode in 400..499 -> HlsStatsDeliveryResult.Rejected
        else -> HlsStatsDeliveryResult.Failed
    }
}
