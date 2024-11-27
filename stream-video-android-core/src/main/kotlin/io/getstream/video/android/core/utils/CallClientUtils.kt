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

package io.getstream.video.android.core.utils

import io.getstream.video.android.core.model.IceServer
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

data class RtpMapAttribute(
    val index: Int,
    val sdpLine: String,
    val payloadType: String,
    val codecName: String,
    val codecClockRate: Int,
    var codecFmtp: String = "",
)

data class MediaStream(val index: Int, var codecs: List<String>, val line: String) {

    override fun toString(): String {
        val parts = line.split(" ")
        val first = parts.subList(0, 3).toMutableList()
        first.addAll(codecs)
        return first.joinToString(" ")
    }
}

internal class MinimalSdpParser(var sdp: String) {

    lateinit var lines: MutableList<String>
    var red: RtpMapAttribute? = null
    var opus: RtpMapAttribute? = null
    var h264: RtpMapAttribute? = null
    var vp8: RtpMapAttribute? = null
    var vp9: RtpMapAttribute? = null
    var av1: RtpMapAttribute? = null
    var audioM: MediaStream? = null
    var videoM: MediaStream? = null
    var useInBandFecLine: Int? = null

    init {
        parse()
    }

    private fun parse() {
        lines = sdp.split("\r\n", "\n").toMutableList()
        lines.let { lines ->
            lines.indices.forEach {
                val line = lines[it]
                if (line.contains("a=rtpmap")) {
                    // we want to detect vp8, h264, red and opus
                    if (line.contains("red/48000")) {
                        red = parseRtpMap(it, line)
                    } else if (line.contains("opus/48000")) {
                        opus = parseRtpMap(it, line)
                    } else if (line.contains("H264/90000")) {
                        h264 = parseRtpMap(it, line)
                    } else if (line.contains("VP8/90000")) {
                        vp8 = parseRtpMap(it, line)
                    } else if (line.contains("VP9/90000")) { // TODO-neg: always hardcode 90000?
                        vp9 = parseRtpMap(it, line)
                    } else if (line.contains("AV1/90000")) {
                        av1 = parseRtpMap(it, line)
                    }
                } else if (line.contains("m=audio")) {
                    audioM = parseMLine(it, line)
                } else if (line.contains("m=video")) {
                    videoM = parseMLine(it, line)
                } else if (line.contains("useinbandfec=1")) {
                    useInBandFecLine = it
                } else if (line.contains("a=fmtp")) {
                    parseFmtp(line)
                }
            }
        }
    }

    private fun parseRtpMap(index: Int, line: String): RtpMapAttribute {
        // Example: a=rtpmap:100 VP9/90000

        val parts = line.split(" ")
        val codec = parts[1]
        val codecName = codec.split("/")[0]
        val codecClockRate = codec.split("/")[1].toInt()
        val payloadType = parts[0].split(":")[1]

        return RtpMapAttribute(index, line, payloadType, codecName, codecClockRate)
    }

    private fun parseMLine(index: Int, line: String): MediaStream {
        val parts = line.split(" ")
        val codecs = parts.subList(3, parts.size)
        return MediaStream(index, codecs, line)
    }

    private fun parseFmtp(line: String) {
        // Example: a=fmtp:96 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=640c29

        // TODO-neg: take lines like a=fmtp:97 apt=96 into account and check for errors

        val parts = line.split(" ")
        val payloadType = parts[0].split(":")[1]
        val parameters = parts[1]

        // TODO-neg: now it assigns the last fmtp it finds, but it should keep all of them?
        when {
            h264?.payloadType == payloadType -> h264?.codecFmtp = parameters
            vp8?.payloadType == payloadType -> vp8?.codecFmtp = parameters
            vp9?.payloadType == payloadType -> vp9?.codecFmtp = parameters
            av1?.payloadType == payloadType -> av1?.codecFmtp = parameters
        }
    }

    fun mangle(enableDtx: Boolean = true, enableRed: Boolean = true, enableVp8: Boolean = true): String {
        if (enableDtx) {
            useInBandFecLine?.let {
                lines[it] = lines[it].replace("useinbandfec=1", "useinbandfec=1;usedtx=1")
            }
        }
        if (enableRed) {
            if (audioM != null && red != null && opus != null) {
                val codecs = audioM?.codecs
                val redPosition = codecs?.indices?.find { codecs[it] == red?.payloadType }
                val opusPosition = codecs?.indices?.find { codecs[it] == opus?.payloadType }

                // swap the position in the M line
                if (opusPosition != null && redPosition != null && opusPosition < redPosition) {
                    // remove red from the list
                    val newCodecs = codecs.filter { it != red!!.payloadType }.toMutableList()
                    newCodecs.add(0, red!!.payloadType)
                    audioM!!.codecs = newCodecs

                    audioM?.let {
                        lines[it.index] = it.toString()
                    }
                }
            }
        }
        if (enableVp8) {
            if (videoM != null && vp8 != null && h264 != null) {
                val codecs = videoM?.codecs
                val vp8Position = codecs?.indices?.find { codecs[it] == vp8?.payloadType }
                val h264Position = codecs?.indices?.find { codecs[it] == h264?.payloadType }

                // swap the position in the M line
                if (vp8Position != null && h264Position != null && h264Position < vp8Position) {
                    // remove red from the list
                    val newCodecs = codecs.filter { it != vp8!!.payloadType }.toMutableList()
                    newCodecs.add(0, vp8!!.payloadType)
                    videoM!!.codecs = newCodecs

                    videoM?.let {
                        lines[it.index] = it.toString()
                    }
                }
            }
        }
        val new = lines.joinToString("\r\n")
        return new
    }

    fun getVideoCodec(codec: String): RtpMapAttribute? {
        return when (codec.lowercase()) {
            "h264" -> h264
            "vp8" -> vp8
            "vp9" -> vp9
            "av1" -> av1
            else -> null
        }
    }
}

/**
 * Enabling DTX or RED requires mangling the SDP a bit
 */
fun mangleSdpUtil(
    sdp: SessionDescription,
    enableRed: Boolean = true,
    enableDtx: Boolean = true,
    enableVp8: Boolean = true,
): SessionDescription {
    // we don't touch the answer (for now)
    if (sdp.type == SessionDescription.Type.ANSWER) {
        return sdp
    }
    var description = sdp.description
    var parser = MinimalSdpParser(description)
    description = parser.mangle(enableDtx = enableDtx, enableRed = enableRed, enableVp8 = enableVp8)

    return SessionDescription(sdp.type, description)
}

@JvmSynthetic
internal fun buildRemoteIceServers(iceServers: List<IceServer>): List<PeerConnection.IceServer> {
    return iceServers.map {
        PeerConnection.IceServer.builder(it.urls)
            .setUsername(it.username)
            .setPassword(it.password)
            .createIceServer()
    }
}

@JvmSynthetic
internal fun buildConnectionConfiguration(
    iceServers: List<PeerConnection.IceServer>,
    sdpSemantics: PeerConnection.SdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN,
): PeerConnection.RTCConfiguration {
    return PeerConnection.RTCConfiguration(emptyList()).apply {
        this.sdpSemantics = sdpSemantics
        this.iceServers = iceServers
    }
}

@JvmSynthetic
internal fun buildMediaConstraints(): MediaConstraints {
    return MediaConstraints().apply {
        mandatory.addAll(
            listOf(
                MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"),
                MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"),
            ),
        )
    }
}

@JvmSynthetic
internal fun buildAudioConstraints(): MediaConstraints {
    val mediaConstraints = MediaConstraints()
    val items = listOf(
        MediaConstraints.KeyValuePair(
            "googEchoCancellation",
            true.toString(),
        ),
        MediaConstraints.KeyValuePair(
            "googAutoGainControl",
            true.toString(),
        ),
        MediaConstraints.KeyValuePair(
            "googHighpassFilter",
            true.toString(),
        ),
        MediaConstraints.KeyValuePair(
            "googNoiseSuppression",
            true.toString(),
        ),
        MediaConstraints.KeyValuePair(
            "googTypingNoiseDetection",
            true.toString(),
        ),
    )

    return mediaConstraints.apply {
        with(optional) {
            add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
            addAll(items)
        }
    }
}
