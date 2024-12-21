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

import io.getstream.log.StreamLog
import io.getstream.log.taggedLogger
import org.webrtc.CameraEnumerationAndroid.CaptureFormat
import org.webrtc.RtpParameters
import stream.video.sfu.models.PublishOption
import stream.video.sfu.models.TrackType
import stream.video.sfu.models.VideoDimension
import stream.video.sfu.models.VideoLayer
import stream.video.sfu.models.VideoQuality
import kotlin.math.max

private val logger by StreamLog.taggedLogger("VideoLayers")

/**
 * This class represents the parameters for a single encoding (simulcast layer).
 * In JavaScript, `OptimalVideoLayer` extends `RTCRtpEncodingParameters`.
 * Here we just define the fields as needed.
 */
data class OptimalVideoLayer(
    var active: Boolean = true,
    var rid: String,
    var width: Int = 0,
    var height: Int = 0,
    var maxBitrate: Int = 0,
    var maxFramerate: Int? = null,
    var scalabilityMode: String? = null,
    var scaleResolutionDownBy: Double? = null,
    val svc: Boolean = false,
)

private val defaultBitratePerRid = mapOf(
    "q" to 300_000,
    "h" to 750_000,
    "f" to 1_250_000,
)

fun isSvcCodec(codecOrMimeType: String?): Boolean {
    if (codecOrMimeType == null) return false
    val lower = codecOrMimeType.lowercase()
    return lower == "vp9" ||
        lower == "av1" ||
        lower == "video/vp9" ||
        lower == "video/av1"
}

// Converts spatial and temporal layers to scalability mode string
fun toScalabilityMode(spatialLayers: Int, temporalLayers: Int): String {
    // Matches `LxTy_KEY` format
    val keySuffix = if (spatialLayers > 1) "_KEY" else ""
    return "L${spatialLayers}T${temporalLayers}$keySuffix"
}

fun toSvcEncodings(layers: List<OptimalVideoLayer>?): List<OptimalVideoLayer>? {
    // We take the `f` layer and rename its rid to `q`.
    return layers
        ?.filter { it.rid == "f" }
        ?.map { it.copy(rid = "q", svc = true) }
}

// Convert rid to VideoQuality
fun ridToVideoQuality(rid: String?): VideoQuality {
    return when (rid) {
        "q" -> VideoQuality.VIDEO_QUALITY_LOW_UNSPECIFIED
        "h" -> VideoQuality.VIDEO_QUALITY_MID
        else -> VideoQuality.VIDEO_QUALITY_HIGH
    }
}

// Convert OptimalVideoLayer list to VideoLayer list
fun toVideoLayers(layers: List<OptimalVideoLayer>): List<VideoLayer> {
    return layers.map {
        VideoLayer(
            rid = it.rid,
            bitrate = it.maxBitrate,
            fps = it.maxFramerate ?: 0,
            quality = if (it.svc) VideoQuality.VIDEO_QUALITY_HIGH else ridToVideoQuality(it.rid),
            video_dimension = VideoDimension(it.width, it.height),
        )
    }
}

fun isAudioTrackType(trackType: TrackType): Boolean =
    trackType == TrackType.TRACK_TYPE_AUDIO || trackType == TrackType.TRACK_TYPE_SCREEN_SHARE_AUDIO

/**
 * Computes the maximum bitrate for a given resolution.
 * If current resolution < target, scale bitrate proportionally down.
 * Otherwise use target bitrate.
 */
fun getComputedMaxBitrate(
    targetResolution: VideoDimension,
    currentWidth: Int,
    currentHeight: Int,
    bitrate: Int,
): Int {
    val (targetWidth, targetHeight) = Pair(targetResolution.height, targetResolution.width)
    return if (currentWidth < targetWidth || currentHeight < targetHeight) {
        val currentPixels = currentWidth * currentHeight
        val targetPixels = targetWidth * targetHeight
        val reductionFactor = currentPixels.toDouble() / targetPixels.toDouble()
        (bitrate * reductionFactor).toInt()
    } else {
        bitrate
    }
}

fun withSimulcastConstraints(
    settings: VideoDimension,
    optimalVideoLayers: List<OptimalVideoLayer>,
): List<OptimalVideoLayer> {
    val size = maxOf(settings.width, settings.height)
    val layers = when {
        size <= 320 -> {
            // only one layer 'f', the highest quality one
            optimalVideoLayers.filter { it.rid == "f" }
        }

        size <= 640 -> {
            // two layers, q and h (original had q,h,f -> remove 'f')
            optimalVideoLayers.filter { it.rid != "f" }
        }

        else -> {
            // three layers for sizes > 640x480
            optimalVideoLayers
        }
    }

    // Re-map rid according to index
    val ridMapping = listOf("q", "h", "f")
    return layers.mapIndexed { index, layer ->
        layer.copy(rid = ridMapping.getOrElse(index) { "q" })
    }
}

fun defaultVideoLayers(publishOption: PublishOption): List<RtpParameters.Encoding> {
    val defaultBitrate = 1_250_000
    val quarterQuality = RtpParameters.Encoding(
        "q",
        true,
        4.0,
    ).apply {
        maxBitrateBps = defaultBitratePerRid["q"] ?: (defaultBitrate / 4)
        maxFramerate = 30
    }

    val halfQuality = RtpParameters.Encoding(
        "h",
        true,
        2.0,
    ).apply {
        maxBitrateBps = defaultBitratePerRid["h"] ?: (defaultBitrate / 2)
        maxFramerate = 30
    }

    val fullQuality = RtpParameters.Encoding(
        "f",
        true,
        1.0,
    ).apply {
        maxBitrateBps = defaultBitratePerRid["f"] ?: defaultBitrate
        maxFramerate = 30
    }

    return if (isSvcCodec(publishOption.codec?.name)) {
        listOf(
            RtpParameters.Encoding(
                "q",
                true,
                1.0,
            ).apply {
                maxBitrateBps = defaultBitratePerRid["f"] ?: defaultBitrate
                maxFramerate = 30
            },
            halfQuality,
            fullQuality,
        )
    } else {
        listOf(quarterQuality, halfQuality, fullQuality)
    }
}

fun findOptimalVideoLayers(
    captureFormat: CaptureFormat,
    publishOption: PublishOption,
): List<OptimalVideoLayer> {
    val optimalVideoLayers = mutableListOf<OptimalVideoLayer>()
    val settings = VideoDimension(captureFormat.width, captureFormat.height)
    val width = settings.width
    val height = settings.height

    val bitrate = publishOption.bitrate
    val codec = publishOption.codec
    val fps = publishOption.fps
    val maxSpatialLayers = max(publishOption.max_spatial_layers, 1)
    val maxTemporalLayers = publishOption.max_temporal_layers
    val videoDimension = publishOption.video_dimension

    val maxBitrate = getComputedMaxBitrate(videoDimension ?: settings, height, width, bitrate)
    var downscaleFactor = 1.0
    var bitrateFactor = 1.0
    val svcCodec = isSvcCodec(codec?.name)

    val rids = listOf("f", "h", "q").take(maxSpatialLayers)

    for (rid in rids) {
        val layerWidth = (width / downscaleFactor).toInt()
        val layerHeight = (height / downscaleFactor).toInt()
        val calculatedBitrate = (maxBitrate / bitrateFactor).toInt()
        val layerBitrate =
            if (calculatedBitrate > 0) {
                calculatedBitrate
            } else {
                (
                    defaultBitratePerRid[rid]
                        ?: 1_250_000
                    )
            }

        val layer = OptimalVideoLayer(
            active = true,
            rid = rid,
            width = layerWidth,
            height = layerHeight,
            maxBitrate = layerBitrate,
            maxFramerate = fps,
        )

        if (svcCodec) {
            layer.scalabilityMode = toScalabilityMode(maxSpatialLayers, maxTemporalLayers)
        } else {
            layer.scaleResolutionDownBy = downscaleFactor
        }

        downscaleFactor *= 2
        bitrateFactor *= 2

        optimalVideoLayers.add(0, layer)
    }
    return withSimulcastConstraints(settings, optimalVideoLayers)
}
