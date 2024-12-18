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
/**
 * A data class representing an available platform codec.
 */
internal data class AvailableCodec(
    val payload: Int,
    val name: String,
    val clockRate: Int,
    val channels: Int? = null,
    val fmtp: String? = null,
    val params: Map<String, String> = emptyMap(),
)

/**
 * Parse the fmtp line to extract the parameters.
 */
internal fun parseFmtpLine(fmtpLine: String): Map<String, String> {
    // fmtp lines have the form:
    // a=fmtp:<payload> <key>=<value>;<key>=<value>;...
    // Example:
    // a=fmtp:96 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f

    // First, remove the "a=fmtp:" prefix to isolate payload and parameters.
    val afterPrefix = fmtpLine.substringAfter("a=fmtp:")

    // afterPrefix now looks like "96 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f"
    // Split once to separate the payload (e.g., "96") from the parameters.
    val parts = afterPrefix.split(" ", limit = 2)

    // If we don't have two parts (payload and params), return empty.
    if (parts.size < 2) return emptyMap()

    val paramsPart = parts[1] // "level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f"
    val pairs = paramsPart.split(";")

    val parameters = mutableMapOf<String, String>()
    for (pair in pairs) {
        // Each pair is in the form <key>=<value>
        val keyValue = pair.split("=", limit = 2)
        if (keyValue.size == 2) {
            val key = keyValue[0].trim()
            val value = keyValue[1].trim()
            parameters[key] = value
        }
    }

    return parameters
}

/**
 * Compute the available codecs from the SDP.
 */
internal fun computePlatformCodecs(sdp: String): List<AvailableCodec> {
    val lines = sdp.lines()

    // Maps payload to a triple (codecName, clockRate, channels)
    val payloadToCodecInfo = mutableMapOf<String, Triple<String, String, String?>>()

    // Maps payload to fmtp line
    val payloadToFmtpLine = mutableMapOf<String, String>()

    // Maps payload to fmtp parameters
    val fmtpParams = mutableMapOf<String, Map<String, String>>()

    // Parse all a=rtpmap lines
    for (line in lines) {
        if (line.startsWith("a=rtpmap:")) {
            // format: a=rtpmap:<payload> <codec>/<clockrate>[/channels]
            val rest = line.substringAfter("a=rtpmap:")
            val parts = rest.split(" ")
            if (parts.size < 2) continue

            val payload = parts[0]
            val codecParts = parts[1].split("/")

            val codecName = codecParts.getOrNull(0) ?: continue
            val clockRate = codecParts.getOrNull(1) ?: ""
            val channels = codecParts.getOrNull(2)

            payloadToCodecInfo[payload] = Triple(codecName, clockRate, channels)
        }
    }

    // Parse all a=fmtp lines
    for (line in lines) {
        if (line.startsWith("a=fmtp:")) {
            // format: a=fmtp:<payload> <params>
            val rest = line.substringAfter("a=fmtp:")
            val parts = rest.split(" ", limit = 2)
            if (parts.size < 2) continue

            val payload = parts[0]
            payloadToFmtpLine[payload] = line
            val params = parseFmtpLine(line)
            fmtpParams[payload] = params
        }
    }

    // Collect all payloads from media sections (m= lines)
    val offeredPayloads = mutableSetOf<String>()
    for (line in lines) {
        if (line.startsWith("m=")) {
            // e.g. m=video 9 UDP/TLS/RTP/SAVPF 96 97 98
            val parts = line.split(" ")
            if (parts.size > 3) {
                val payloads = parts.subList(3, parts.size)
                offeredPayloads.addAll(payloads)
            }
        }
    }

    // Build the list of AvailableCodec
    val availableCodecs = mutableListOf<AvailableCodec>()
    for (payload in offeredPayloads) {
        val info = payloadToCodecInfo[payload] ?: continue
        val payloadInt = payload.toIntOrNull() ?: continue
        val clockRateInt = info.second.toIntOrNull() ?: 0
        val channelsInt = info.third?.toIntOrNull()

        val codec = AvailableCodec(
            payload = payloadInt,
            name = info.first,
            clockRate = clockRateInt,
            channels = channelsInt,
            fmtp = payloadToFmtpLine[payload],
            params = fmtpParams[payload] ?: emptyMap(),
        )
        availableCodecs.add(codec)
    }

    return availableCodecs
}

/**
 * A data class representing a codec bitrate table.
 */
internal data class CodecBitrateTable(
    val map: Map<Int, Int>,
    val default: Int,
)

private val bitrateLookupTable = mapOf(
    "h264" to CodecBitrateTable(
        mapOf(
            2160 to 5_000_000,
            1440 to 3_000_000,
            1080 to 2_000_000,
            720 to 1_250_000,
            540 to 750_000,
            360 to 400_000,
        ),
        default = 1_250_000,
    ),
    "vp8" to CodecBitrateTable(
        mapOf(
            2160 to 5_000_000,
            1440 to 2_750_000,
            1080 to 2_000_000,
            720 to 1_250_000,
            540 to 600_000,
            360 to 350_000,
        ),
        default = 1_250_000,
    ),
    "vp9" to CodecBitrateTable(
        mapOf(
            2160 to 3_000_000,
            1440 to 2_000_000,
            1080 to 1_500_000,
            720 to 1_250_000,
            540 to 500_000,
            360 to 275_000,
        ),
        default = 1_250_000,
    ),
    "av1" to CodecBitrateTable(
        mapOf(
            2160 to 2_000_000,
            1440 to 1_550_000,
            1080 to 1_000_000,
            720 to 600_000,
            540 to 350_000,
            360 to 200_000,
        ),
        default = 600_000,
    ),
)

/**
 * Get the optimal bitrate for a given codec and frame height.
 */
internal fun getOptimalBitrate(codec: String, frameHeight: Int): Int {
    val lowerCaseCodec = codec.lowercase()
    val codecLookup = bitrateLookupTable[lowerCaseCodec]
        ?: throw IllegalArgumentException("Unknown codec: $codec")

    // Check for an exact match
    codecLookup.map[frameHeight]?.let { return it }

    // Find nearest resolution
    val nearest = codecLookup.map.keys.minByOrNull { kotlin.math.abs(it - frameHeight) }
    return nearest?.let { codecLookup.map[it] } ?: codecLookup.default
}
