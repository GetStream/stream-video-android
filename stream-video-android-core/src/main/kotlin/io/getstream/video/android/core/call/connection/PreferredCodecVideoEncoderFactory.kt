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

package io.getstream.video.android.core.call.connection

import android.util.Log
import org.webrtc.VideoCodecInfo
import org.webrtc.VideoEncoder
import org.webrtc.VideoEncoderFactory

internal class PreferredCodecVideoEncoderFactory(
    private val baseFactory: VideoEncoderFactory,
    internal var preferredCodec: VideoCodec? = null,
) : VideoEncoderFactory {

    override fun createEncoder(p0: VideoCodecInfo?): VideoEncoder? {
        Log.d("CodecDebug", "[createEncoder] Creating encoder for codec: ${p0?.name}")
        return baseFactory.createEncoder(p0)
    }

    override fun getSupportedCodecs(): Array<VideoCodecInfo> {
        // Search for preferredCodec and make it first in the list if found
        val codecs = baseFactory.supportedCodecs.toMutableList()
        Log.d(
            "CodecDebug",
            "[getSupportedCodecs] Supported codecs: ${codecs.joinToString { it.name }}",
        )
        val preferredCodecIndex = codecs.indexOfFirst {
            it.name.lowercase() == preferredCodec?.name?.lowercase()
        }
        if (preferredCodecIndex != -1) {
            val preferredCodecInfo = codecs.removeAt(preferredCodecIndex)
            codecs.add(0, preferredCodecInfo)
        }
        Log.d(
            "CodecDebug",
            "[getSupportedCodecs] Preferred codecs: ${codecs.joinToString { it.name }}",
        )
        return codecs.toTypedArray()
    }
}

enum class VideoCodec {
    H264,
    VP8,
    VP9,
    AV1,
}
