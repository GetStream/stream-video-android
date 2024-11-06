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

import io.getstream.log.taggedLogger
import io.getstream.video.android.core.model.VideoCodec
import org.webrtc.VideoCodecInfo
import org.webrtc.VideoEncoder
import org.webrtc.VideoEncoderFactory

internal class PreferredCodecVideoEncoderFactory(
    private val baseFactory: VideoEncoderFactory,
    internal var preferredCodec: VideoCodec? = null,
) : VideoEncoderFactory {

    private val logger by taggedLogger("Call:PreferredCodecVideoEncoderFactory")

    override fun createEncoder(info: VideoCodecInfo?): VideoEncoder? {
        logger.d { "[createEncoder] #updatePublishOptions; Creating encoder for codec: ${info?.name}" }
        return baseFactory.createEncoder(info)
    }

    override fun getSupportedCodecs(): Array<VideoCodecInfo> {
        // Search for preferredCodec and make it first in the list if found
        return preferredCodec?.let { preferredCodec ->
            val supportedCodecs = baseFactory.supportedCodecs.toMutableList()

            logger.d {
                "[getSupportedCodecs] #updatePublishOptions; Supported codecs: ${supportedCodecs.joinToString { it.name }}"
            }

            val preferredCodecIndex = supportedCodecs.indexOfFirst {
                it.name.lowercase() == preferredCodec.name.lowercase()
            }
            if (preferredCodecIndex == -1) {
                logger.w {
                    "[getSupportedCodecs] #updatePublishOptions; Preferred codec ${preferredCodec.name} not supported. Ignoring."
                }
            } else {
                val preferredCodecInfo = supportedCodecs.removeAt(preferredCodecIndex)
                supportedCodecs.add(0, preferredCodecInfo)
            }

            logger.d {
                "[getSupportedCodecs] #updatePublishOptions; Sorted codecs: ${supportedCodecs.joinToString { it.name }}"
            }

            supportedCodecs.toTypedArray()
        } ?: baseFactory.supportedCodecs
    }
}
