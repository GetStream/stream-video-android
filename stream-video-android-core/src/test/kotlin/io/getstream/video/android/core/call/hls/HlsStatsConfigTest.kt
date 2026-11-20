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

import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.models.JoinCallResponse
import io.getstream.android.video.generated.models.PlaybackResponse
import io.getstream.android.video.generated.models.StatsOptions
import io.getstream.result.Error
import io.mockk.every
import io.mockk.mockk
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class HlsStatsConfigTest {

    @Test
    fun `reads the session, token and HLS interval`() {
        val config = HlsStatsConfig.from(response(), "unified")!!

        assertThat(config.callSessionId).isEqualTo("call-session")
        assertThat(config.statsToken).isEqualTo("token")
        assertThat(config.unifiedSessionId).isEqualTo("unified")
        assertThat(config.intervalMs).isEqualTo(5_000)
    }

    @Test
    fun `is null unless HLS stats are enabled`() {
        assertThat(HlsStatsConfig.from(response(enabled = null), "unified")).isNull()
        assertThat(HlsStatsConfig.from(response(enabled = false), "unified")).isNull()
    }

    @Test
    fun `is null without a stats token or call session`() {
        assertThat(HlsStatsConfig.from(response(statsToken = null), "unified")).isNull()
        assertThat(HlsStatsConfig.from(response(statsToken = " "), "unified")).isNull()
        assertThat(HlsStatsConfig.from(response(callSessionId = ""), "unified")).isNull()
    }

    @Test
    fun `falls back to the RTC interval, then the default`() {
        assertThat(HlsStatsConfig.from(response(hlsIntervalMs = null), "u")!!.intervalMs)
            .isEqualTo(8_000)
        assertThat(
            HlsStatsConfig.from(response(hlsIntervalMs = 0, rtcIntervalMs = 0), "u")!!.intervalMs,
        ).isEqualTo(HlsStatsConfig.DEFAULT_INTERVAL_MS)
    }

    @Test
    fun `maps response codes to delivery results`() {
        assertThat(
            hlsStatsDeliveryResultOf(httpError(400)),
        ).isEqualTo(HlsStatsDeliveryResult.Rejected)
        assertThat(
            hlsStatsDeliveryResultOf(httpError(403)),
        ).isEqualTo(HlsStatsDeliveryResult.Forbidden)
        assertThat(
            hlsStatsDeliveryResultOf(httpError(429)),
        ).isEqualTo(HlsStatsDeliveryResult.Failed)
        assertThat(
            hlsStatsDeliveryResultOf(httpError(503)),
        ).isEqualTo(HlsStatsDeliveryResult.Failed)
        assertThat(
            hlsStatsDeliveryResultOf(
                Error.NetworkError("bad", serverErrorCode = 4, statusCode = 422),
            ),
        ).isEqualTo(HlsStatsDeliveryResult.Rejected)
        assertThat(hlsStatsDeliveryResultOf(Error.ThrowableError("offline", java.io.IOException())))
            .isEqualTo(HlsStatsDeliveryResult.Failed)
    }

    private fun httpError(code: Int): Error = Error.ThrowableError(
        "HTTP $code",
        HttpException(Response.error<Any>(code, "{}".toResponseBody())),
    )

    private fun response(
        enabled: Boolean? = true,
        statsToken: String? = "token",
        callSessionId: String = "call-session",
        hlsIntervalMs: Int? = 5_000,
        rtcIntervalMs: Int = 8_000,
    ): JoinCallResponse {
        val playback =
            mockk<PlaybackResponse> { every { this@mockk.statsToken } returns statsToken }
        return mockk {
            every { statsOptions } returns StatsOptions(
                enableRtcStats = true,
                reportingIntervalMs = rtcIntervalMs,
                enableHlsStats = enabled,
                reportingHlsIntervalMs = hlsIntervalMs,
            )
            every { this@mockk.playback } returns playback
            every { call.currentSessionId } returns callSessionId
        }
    }
}
