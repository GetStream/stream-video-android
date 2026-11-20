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

package io.getstream.video.android.core.model

import com.google.common.truth.Truth.assertThat
import io.getstream.android.video.generated.infrastructure.Serializer
import io.getstream.android.video.generated.models.HLSPlaybackStats
import io.getstream.android.video.generated.models.HLSPlayerEvent
import io.getstream.android.video.generated.models.JoinCallRequest
import io.getstream.android.video.generated.models.PlaybackResponse
import io.getstream.android.video.generated.models.ReportHLSStatsRequest
import io.getstream.android.video.generated.models.StatsOptions
import org.junit.Test

class PlaybackSerializationTest {

    @Test
    fun `join request sends playback capabilities and prefer`() {
        val json = Serializer.moshi.adapter(JoinCallRequest::class.java).toJson(
            JoinCallRequest(
                location = "AMS",
                prefer = JoinCallRequest.Prefer.Hls,
                playbackCapabilities = listOf("hls"),
            ),
        )

        assertThat(json).contains("\"playback_capabilities\":[\"hls\"]")
        assertThat(json).contains("\"prefer\":\"hls\"")
    }

    @Test
    fun `playback response parses the HLS grant`() {
        val playback = Serializer.moshi.adapter(PlaybackResponse::class.java).fromJson(
            """{"type":"hls","playlist_url":"https://hls.example.com/playlist.m3u8","reason":"percentage"}""",
        )

        assertThat(playback).isEqualTo(
            PlaybackResponse(
                playlistUrl = "https://hls.example.com/playlist.m3u8",
                reason = PlaybackResponse.Reason.Percentage,
                type = PlaybackResponse.Type.Hls,
            ),
        )
    }

    @Test
    fun `playback response parses the stats token`() {
        val playback = Serializer.moshi.adapter(PlaybackResponse::class.java).fromJson(
            """{"type":"hls","playlist_url":"u","reason":"percentage","stats_token":"t"}""",
        )

        assertThat(playback?.statsToken).isEqualTo("t")
    }

    @Test
    fun `stats options parse the HLS stats settings`() {
        val options = Serializer.moshi.adapter(StatsOptions::class.java).fromJson(
            """{"enable_rtc_stats":true,"reporting_interval_ms":8000,"enable_hls_stats":true,"reporting_hls_interval_ms":5000}""",
        )

        assertThat(options?.enableHlsStats).isTrue()
        assertThat(options?.reportingHlsIntervalMs).isEqualTo(5000)
    }

    @Test
    fun `HLS stats report uses the backend field names`() {
        val json = Serializer.moshi.adapter(ReportHLSStatsRequest::class.java).toJson(
            ReportHLSStatsRequest(
                statsToken = "t",
                callSessionId = "c",
                userSessionId = "u",
                unifiedSessionId = "s",
                type = "ended",
                timestamp = 1L,
                endReason = "left",
                stats = HLSPlaybackStats(
                    watchDurationMs = 2L,
                    stallCount = 1,
                    stallDurationMs = 3L,
                    maxStallDurationMs = 3L,
                ),
                events = listOf(HLSPlayerEvent("stall_end", 4L, mapOf("duration_ms" to 3L))),
            ),
        )

        assertThat(json).contains("\"stats_token\":\"t\"")
        assertThat(json).contains("\"call_session_id\":\"c\"")
        assertThat(json).contains("\"user_session_id\":\"u\"")
        assertThat(json).contains("\"unified_session_id\":\"s\"")
        assertThat(json).contains("\"end_reason\":\"left\"")
        assertThat(json).contains("\"watch_duration_ms\":2")
        assertThat(json).contains("\"max_stall_duration_ms\":3")
        assertThat(json).contains("\"data\":{\"duration_ms\":3}")
        assertThat(json).doesNotContain("user_id")
    }

    @Test
    fun `playback response keeps unknown types and reasons`() {
        val playback = Serializer.moshi.adapter(PlaybackResponse::class.java).fromJson(
            """{"type":"dash","playlist_url":"u","reason":"threshold"}""",
        )

        assertThat(playback?.type).isEqualTo(PlaybackResponse.Type.Unknown("dash"))
        assertThat(playback?.reason).isEqualTo(PlaybackResponse.Reason.Unknown("threshold"))
    }
}
