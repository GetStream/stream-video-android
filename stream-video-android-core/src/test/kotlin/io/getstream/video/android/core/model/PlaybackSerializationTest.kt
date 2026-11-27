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
import io.getstream.android.video.generated.models.JoinCallRequest
import io.getstream.android.video.generated.models.JoinCallResponse
import io.getstream.android.video.generated.models.PlaybackResponse
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
    fun `playback response keeps unknown types and reasons`() {
        val playback = Serializer.moshi.adapter(PlaybackResponse::class.java).fromJson(
            """{"type":"dash","playlist_url":"u","reason":"threshold"}""",
        )

        assertThat(playback?.type).isEqualTo(PlaybackResponse.Type.Unknown("dash"))
        assertThat(playback?.reason).isEqualTo(PlaybackResponse.Reason.Unknown("threshold"))
    }

    @Test
    fun `join response with an HLS grant parses with empty SFU credentials`() {
        // With an HLS grant the backend sends credentials with empty strings and no ICE servers.
        val response = Serializer.moshi.adapter(JoinCallResponse::class.java).fromJson(
            """
            {
              "created": false,
              "duration": "12.34ms",
              "members": [],
              "own_capabilities": ["join-call", "read-call"],
              "call": {
                "id": "test-call",
                "type": "livestream",
                "cid": "livestream:test-call",
                "backstage": false,
                "captioning": false,
                "recording": false,
                "transcribing": false,
                "translating": false,
                "current_session_id": "session-id",
                "created_at": "2026-11-20T10:00:00.000000Z",
                "updated_at": "2026-11-20T10:05:00.000000Z",
                "blocked_user_ids": [],
                "custom": {},
                "created_by": {
                  "id": "host",
                  "language": "en",
                  "role": "user",
                  "created_at": "2026-11-01T10:00:00.000000Z",
                  "updated_at": "2026-11-01T10:00:00.000000Z",
                  "custom": {},
                  "teams": []
                },
                "egress": { "broadcasting": true, "rtmps": [] },
                "ingress": {
                  "rtmp": { "address": "" },
                  "srt": { "address": "" },
                  "whip": { "address": "" }
                },
                "settings": {
                  "audio": {
                    "access_request_enabled": false,
                    "default_device": "speaker",
                    "hifi_audio_enabled": false,
                    "mic_default_on": false,
                    "opus_dtx_enabled": true,
                    "redundant_coding_enabled": true,
                    "speaker_default_on": true
                  },
                  "backstage": { "enabled": true },
                  "broadcasting": {
                    "enabled": true,
                    "hls": { "auto_on": false, "enabled": true, "quality_tracks": ["720p"] },
                    "rtmp": { "enabled": false, "quality": "720p" }
                  },
                  "frame_recording": { "capture_interval_in_seconds": 0, "mode": "available" },
                  "geofencing": { "names": [] },
                  "individual_recording": { "mode": "available" },
                  "limits": {},
                  "raw_recording": { "mode": "available" },
                  "recording": { "audio_only": false, "mode": "available", "quality": "720p" },
                  "ring": {
                    "auto_cancel_timeout_ms": 0,
                    "incoming_call_timeout_ms": 0,
                    "missed_call_timeout_ms": 0
                  },
                  "screensharing": { "access_request_enabled": false, "enabled": true },
                  "session": { "inactivity_timeout_seconds": 0 },
                  "thumbnails": { "enabled": false },
                  "transcription": {
                    "closed_caption_mode": "available",
                    "language": "auto",
                    "mode": "available"
                  },
                  "video": {
                    "access_request_enabled": false,
                    "camera_default_on": false,
                    "camera_facing": "front",
                    "enabled": true,
                    "target_resolution": { "height": 720, "width": 1280 }
                  }
                }
              },
              "credentials": {
                "server": { "edge_name": "", "url": "", "ws_endpoint": "" },
                "token": "",
                "ice_servers": []
              },
              "stats_options": { "enable_rtc_stats": false, "reporting_interval_ms": 0 },
              "playback": {
                "type": "hls",
                "playlist_url": "https://hls.example.com/playlist.m3u8",
                "reason": "percentage"
              }
            }
            """.trimIndent(),
        )!!

        assertThat(response.playback).isEqualTo(
            PlaybackResponse(
                playlistUrl = "https://hls.example.com/playlist.m3u8",
                reason = PlaybackResponse.Reason.Percentage,
                type = PlaybackResponse.Type.Hls,
            ),
        )
        assertThat(response.credentials.token).isEmpty()
        assertThat(response.credentials.server.url).isEmpty()
        assertThat(response.credentials.server.wsEndpoint).isEmpty()
        assertThat(response.credentials.server.edgeName).isEmpty()
        assertThat(response.credentials.iceServers).isEmpty()
        assertThat(response.call.cid).isEqualTo("livestream:test-call")
    }
}
