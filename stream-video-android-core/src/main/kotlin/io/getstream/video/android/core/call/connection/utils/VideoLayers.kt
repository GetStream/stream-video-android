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

package io.getstream.video.android.core.call.connection.utils

import io.getstream.log.StreamLog
import io.getstream.log.taggedLogger
import io.getstream.webrtc.CameraEnumerationAndroid.CaptureFormat
import io.getstream.webrtc.RtpParameters
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
internal data class OptimalVideoLayer(
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

internal fun isSvcCodec(codecOrMimeType: String?): Boolean {
    if (codecOrMimeType == null) return false
    val lower = codecOrMimeType.lowercase()
    return lower == "vp9" ||
        lower == "av1" ||
        lower == "video/vp9" ||
        lower == "video/av1"
}

// Converts spatial and temporal layers to scalability mode string
internal fun toScalabilityMode(spatialLayers: Int, temporalLayers: Int): String {
    // Matches `LxTy_KEY` format
    val keySuffix = if (spatialLayers > 1) "_KEY" else ""
    return "L${spatialLayers}T${temporalLayers}$keySuffix"
}

internal fun toSvcEncodings(layers: List<OptimalVideoLayer>?): List<OptimalVideoLayer>? {
    // We take the `f` layer and rename its rid to `q`.
    val f = layers?.find { it.rid == "f" }
    val h = layers?.find { it.rid == "h" }
    val q = layers?.find { it.rid == "q" }
    return f?.let {
        listOf(it.copy(rid = "q", svc = true))
    } ?: h?.let {
        listOf(it.copy(rid = "q", svc = true))
    } ?: q?.let {
        listOf(it.copy(svc = true))
    }
}

internal fun RtpParameters.Encoding.stringify(): String {
    return "Encoding(rid=$rid, active=$active, scaleResolutionDownBy=$scaleResolutionDownBy, maxBitrateBps=$maxBitrateBps, maxFramerate=$maxFramerate, scalabilityMode=$scalabilityMode)"
}

// Convert rid to VideoQuality
internal fun ridToVideoQuality(rid: String?): VideoQuality {
    return when (rid) {
        "q" -> VideoQuality.VIDEO_QUALITY_LOW_UNSPECIFIED
        "h" -> VideoQuality.VIDEO_QUALITY_MID
        else -> VideoQuality.VIDEO_QUALITY_HIGH
    }
}

// Convert OptimalVideoLayer list to VideoLayer list
internal fun toVideoLayers(layers: List<OptimalVideoLayer>): List<VideoLayer> {
    return layers.map {
        VideoLayer(
            rid = it.rid,
            bitrate = it.maxBitrate,
            fps = it.maxFramerate ?: 0,
            quality = ridToVideoQuality(it.rid),
            video_dimension = VideoDimension(it.width, it.height),
        )
    }
}

internal fun isAudioTrackType(trackType: TrackType): Boolean =
    trackType == TrackType.TRACK_TYPE_AUDIO || trackType == TrackType.TRACK_TYPE_SCREEN_SHARE_AUDIO

/**
 * Computes the maximum bitrate for a given resolution.
 * If current resolution < target, scale bitrate proportionally down.
 * Otherwise use target bitrate.
 */
internal fun getComputedMaxBitrate(
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

internal fun withSimulcastConstraints(
    settings: VideoDimension,
    optimalVideoLayers: List<OptimalVideoLayer>,
): List<OptimalVideoLayer> {
    // Re-map rid according to index
    val size = maxOf(settings.width, settings.height)
    val layers = when {
        size <= 320 -> {
            // only one layer 'f', the highest quality one
            optimalVideoLayers.filter { it.rid == "f" }
        }

        size <= 640 -> {
            // two layers, q and h (original had q,h,f -> remove 'f')
            optimalVideoLayers.filter { it.rid != "q" }
        }

        else -> {
            // three layers for sizes > 640x480
            optimalVideoLayers
        }
    }
    // [q, h, f]
    return layers.mapIndexed { index, layer ->
        layer.copy(
            rid = when (index) {
                0 -> "q"
                1 -> "h"
                else -> "f"
            },
        )
    }
}

internal fun CaptureFormat.toVideoDimension(): VideoDimension {
    return VideoDimension(width, height)
}

internal fun computeTransceiverEncodings(
    captureFormat: CaptureFormat?,
    publishOption: PublishOption,
): List<RtpParameters.Encoding> {
    if (isAudioTrackType(publishOption.track_type)) {
        return emptyList()
    }
    val settings =
        captureFormat?.toVideoDimension() ?: publishOption.video_dimension ?: VideoDimension(
            1280,
            720,
        )
    val layers = findOptimalVideoLayers(
        settings,
        publishOption,
    )

    val codecLayers = if (isSvcCodec(publishOption.codec?.name)) {
        toSvcEncodings(layers) ?: emptyList()
    } else {
        layers
    }

    return codecLayers.map {
        RtpParameters.Encoding(
            it.rid,
            it.active,
            it.scaleResolutionDownBy,
        ).apply {
            scalabilityMode = it.scalabilityMode
            maxBitrateBps = it.maxBitrate
            maxFramerate = it.maxFramerate ?: 30
            numTemporalLayers = publishOption.max_temporal_layers
        }
    }
}

internal fun findOptimalVideoLayers(
    settings: VideoDimension,
    publishOption: PublishOption,
): List<OptimalVideoLayer> {
    val optimalVideoLayers = mutableListOf<OptimalVideoLayer>()
    val width = settings.width
    val height = settings.height
    val bitrate = publishOption.bitrate
    val codec = publishOption.codec
    val svcCodec = isSvcCodec(codec?.name)
    val fps = publishOption.fps
    val maxSpatialLayers = max(publishOption.max_spatial_layers, 1)
    val maxTemporalLayers = publishOption.max_temporal_layers
    val videoDimension = publishOption.video_dimension

    val maxBitrate = getComputedMaxBitrate(videoDimension ?: settings, height, width, bitrate)
    var downscaleFactor = 1.0
    var bitrateFactor = 1.0

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
