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
}
