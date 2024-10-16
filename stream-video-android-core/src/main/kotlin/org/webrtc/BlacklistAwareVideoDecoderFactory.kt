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

package org.webrtc

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import io.getstream.log.taggedLogger

internal class BlacklistAwareVideoDecoderFactory(
    eglContext: EglBase.Context?,
) : VideoDecoderFactory {

    private val logger by taggedLogger("BlacklistAwareVideoDecoderFactory")

    private val mediaCodecList by lazy { MediaCodecList(MediaCodecList.ALL_CODECS) }

    /**
     * Blacklist of codecs that are known to be buggy; we want to force software decoding for them.
     */
    private val isHardwareDecoderBlacklisted: (MediaCodecInfo?) -> Boolean = {
        it?.isExynosVP9() ?: false
    }

    private val allowedCodecPredicate: Predicate<MediaCodecInfo> = Predicate {
        MediaCodecUtils.isHardwareAccelerated(it) || MediaCodecUtils.isSoftwareOnly(it)
    }

    private val hardwareVideoDecoderFactory: VideoDecoderFactory =
        HardwareVideoDecoderFactory(eglContext)
    private val softwareVideoDecoderFactory: VideoDecoderFactory = SoftwareVideoDecoderFactory()
    private val platformSoftwareVideoDecoderFactory: VideoDecoderFactory =
        PlatformSoftwareVideoDecoderFactory(eglContext)

    override fun createDecoder(codecType: VideoCodecInfo): VideoDecoder? {
        val type = VideoCodecMimeType.valueOf(codecType.getName())
        val codec = findCodecForType(type)
        logger.d { "[createDecoder] codecType: $codecType, codec: ${codec?.stringify()}" }

        var softwareDecoder = softwareVideoDecoderFactory.createDecoder(codecType)
        val hardwareDecoder = hardwareVideoDecoderFactory.createDecoder(codecType)
        if (softwareDecoder == null) {
            softwareDecoder = platformSoftwareVideoDecoderFactory.createDecoder(codecType)
        }

        if (isHardwareDecoderBlacklisted(codec)) {
            logger.i { "[createDecoder] hardware decoder is blacklisted: ${codec?.stringify()}" }
            return softwareDecoder
        }

        return if (hardwareDecoder != null && softwareDecoder != null) {
            VideoDecoderFallback(softwareDecoder, hardwareDecoder)
        } else {
            hardwareDecoder ?: softwareDecoder
        }
    }

    override fun getSupportedCodecs(): Array<VideoCodecInfo> {
        val supportedCodecInfos = mutableSetOf<VideoCodecInfo>().apply {
            addAll(softwareVideoDecoderFactory.supportedCodecs)
            addAll(hardwareVideoDecoderFactory.supportedCodecs)
            addAll(platformSoftwareVideoDecoderFactory.supportedCodecs)
        }
        logger.v { "[getSupportedCodecs] supportedCodecInfos: $supportedCodecInfos" }
        return supportedCodecInfos.toTypedArray<VideoCodecInfo>()
    }

    private fun findCodecForType(type: VideoCodecMimeType): MediaCodecInfo? {
        val codecInfos: List<MediaCodecInfo> = try {
            mediaCodecList.codecInfos.filterNotNull().toList()
        } catch (e: Throwable) {
            logger.e(e) { "[findCodecForType] failed: $e" }
            emptyList()
        }
        return codecInfos.firstOrNull {
            !it.isEncoder && isSupportedCodec(it, type)
        }
    }

    private fun isSupportedCodec(codec: MediaCodecInfo, type: VideoCodecMimeType): Boolean {
        if (!MediaCodecUtils.codecSupportsType(codec, type)) {
            return false
        }
        val colorFormat = MediaCodecUtils.selectColorFormat(
            MediaCodecUtils.DECODER_COLOR_FORMATS,
            codec.getCapabilitiesForType(type.mimeType()),
        )
        if (colorFormat == null) {
            return false
        }
        return isCodecAllowed(codec)
    }

    private fun isCodecAllowed(info: MediaCodecInfo): Boolean {
        return allowedCodecPredicate.test(info)
    }
}

private fun MediaCodecInfo.isExynosVP9(): Boolean {
    return !isEncoder && name.contains("exynos", ignoreCase = true) &&
        name.contains("vp9", ignoreCase = true)
}

private fun MediaCodecInfo.stringify(): String {
    return "MediaCodecInfo(" +
        "name=$name, " +
        "canonicalName=$canonicalName, " +
        "isAlias=$isAlias, " +
        "isVendor=$isVendor, " +
        "isEncoder=$isEncoder, " +
        "isHardwareAccelerated=$isHardwareAccelerated, " +
        "isSoftwareOnly=$isSoftwareOnly, " +
        "supportedTypes=${supportedTypes.joinToString()}" +
        ")"
}
