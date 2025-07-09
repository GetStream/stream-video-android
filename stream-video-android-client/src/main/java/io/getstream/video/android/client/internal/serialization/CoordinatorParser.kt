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

package io.getstream.video.android.client.internal.serialization

import com.squareup.moshi.Moshi
import io.getstream.android.video.generated.models.VideoEvent
import io.getstream.android.video.generated.models.WSCallEvent
import okio.ByteString
import okio.ByteString.Companion.toByteString
import stream.video.sfu.event.SfuEvent
import stream.video.sfu.event.SfuRequest

internal interface GenericParser<E, E2> {
    /** Encodes the given object into a ByteString. */
    fun encode(event: E): Result<ByteString>

    /** Decodes the given [raw] [ByteArray] into an object of type [E2]. */
    fun decode(raw: ByteArray): Result<E2>
}

/**
 * Parser for the SFU socket.
 */
internal class SfuParser : GenericParser<SfuRequest, SfuEvent> {

    override fun encode(event: SfuRequest): Result<ByteString> = runCatching {
        event.encodeByteString()
    }

    override fun decode(raw: ByteArray): Result<SfuEvent> = runCatching {
        SfuEvent.ADAPTER.decode(raw)
    }
}

/**
 * Parser for the Video socket.
 */
internal class CoordinatorParser(private val moshi: Moshi) : GenericParser<VideoEvent, VideoEvent> {

    override fun encode(event: VideoEvent): Result<ByteString> = runCatching {
        val json = moshi.adapter(event.javaClass).toJson(event)
        json.encodeToByteArray().toByteString()
    }

    override fun decode(raw: ByteArray): Result<VideoEvent> = runCatching {
        val text = String(raw)
        val event = moshi.adapter(VideoEvent::class.java).fromJson(text)
            ?: throw IllegalArgumentException("Failed to parse event: $text")
        event
    }
}
