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

package io.getstream.video.android.core.socket.coordinator.v2

import io.getstream.android.core.api.serialization.StreamEventSerialization
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.video.android.core.socket.common.parser2.MoshiVideoParser

/**
 * Adapts the existing Moshi-based [MoshiVideoParser] to core's [StreamEventSerialization]
 * interface so that `stream-android-core`'s composite deserializer can route product events
 * to the video SDK without re-implementing Moshi adapter registration.
 *
 * Wraps parser calls in [runCatching] because [MoshiVideoParser.fromJson] throws on null while
 * the contract here is a [Result].
 */
internal class VideoEventSerialization(
    private val parser: MoshiVideoParser = MoshiVideoParser(),
) : StreamEventSerialization<VideoEvent> {

    override fun serialize(data: VideoEvent): Result<String> = runCatching {
        parser.toJson(data)
    }

    override fun deserialize(raw: String): Result<VideoEvent> = runCatching {
        parser.fromJson(raw, VideoEvent::class.java)
    }
}
